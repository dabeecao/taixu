package top.wkbin.taixu.runtime.doctor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import top.wkbin.taixu.core.model.BuiltinPluginBundles
import top.wkbin.taixu.core.model.DoctorCategory
import top.wkbin.taixu.core.model.DoctorItem
import top.wkbin.taixu.core.model.DoctorReport
import top.wkbin.taixu.core.model.DoctorStatus
import top.wkbin.taixu.core.model.RuntimeState
import top.wkbin.taixu.runtime.LinuxRuntime
import top.wkbin.taixu.runtime.shell.CommandResult
import top.wkbin.taixu.runtime.shell.ShellCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EnvironmentDoctor(
    private val context: Context? = null,
    private val linuxRuntime: LinuxRuntime,
) {
    suspend fun check(): DoctorReport = withContext(Dispatchers.IO) {
        // 宿主侧权限检查：不依赖沙箱状态，两种路径都要展示
        val allFilesAccessItem = checkAllFilesAccess()

        val state = linuxRuntime.state.value
        if (state !is RuntimeState.Ready) {
            val unreadyItem = DoctorItem(
                id = "sandbox_unready",
                category = DoctorCategory.SANDBOX,
                title = "Trạng thái sandbox PRoot",
                status = DoctorStatus.ERROR,
                summary = if (state is RuntimeState.Error) "Sandbox bất thường: ${state.throwable.message}" else "Sandbox chưa khởi tạo",
                detail = "Hãy khởi tạo và khởi động môi trường Linux sandbox trong dashboard trước",
                fixable = false,
            )
            return@withContext DoctorReport(
                items = listOf(unreadyItem, allFilesAccessItem),
                timestamp = System.currentTimeMillis(),
                overallStatus = DoctorStatus.ERROR,
                healthyCount = itemsHealthy(listOf(unreadyItem, allFilesAccessItem)),
                warningCount = itemsWarning(listOf(unreadyItem, allFilesAccessItem)),
                errorCount = 1,
            )
        }

        val items = mutableListOf<DoctorItem>()

        // 1. 沙箱与存储检查
        items.add(checkSandboxStorage())
        items.add(allFilesAccessItem)

        // 2. DNS 与网络连通性检查
        items.add(checkDnsAndNetwork())

        // 3. CA 根证书检查
        items.add(checkCaCertificates())

        // 4. APT 软件源与镜像加速检查
        items.add(checkAptMirrors())

        // 5. 基础开发工具链检查 (git/curl/tar/xz)
        items.add(checkBaseDevTools())

        // 6. Node.js 运行时检查
        items.add(checkNodeRuntime())

        // 7. Android 开发环境检查（可选插件，未安装时引导获取离线/在线插件包）
        items.add(checkAndroidEnvironment())

        val healthyCount = items.count { it.status == DoctorStatus.HEALTHY }
        val warningCount = items.count { it.status == DoctorStatus.WARNING }
        val errorCount = items.count { it.status == DoctorStatus.ERROR }
        val unknownCount = items.count { it.status == DoctorStatus.UNKNOWN }

        val overallStatus = when {
            errorCount > 0 -> DoctorStatus.ERROR
            warningCount > 0 -> DoctorStatus.WARNING
            unknownCount > 0 -> DoctorStatus.UNKNOWN
            else -> DoctorStatus.HEALTHY
        }

        DoctorReport(
            items = items,
            timestamp = System.currentTimeMillis(),
            overallStatus = overallStatus,
            healthyCount = healthyCount,
            warningCount = warningCount,
            errorCount = errorCount,
            unknownCount = unknownCount,
        )
    }

    /**
     * 沙箱探测统一入口：失败后以双倍超时重试一次。
     * 两次都拿不到结果（异常/超时）返回 null —— 探测不可达 ≠ 配置异常，
     * 调用方应如实给出 UNKNOWN（灰牌「沙箱正忙，暂时无法确认」），
     * 绝不把超时误报成 WARNING/ERROR 配置异常。
     */
    private suspend fun probe(commandLine: String, timeoutMs: Long): CommandResult? {
        val first = runCatching {
            linuxRuntime.execute(ShellCommand(commandLine, timeoutMs = timeoutMs))
        }.getOrNull()
        if (first != null) return first
        return runCatching {
            linuxRuntime.execute(ShellCommand(commandLine, timeoutMs = timeoutMs * 2))
        }.getOrNull()
    }

    /** 「沙箱正忙，暂时无法确认」灰牌条目：不计入待修复项。 */
    private fun unknownItem(
        id: String,
        category: DoctorCategory,
        title: String,
        detail: String? = null,
    ) = DoctorItem(
        id = id,
        category = category,
        title = title,
        status = DoctorStatus.UNKNOWN,
        summary = "Sandbox đang bận, tạm thời chưa xác nhận được",
        detail = detail ?: "Sandbox đang xử lý tác vụ khác nên dò timeout, khám lại sau sẽ phục hồi",
        fixable = false,
    )

    private fun checkAllFilesAccess(): DoctorItem {
        val granted = if (context == null) {
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
        return if (granted) {
            DoctorItem(
                id = "host_all_files_access",
                category = DoctorCategory.SANDBOX,
                title = "Mọi quyền truy cập file",
                status = DoctorStatus.HEALTHY,
                summary = "Đã cấp「Truy cập mọi file」, trình duyệt file có thể truy cập đầy đủ bộ nhớ dùng chung",
            )
        } else {
            DoctorItem(
                id = "host_all_files_access",
                category = DoctorCategory.SANDBOX,
                title = "Mọi quyền truy cập file",
                status = DoctorStatus.WARNING,
                summary = "Chưa cấp quyền「Truy cập mọi file」",
                detail = "Android sẽ lọc file của ứng dụng khác trong bộ nhớ dùng chung, mục /sdcard của trình duyệt file có thể chỉ hiện thư mục mà không thấy file. Bấm「đến ủy quyền」bên phải để đến ngay cài đặt hệ thống bật lên.",
                fixable = true,
            )
        }
    }

    private fun itemsHealthy(items: List<DoctorItem>): Int = items.count { it.status == DoctorStatus.HEALTHY }

    private fun itemsWarning(items: List<DoctorItem>): Int = items.count { it.status == DoctorStatus.WARNING }

    private suspend fun checkSandboxStorage(): DoctorItem {
        val res = probe(
            commandLine = "mkdir -p /workspace /tmp && touch /workspace/.doctor_probe && rm -f /workspace/.doctor_probe",
            timeoutMs = 5000L,
        )

        return when {
            res == null -> unknownItem(
                id = "sandbox_storage",
                category = DoctorCategory.SANDBOX,
                title = "PRoot sandbox & không gian làm việc",
                detail = "Probe đọc ghi timeout, sandbox có thể đang bận (như đang chạy tác vụ build/cài đặt)",
            )
            res.isSuccess -> DoctorItem(
                id = "sandbox_storage",
                category = DoctorCategory.SANDBOX,
                title = "PRoot sandbox & không gian làm việc",
                status = DoctorStatus.HEALTHY,
                summary = "Môi trường ảo sandbox bình thường, /workspace và /tmp đã sẵn sàng đọc ghi",
            )
            else -> DoctorItem(
                id = "sandbox_storage",
                category = DoctorCategory.SANDBOX,
                title = "PRoot sandbox & không gian làm việc",
                status = DoctorStatus.ERROR,
                summary = "Kiểm tra đọc ghi hoặc quyền của không gian làm việc thất bại",
                detail = res.stderr.ifBlank { res.stdout }.ifBlank { "Lệnh probe thực thi thất bại" },
            )
        }
    }

    private suspend fun checkDnsAndNetwork(): DoctorItem {
        val resolvCheck = probe("cat /etc/resolv.conf 2>/dev/null", timeoutMs = 3000L)

        // 探测不可达（沙箱正忙/超时）：不确定是否真的缺 DNS，如实给灰牌
        if (resolvCheck == null) {
            return unknownItem(
                id = "network_dns",
                category = DoctorCategory.NETWORK_SSL,
                title = "Phân giải tên miền DNS",
            )
        }

        val resolvContent = resolvCheck.stdout
        val hasNameserver = resolvContent.contains("nameserver", ignoreCase = true)

        if (!hasNameserver) {
            return DoctorItem(
                id = "network_dns",
                category = DoctorCategory.NETWORK_SSL,
                title = "Phân giải tên miền DNS",
                status = DoctorStatus.WARNING,
                summary = "Chưa cấu hình máy chủ phân giải DNS hợp lệ",
                detail = "/etc/resolv.conf trống hoặc mất, có thể không phân giải được tên miền tải phần mềm",
            )
        }

        return DoctorItem(
            id = "network_dns",
            category = DoctorCategory.NETWORK_SSL,
            title = "Phân giải tên miền DNS",
            status = DoctorStatus.HEALTHY,
            summary = "Cấu hình phân giải DNS bình thường",
            detail = resolvContent.lineSequence().filter { it.startsWith("nameserver") }.take(2).joinToString(", "),
        )
    }

    private suspend fun checkCaCertificates(): DoctorItem {
        val certCheck = probe(
            commandLine = "test -f /etc/ssl/certs/ca-certificates.crt || test -d /etc/ssl/certs",
            timeoutMs = 3000L,
        )

        return when {
            // 探测不可达：不把「没探测到」当成「证书缺失」
            certCheck == null -> unknownItem(
                id = "ca_certificates",
                category = DoctorCategory.NETWORK_SSL,
                title = "Chứng chỉ gốc SSL (CA)",
            )
            certCheck.isSuccess -> DoctorItem(
                id = "ca_certificates",
                category = DoctorCategory.NETWORK_SSL,
                title = "Chứng chỉ gốc SSL (CA)",
                status = DoctorStatus.HEALTHY,
                summary = "CA root đã sẵn sàng, hỗ trợ tải dependency qua HTTPS",
            )
            else -> DoctorItem(
                id = "ca_certificates",
                category = DoctorCategory.NETWORK_SSL,
                title = "Chứng chỉ gốc SSL (CA)",
                status = DoctorStatus.WARNING,
                summary = "Hệ thống thiếu chứng chỉ gốc CA",
                detail = "Khi tải tài nguyên HTTPS hoặc Git Clone có thể gặp lỗi xác minh SSL",
            )
        }
    }

    private suspend fun checkAptMirrors(): DoctorItem {
        val sourcesCheck = probe(
            commandLine = "cat /etc/apt/sources.list /etc/apt/sources.list.d/*.sources /etc/apt/sources.list.d/*.list 2>/dev/null || true",
            timeoutMs = 4000L,
        )

        // 探测不可达：不能因超时误报「官方默认源」（沙箱正忙时 cat 拿不到输出）
        if (sourcesCheck == null) {
            return unknownItem(
                id = "apt_mirrors",
                category = DoctorCategory.PACKAGE_MANAGER,
                title = "Nguồn gói phần mềm APT",
            )
        }

        val content = sourcesCheck.stdout
        // 命令带 || true，stdout 为空说明源配置整体不可读（而非官方源）——无法判定，如实灰牌
        if (content.isBlank()) {
            return unknownItem(
                id = "apt_mirrors",
                category = DoctorCategory.PACKAGE_MANAGER,
                title = "Nguồn gói phần mềm APT",
                detail = "Chưa đọc được nội dung cấu hình nguồn APT nào, không đánh giá được trạng thái image; sandbox đang bận hoặc danh sách nguồn bất thường, hãy khám lại sau",
            )
        }

        val hasInvalidUbuntuMirror = (content.contains("/ubuntu ") || content.contains("/ubuntu/")) &&
            !content.contains("ubuntu-ports")

        if (hasInvalidUbuntuMirror) {
            return DoctorItem(
                id = "apt_mirrors",
                category = DoctorCategory.PACKAGE_MANAGER,
                title = "Nguồn gói phần mềm APT",
                status = DoctorStatus.WARNING,
                summary = "Cấu hình nguồn APT bất thường (Ubuntu ARM64 cần dùng nguồn ubuntu-ports)",
                detail = "Phát hiện dùng đường dẫn image x86 trên kiến trúc ARM64, sẽ gây lỗi 404 gói phần mềm. Hãy bấm sửa một chạm để tự sửa.",
            )
        }

        val hasDomesticMirror = content.contains("tsinghua.edu.cn", ignoreCase = true) ||
            content.contains("aliyun.com", ignoreCase = true) ||
            content.contains("ustc.edu.cn", ignoreCase = true) ||
            content.contains("tencent.com", ignoreCase = true) ||
            content.contains("163.com", ignoreCase = true)

        return if (hasDomesticMirror) {
            DoctorItem(
                id = "apt_mirrors",
                category = DoctorCategory.PACKAGE_MANAGER,
                title = "Nguồn gói phần mềm APT",
                status = DoctorStatus.HEALTHY,
                summary = "Đã cấu hình tăng tốc bằng mirror trong nước (Tsinghua/Ali/Zhongkeda)",
            )
        } else {
            DoctorItem(
                id = "apt_mirrors",
                category = DoctorCategory.PACKAGE_MANAGER,
                title = "Nguồn gói phần mềm APT",
                status = DoctorStatus.WARNING,
                summary = "Hiện là nguồn mặc định chính thức, cài ở trong nước có thể chậm hoặc timeout",
                detail = "Nên một chạm chuyển sang mirror trong nước để tải dependency nhanh ổn định",
            )
        }
    }

    private suspend fun checkBaseDevTools(): DoctorItem {
        val toolsCheck = probe(
            commandLine = "for t in curl git tar xz file; do which \$t >/dev/null 2>&1 || echo \$t; done",
            timeoutMs = 4000L,
        )

        // 探测不可达：不把「没探测到」当成「工具缺失」
        if (toolsCheck == null) {
            return unknownItem(
                id = "base_devtools",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Toolchain cơ bản cốt lõi",
            )
        }

        val missingTools = toolsCheck.stdout.lines().map { it.trim() }.filter { it.isNotBlank() }

        return if (missingTools.isEmpty()) {
            DoctorItem(
                id = "base_devtools",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Toolchain cơ bản cốt lõi",
                status = DoctorStatus.HEALTHY,
                summary = "Git, Curl, Tar, XZ, File và các công cụ thường dùng đã sẵn sàng",
            )
        } else {
            DoctorItem(
                id = "base_devtools",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Toolchain cơ bản cốt lõi",
                status = DoctorStatus.WARNING,
                summary = "Thiếu công cụ cơ bản thường dùng: ${missingTools.joinToString(", ")}",
                detail = "Một phần script cài đặt hoặc plugin phụ thuộc các công cụ trên để giải nén và kéo code",
            )
        }
    }

    /**
     * 复用插件中心 android-core 组件的同一探针命令，保证体检与插件安装的状态判定口径一致。
     */
    private suspend fun checkAndroidEnvironment(): DoctorItem {
        val checkCommand = BuiltinPluginBundles.bundles
            .firstOrNull { it.id == "android-suite" }
            ?.components?.firstOrNull { it.id == "android-core" }
            ?.checkCommand

        val installed: Boolean?
        if (checkCommand.isNullOrBlank()) {
            installed = null
        } else {
            val res = probe(
                commandLine = checkCommand,
                timeoutMs = 8000L,
            )
            // 探测不可达时置 null：不把「没探测到」当成「环境未安装」
            installed = res?.isSuccess
        }

        val notInstalledItem = DoctorItem(
            id = "android_environment",
            category = DoctorCategory.DEV_RUNTIMES,
            title = "Môi trường phát triển Android",
            status = DoctorStatus.WARNING,
            summary = "Chưa cài môi trường Android / Flutter / dịch ngược",
            detail = "Có thể vào nhóm QQ chính thức tải gói plugin offline đầy đủ (đã tích hợp sẵn 3 môi trường Android, Flutter, dịch ngược, khỏi cài online), hoặc đến trung tâm plugin cài online.",
            fixable = false,
        )

        return when {
            // Bundle 数据未定义探针命令：按未安装引导（与历史行为一致）
            checkCommand.isNullOrBlank() -> notInstalledItem
            installed == true -> DoctorItem(
                id = "android_environment",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Môi trường phát triển Android",
                status = DoctorStatus.HEALTHY,
                summary = "JDK 17 / Android SDK / Gradle / NDK đã sẵn sàng, có thể build và dịch ngược APK",
            )
            // 探针执行了但沙箱不可达：灰牌，不误报「未安装」
            installed == null -> unknownItem(
                id = "android_environment",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Môi trường phát triển Android",
                detail = "Probe môi trường Android timeout, sandbox có thể đang bận (như đang chạy tác vụ build), khám lại sau là xác nhận được",
            )
            else -> notInstalledItem
        }
    }

    private suspend fun checkNodeRuntime(): DoctorItem {
        val nodeCheck = probe(
            commandLine = "node --version 2>/dev/null || /opt/taixu/bin/node --version 2>/dev/null || /usr/bin/node --version 2>/dev/null",
            timeoutMs = 4000L,
        )

        // 探测不可达：不把「没探测到」当成「Node 未安装」
        if (nodeCheck == null) {
            return unknownItem(
                id = "node_runtime",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Runtime Node.js",
            )
        }

        val rawVersion = nodeCheck.stdout.trim()
        if (rawVersion.isBlank()) {
            return DoctorItem(
                id = "node_runtime",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Runtime Node.js",
                status = DoctorStatus.WARNING,
                summary = "Chưa phát hiện runtime Node.js",
                detail = "Các AI tool như OpenClaw, Claude Code phụ thuộc nặng vào Node.js (nên >= v20)",
            )
        }

        val majorVersion = Regex("v?(\\d+)").find(rawVersion)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        return if (majorVersion >= 20) {
            DoctorItem(
                id = "node_runtime",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Runtime Node.js",
                status = DoctorStatus.HEALTHY,
                summary = "Node.js $rawVersion (đáp ứng yêu cầu của các AI tool phổ biến)",
            )
        } else {
            DoctorItem(
                id = "node_runtime",
                category = DoctorCategory.DEV_RUNTIMES,
                title = "Runtime Node.js",
                status = DoctorStatus.WARNING,
                summary = "Node.js $rawVersion phiên bản thấp (nên >= v20)",
                detail = "AI tool bản mới có thể cần tính năng JavaScript/V8 engine mới hơn",
            )
        }
    }
}
