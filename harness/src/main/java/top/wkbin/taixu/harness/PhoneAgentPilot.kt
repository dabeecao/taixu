package top.wkbin.taixu.harness

import java.util.Base64
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import top.wkbin.taixu.core.datastore.AgentPreferences
import top.wkbin.taixu.core.model.workflow.PhoneWorkflowOperation
import top.wkbin.taixu.runtime.gui.GuiKey
import top.wkbin.taixu.runtime.gui.GuiPrimitive
import top.wkbin.taixu.runtime.virtualdisplay.VirtualDisplayCoordinator
import top.wkbin.taixu.runtime.virtualdisplay.VirtualScreenToolkit
import top.wkbin.taixu.runtime.virtualdisplay.PhoneTaskControl
import top.wkbin.taixu.runtime.virtualdisplay.PhoneTaskState
import top.wkbin.taixu.runtime.virtualdisplay.VirtualDisplayHud

/**
 * 用设置里选定的手机操作模型看虚拟屏截图，并按它输出的动作去点、滑、输入。
 * 主模型只负责下达任务，不再自己一轮轮截图猜坐标。
 */
internal class PhoneAgentPilot(
    private val providerClient: ProviderClient,
    private val preferences: AgentPreferences,
    private val coordinator: VirtualDisplayCoordinator,
    private val toolkit: VirtualScreenToolkit,
    private val services: PhoneAgentServices? = null,
) {
    suspend fun run(session: String, goal: String, packageName: String, maxSteps: Int, workflowName: String = ""): Pair<Boolean, String> = coroutineScope {
        val task = coordinator.phoneTasks.start(session, currentCoroutineContext()[Job]!!)
            ?: return@coroutineScope false to "Phiên màn hình ảo này đã có tác vụ điện thoại, hãy dừng nó trước hoặc dùng session khác."
        try {
            services?.log(session, "RunStart executor=phone-agent maxSteps=$maxSteps record=${workflowName.isNotBlank()}")
            val result = runTask(session, goal, packageName, maxSteps.coerceIn(1, 20), task, workflowName)
            if (task.state.value != PhoneTaskState.NEEDS_USER && task.state.value != PhoneTaskState.CANCELLED) {
                task.finish(if (result.first) PhoneTaskState.COMPLETED else PhoneTaskState.FAILED)
                if (!result.first) VirtualDisplayHud.setStep("Tác vụ chưa xong · Xem nguyên nhân trong chat")
            }
            services?.log(session, "RunEnd success=${result.first} state=${task.state.value}")
            result.first to "Người thực hiện: mô hình thao tác điện thoại.\n${result.second}"
        } catch (e: CancellationException) {
            task.finish(PhoneTaskState.CANCELLED)
            VirtualDisplayHud.setStep("Tác vụ đã dừng")
            throw e
        } catch (e: Exception) {
            task.finish(PhoneTaskState.FAILED)
            services?.log(session, "RunError kind=${e::class.java.simpleName}")
            VirtualDisplayHud.setStep("Tác vụ thực hiện thất bại")
            throw e
        } finally {
            coordinator.phoneTasks.release(session, task)
        }
    }

    private suspend fun runTask(
        session: String, goal: String, packageName: String, maxSteps: Int, task: PhoneTaskControl, workflowName: String,
    ): Pair<Boolean, String> {
        val endpoint = preferences.phoneAgentConfig.first()
        if (!endpoint.ready) {
            return false to "Chưa cấu hình mô hình thao tác điện thoại. Hãy tới Cài đặt → Agent và mô hình → Màn hình ảo → Mô hình thao tác điện thoại để điền riêng interface. Chỗ này khác với mô hình chính của chat. Zhipu Base URL là https://open.bigmodel.cn/api/paas/v4 , tên mô hình là autoglm-phone."
        }
        val model = phoneAgentModel(endpoint)
        services?.log(session, "ModelConfig model=${model.model} max_tokens=${model.maxTokens}")
        val recording = mutableListOf<PhoneWorkflowOperation>()
        var recordingClean = true
        var lastVersion = task.awaitReady()
        if (coordinator.getDisplayId(session) == null && coordinator.ensureVirtualDisplay(session) == null) {
            return false to "Tạo màn hình ảo thất bại, mô hình thao tác điện thoại không thể bắt đầu."
        }
        if (packageName.isNotBlank()) {
            val ready = task.awaitReady()
            if (ready != lastVersion) recordingClean = false
            lastVersion = ready
            val pkg = resolveLaunchablePackage(packageName, coordinator.installedApps())
                ?: return false to "Không tìm thấy ứng dụng「$packageName」. Hãy dùng tên gói, hoặc đừng truyền package, để mô hình thao tác điện thoại tự mở."
            if (!task.isCurrent(lastVersion)) return false to "Có can thiệp thủ công trước khi khởi động ứng dụng, tác vụ đã dừng; hãy đối chiếu trang rồi chạy lại."
            if (!coordinator.launchApp(session, pkg)) return false to "Màn hình ảo khởi động ứng dụng thất bại: $pkg"
            recording += PhoneWorkflowOperation("virtual_screen_launch", mapOf("package" to pkg), 900)
            if (!task.isCurrent(lastVersion)) recordingClean = false
            delay(900)
        }
        val notes = ArrayList<String>(maxSteps)
        coordinator.reveal(session, "Mô hình điện thoại tiếp quản")?.let { notes += it }
        val progress = PhoneAgentProgress()
        var invalidReplies = 0
        var step = 0
        var request = 0
        while (step < maxSteps) {
            val version = task.awaitReady()
            if (version != lastVersion) recordingClean = false
            lastVersion = version
            val png = coordinator.requestScreenshot(session)
                ?: return false to finish(notes, "Bước ${step + 1} chụp màn hình thất bại, màn hình ảo không có hình ảnh.")
            val size = screenSize(session, png)
                ?: return false to finish(notes, "Bước ${step + 1} không biết kích thước màn hình ảo, không đổi được tọa độ tương đối sang pixel.")
            val history = if (notes.isEmpty()) "Đây là bước đầu tiên." else "Đã làm:\n" + notes.joinToString("\n")
            val recordingHint = if (workflowName.isNotBlank() && recording.isEmpty() && packageName.isBlank())
                "\nCần ghi workflow tái sử dụng, hãy dùng Launch mở ứng dụng đích trước, rồi thao tác." else ""
            if (!task.isCurrent(version)) { recordingClean = false; continue }
            val image = ImagePayloadCompressor.downscaleDataUrl("data:image/png;base64," + Base64.getEncoder().encodeToString(png))
            val started = System.nanoTime()
            services?.log(session, "ModelRequest request=${++request} step=${step + 1} display=${coordinator.getDisplayId(session)} frame=${size.first}x${size.second} pngBytes=${png.size} imageChars=${image.length}")
            val reply = try {
                providerClient.chat(
                    model,
                    listOf(
                        ApiMessage(role = "system", content = phoneAgentSystemPrompt()),
                        ApiMessage(
                            role = "user",
                            content = "任务：$goal\n$history$recordingHint\n请根据截图输出下一个动作。",
                            imageUrls = listOf(image),
                        ),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                services?.log(session, "ModelError request=$request elapsedMs=${(System.nanoTime() - started) / 1_000_000} kind=${e::class.java.simpleName} outputBudgetRejected=${e.message?.contains("max_tokens", true) == true}")
                return false to finish(notes, "Yêu cầu model điều khiển điện thoại thất bại: ${e.message}")
            }
            services?.log(session, "ModelResponse request=$request elapsedMs=${(System.nanoTime() - started) / 1_000_000} contentChars=${reply.content?.length ?: 0} reasoningChars=${reply.reasoningContent?.length ?: 0} inputTokens=${reply.usage.inputTokens} outputTokens=${reply.usage.outputTokens}")
            currentCoroutineContext().ensureActive()
            // 人工触摸或暂停/继续后，这次推理基于旧截图，必须重新观察。
            if (!task.isCurrent(version)) { recordingClean = false; continue }
            val text = reply.content.orEmpty()
            val action = parsePhoneAgentAction(text)
            if (action == null) {
                services?.log(session, "ActionParseFailed request=$request")
                invalidReplies++
                if (invalidReplies >= 2) return false to finish(notes, "Hai lần liên tiếp không parse được hành động chính thức, hãy kiểm tra giao thức model điện thoại.")
                notes += "Lỗi định dạng: chỉ xuất một lệnh do(...) hoặc finish(message=...) hoàn chỉnh trong <answer>, dùng đúng action và tham số đã định nghĩa."
                continue
            }
            invalidReplies = 0
            val frame = MessageDigest.getInstance("SHA-256").digest(png).joinToString("") { "%02x".format(it) }
            progress.beforeAction(action, frame)?.let { return false to finish(notes, it) }
            step++
            services?.log(session, "Action step=$step ${phoneActionLog(action)}")
            if (!task.isCurrent(version)) { recordingClean = false; continue }
            when (action) {
                is PhoneAgentAction.Finish -> {
                    task.finish(PhoneTaskState.COMPLETED)
                    coordinator.reveal(session, "Tác vụ hoàn thành")
                    val saved = when {
                        workflowName.isBlank() -> ""
                        !recordingClean -> "\nCó bước can thiệp thủ công hoặc thất bại, chưa tự lưu workflow; hãy sắp xếp thủ công trong trình chỉnh sửa."
                        phoneRecordingIssue(recording) != null -> "\n${phoneRecordingIssue(recording)}"
                        services == null -> "\nDịch vụ lưu workflow chưa kết nối, chưa lưu."
                        else -> try { "\n" + services.save(workflowName, recording) }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { "\nThao tác kết thúc, nhưng lưu workflow thất bại: ${e.message}" }
                    }
                    return true to finish(notes, action.message + saved)
                }
                is PhoneAgentAction.TakeOver -> {
                    task.finish(PhoneTaskState.NEEDS_USER)
                    coordinator.reveal(session, "Cần bạn tiếp quản")
                    return false to finish(notes, "Cần can thiệp thủ công: ${action.message.ifBlank { "登录、验证码或支付" }}. Hãy tiếp tục thao tác trên cửa sổ nổi.")
                }
                is PhoneAgentAction.Note -> {
                    notes += "$step. Ghi chép: ${action.message.ifBlank { "当前页面" }}"
                    coordinator.reveal(session, "Ghi lại trang")
                }
                else -> {
                    val actionStarted = System.nanoTime()
                    val performed = perform(session, action, size, task, version)
                    services?.log(session, "Injection step=$step success=${performed.first} elapsedMs=${(System.nanoTime() - actionStarted) / 1_000_000}")
                    if (!task.isCurrent(version)) {
                        recordingClean = false
                        notes += "$step. ${actionLabel(action)} có can thiệp thủ công trong lúc thực hiện, một phần input có thể đã gửi; chụp lại để đối chiếu."
                        continue
                    }
                    notes += "$step. ${actionLabel(action)}：${if (performed.first) "输入已发出/等待截图核验" else "执行失败"}；${performed.second}"
                    coordinator.reveal(session, performed.second.take(24))
                    progress.afterAction(performed.first)?.let { return false to finish(notes, it) }
                    val waitMs = when (action) {
                        is PhoneAgentAction.Wait -> action.durationMs
                        is PhoneAgentAction.Launch -> 900
                        else -> 700
                    }
                    if (performed.first) action.workflowOperation(waitMs, performed.second.removePrefix("Khởi động "))?.let { recording += it }
                    else recordingClean = false
                    delay(waitMs)
                }
            }
        }
        return false to finish(notes, "Đã đạt $maxSteps bước, tác vụ vẫn chưa xong. Có thể giao bước tiếp cho mô hình thao tác điện thoại.")
    }

    private fun screenSize(session: String, png: ByteArray): Pair<Int, Int>? {
        val (width, height) = VirtualScreenHostActions.pngDimensions(png)
        if (width > 1 && height > 1) return width to height
        return coordinator.getVideoSize(session)?.takeIf { it.first > 1 && it.second > 1 }
    }

    private suspend fun perform(
        session: String, action: PhoneAgentAction, size: Pair<Int, Int>, task: PhoneTaskControl, version: Long,
    ): Pair<Boolean, String> {
        val canInject = { task.isCurrent(version) }
        suspend fun execute(primitive: GuiPrimitive) = toolkit.execute(session, primitive, canInject)
        val result = when (action) {
            is PhoneAgentAction.Tap -> execute(GuiPrimitive.Tap(phoneAgentPoint(action.x, size.first), phoneAgentPoint(action.y, size.second)))
            is PhoneAgentAction.DoubleTap -> execute(
                GuiPrimitive.DoubleTap(phoneAgentPoint(action.x, size.first), phoneAgentPoint(action.y, size.second)),
            )
            is PhoneAgentAction.LongPress -> execute(
                GuiPrimitive.LongPress(phoneAgentPoint(action.x, size.first), phoneAgentPoint(action.y, size.second)),
            )
            is PhoneAgentAction.Swipe -> execute(
                GuiPrimitive.Swipe(
                    phoneAgentPoint(action.x1, size.first),
                    phoneAgentPoint(action.y1, size.second),
                    phoneAgentPoint(action.x2, size.first),
                    phoneAgentPoint(action.y2, size.second),
                ),
            )
            is PhoneAgentAction.Type -> toolkit.setText(session, action.text, canInject)
            is PhoneAgentAction.Launch -> {
                val requested = action.app.trim()
                val pkg = resolveLaunchablePackage(requested, coordinator.installedApps())
                    ?: return false to "Không tìm thấy ứng dụng「$requested」. Hãy bấm icon trên desktop, hoặc dùng tên gói."
                if (!canInject()) return false to "Khởi động ứng dụng đã hủy: tác vụ tạm dừng hoặc hình ảnh mất hiệu lực"
                return if (coordinator.launchApp(session, pkg)) true to "Khởi động $pkg" else false to "Khởi động thất bại $pkg"
            }
            PhoneAgentAction.Back -> execute(GuiPrimitive.Key(GuiKey.BACK))
            PhoneAgentAction.Home -> execute(GuiPrimitive.Key(GuiKey.HOME))
            is PhoneAgentAction.Wait -> return true to "Chờ trang ${action.durationMs}ms"
            is PhoneAgentAction.Note -> return true to "Ghi chép: ${action.message}"
            is PhoneAgentAction.Finish, is PhoneAgentAction.TakeOver -> return false to "Hành động này không thể inject"
        }
        return result.success to result.message
    }

    private fun finish(notes: List<String>, outcome: String): String = buildString {
        append(outcome)
        if (notes.isNotEmpty()) {
            append("\nNhật ký thao tác:\n")
            append(notes.joinToString("\n"))
        }
    }

}

private fun actionLabel(action: PhoneAgentAction): String = when (action) {
    is PhoneAgentAction.Type -> "Type(${action.text.length} ký tự)"
    else -> action.toString()
}
