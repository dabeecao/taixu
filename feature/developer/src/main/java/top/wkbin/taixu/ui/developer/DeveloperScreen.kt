package top.wkbin.taixu.ui.developer
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import top.wkbin.taixu.ui.components.RuntimeButton as Button
import androidx.compose.material3.HorizontalDivider
import top.wkbin.taixu.ui.components.RuntimeLinearProgressIndicator as LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import top.wkbin.taixu.ui.components.RuntimeSwitch as Switch
import top.wkbin.taixu.ui.developer.LocalizedText as Text
import top.wkbin.taixu.ui.components.RuntimeTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.wkbin.taixu.core.model.InstalledRuntime
import top.wkbin.taixu.core.model.RuntimeState
import top.wkbin.taixu.ui.components.EmptyPanel
import top.wkbin.taixu.ui.components.IconTile
import top.wkbin.taixu.ui.components.InfoRow
import top.wkbin.taixu.ui.components.NoticeBanner
import top.wkbin.taixu.ui.components.RuntimeCard
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeTopBar
import top.wkbin.taixu.ui.components.SectionHeader
import top.wkbin.taixu.ui.components.StatusBadge
import top.wkbin.taixu.runtime.bridge.adb.EmbeddedAdbManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import top.wkbin.taixu.runtime.RootfsUpdateInfo

@Composable
internal fun LocalDeveloperScreen(
    onBack: () -> Unit,
    viewModel: DeveloperViewModel? = previewOrNull { koinViewModel() },
) {
    val runtimeState by viewModel?.runtimeState?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.model.RuntimeState.Ready)
    val busy by viewModel?.busy?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val commandInput by viewModel?.commandInput?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val health by viewModel?.health?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val commandResult by viewModel?.commandResult?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val message by viewModel?.message?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val messageIsError by viewModel?.messageIsError?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val registryStatusIsError by viewModel?.registryStatusIsError?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val unusedRuntimes by viewModel?.unusedRuntimes?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val processes by viewModel?.processes?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val rootfsVersion by viewModel?.rootfsVersion?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val rootfsUpdate by viewModel?.rootfsUpdate?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val savedManifestUrl by viewModel?.registryManifestUrl?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val savedSignatureUrl by viewModel?.registrySignatureUrl?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val savedPublicKey by viewModel?.registryPublicKey?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val registryStatus by viewModel?.registryStatus?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val agentLoggingEnabled by viewModel?.agentLoggingEnabled?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val agentLogSize by viewModel?.agentLogSize?.collectAsStateWithLifecycle() ?: previewStateOf(0L)
    val agentLogLocation by viewModel?.agentLogLocation?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val adbState by viewModel?.adbState?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.bridge.adb.EmbeddedAdbManager.ConnectionState.Disconnected)
    val adbDiscovery by viewModel?.adbDiscovery?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.bridge.adb.EmbeddedAdbManager.DiscoveryState())
    val adbBusy by viewModel?.adbBusy?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val adbMessage by viewModel?.adbMessage?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val logcatOutput by viewModel?.logcatOutput?.collectAsStateWithLifecycle() ?: previewStateOf("")
    var manifestUrl by remember(savedManifestUrl) { mutableStateOf(savedManifestUrl) }
    var signatureUrl by remember(savedSignatureUrl) { mutableStateOf(savedSignatureUrl) }
    var publicKey by remember(savedPublicKey) { mutableStateOf(savedPublicKey) }
    var cleanupTarget by remember { mutableStateOf<InstalledRuntime?>(null) }
    var showRootfsUpdateConfirmation by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showAgentLogDialog by remember { mutableStateOf(false) }
    var agentLogText by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var logcatPackage by remember { mutableStateOf("") }
    var logcatTag by remember { mutableStateOf("") }
    var logcatKeyword by remember { mutableStateOf("") }
    var logcatLines by remember { mutableStateOf("200") }
    var logcatPriority by remember { mutableStateOf('V') }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(adbState) {
        if (adbState is EmbeddedAdbManager.ConnectionState.Connected) pairingCode = ""
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { RuntimeTopBar("Bảng điều khiển nhà phát triển", onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(top.wkbin.taixu.ui.components.runtimeScrollViewport(innerPadding))
                .verticalScroll(rememberScrollState()).padding(bottom = innerPadding.calculateBottomPadding())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            RuntimeControlCard(
                state = runtimeState,
                rootfsVersion = rootfsVersion,
                rootfsUpdate = rootfsUpdate,
                busy = busy,
                onInitialize = { viewModel?.initialize() },
                onCancel = { viewModel?.cancelInitialization() },
                onUpdate = { showRootfsUpdateConfirmation = true },
                onCheckUpdate = { viewModel?.checkRootfsUpdate() },
                onHealthCheck = { viewModel?.runHealthCheck() },
                onReset = { showResetConfirmation = true },
            )

            message?.let { NoticeBanner(it, isError = messageIsError) }

            health?.let { result ->
                RuntimeCard(
                    Modifier.fillMaxWidth(),
                    containerColor = if (result.isHealthy) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconTile(
                            if (result.isHealthy) RuntimeIconName.Shield else RuntimeIconName.Alert,
                            color = if (result.isHealthy) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (result.isHealthy) "Kiểm tra sức khỏe đạt" else "Kiểm tra sức khỏe bất thường", style = MaterialTheme.typography.titleLarge)
                            Text(result.detail ?: "Đã kiểm tra xong năng lực cốt lõi của Runtime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(15.dp))
                    InfoRow("Hệ thống", result.osRelease ?: "-")
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Kiến trúc", result.architecture ?: "-")
                    Spacer(Modifier.height(8.dp))
                    InfoRow("Không gian làm việc ghi được", if (result.workspaceWritable) "Có" else "Không")
                }
            }

            SectionHeader("ADB không dây · Logcat", "Tự phát hiện cổng động qua mDNS; sau một lần ghép nối thành công sẽ tự kết nối lại")
            RuntimeCard(Modifier.fillMaxWidth()) {
                val connected = adbState as? EmbeddedAdbManager.ConnectionState.Connected
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(RuntimeIconName.Terminal)
                    Column(Modifier.weight(1f)) {
                        Text("ADB không dây tích hợp", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (val current = adbState) {
                                EmbeddedAdbManager.ConnectionState.Disconnected -> "Chưa kết nối"
                                EmbeddedAdbManager.ConnectionState.Discovering -> "Đang phát hiện cổng qua mDNS"
                                EmbeddedAdbManager.ConnectionState.Pairing -> "Đang ghép nối an toàn"
                                EmbeddedAdbManager.ConnectionState.Connecting -> "Đang kết nối"
                                is EmbeddedAdbManager.ConnectionState.Connected -> "${current.host}:${current.port}"
                                is EmbeddedAdbManager.ConnectionState.Failed -> current.message
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    StatusBadge(if (connected != null) "Đã kết nối" else "Chưa sẵn sàng", if (connected != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Đã phát hiện: ${adbDiscovery.pairingEndpoints.size} cổng ghép nối · ${adbDiscovery.connectEndpoints.size} cổng kết nối",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { value -> pairingCode = value.filter(Char::isDigit).take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Mã ghép nối 6 chữ số dùng một lần") },
                    supportingText = { Text("Trước tiên hãy bật "Ghép nối thiết bị bằng mã ghép nối" trong "Gỡ lỗi không dây" của hệ thống; không cần nhập cổng thủ công") },
                    singleLine = true,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel?.pairWirelessAdb(pairingCode) },
                        enabled = !adbBusy && pairingCode.length == 6 && adbDiscovery.pairingEndpoints.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Ghép nối và kết nối") }
                    OutlinedButton(
                        onClick = { viewModel?.connectWirelessAdb() },
                        enabled = !adbBusy && adbDiscovery.connectEndpoints.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Kết nối lại") }
                }
                adbMessage?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(18.dp))
                Text("Thu thập nhật ký ứng dụng", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = logcatPackage,
                    onValueChange = { logcatPackage = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tên gói (để trống để lấy tất cả)") },
                    singleLine = true,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = logcatTag,
                        onValueChange = { logcatTag = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Tag (tùy chọn)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = logcatLines,
                        onValueChange = { logcatLines = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.weight(0.55f),
                        label = { Text("Số dòng") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = logcatKeyword,
                    onValueChange = { logcatKeyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Từ khóa (tùy chọn, không phân biệt hoa thường)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf('V', 'D', 'I', 'W', 'E', 'F').forEach { priority ->
                        if (priority == logcatPriority) {
                            Button(onClick = { logcatPriority = priority }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)) { Text(priority.toString()) }
                        } else {
                            OutlinedButton(onClick = { logcatPriority = priority }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)) { Text(priority.toString()) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel?.captureLogcat(logcatPackage, logcatTag, logcatPriority, logcatKeyword, logcatLines.toIntOrNull() ?: 200) },
                        enabled = connected != null && !adbBusy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Lấy nhật ký") }
                    OutlinedButton(onClick = { viewModel?.clearDeviceLogcat() }, enabled = connected != null && !adbBusy) { Text("Xóa hết") }
                }
                if (logcatOutput.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            logcatOutput,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Logcat", logcatOutput))
                            Toast.makeText(context, "Đã sao chép nhật ký", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Sao chép nhật ký") }
                }
            }

            SectionHeader("Nguồn công cụ", "Cập nhật danh sách từ xa: HTTPS + xác minh Ed25519 (khóa công khai tùy chỉnh chỉ chống hỏng khi truyền, không phải neo tin cậy chống giả mạo)")
            RuntimeCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 保存前校验：两个地址必须是 HTTPS，公钥不能为空
                    val manifestUrlValid = manifestUrl.trim().startsWith("https://")
                    val signatureUrlValid = signatureUrl.trim().startsWith("https://")
                    val publicKeyValid = publicKey.isNotBlank()
                    OutlinedTextField(
                        value = manifestUrl,
                        onValueChange = { manifestUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Địa chỉ HTTPS của danh sách") },
                        isError = manifestUrl.isNotBlank() && !manifestUrlValid,
                        supportingText = {
                            if (manifestUrl.isNotBlank() && !manifestUrlValid) Text("Địa chỉ phải bắt đầu bằng https://")
                        },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = signatureUrl,
                        onValueChange = { signatureUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Địa chỉ HTTPS của chữ ký") },
                        isError = signatureUrl.isNotBlank() && !signatureUrlValid,
                        supportingText = {
                            if (signatureUrl.isNotBlank() && !signatureUrlValid) Text("Địa chỉ phải bắt đầu bằng https://")
                        },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = publicKey,
                        onValueChange = { publicKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Khóa công khai Ed25519 (Base64)") },
                        isError = !publicKeyValid,
                        supportingText = {
                            if (!publicKeyValid) Text("Khóa công khai không được để trống")
                        },
                        minLines = 2,
                    )
                    Text(
                        "Lưu ý: người kiểm soát được địa chỉ danh sách cũng có thể cung cấp khóa công khai; tự nhập khóa chỉ để chống hỏng khi truyền/kiểm tra tính nhất quán nội dung, không chống giả mạo đầu-cuối. Nếu cần neo tin cậy thực sự, hãy nhúng khóa công khai cố định trong APK.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel?.saveRegistryConfig(manifestUrl.trim(), signatureUrl.trim(), publicKey.trim()) },
                            enabled = manifestUrlValid && signatureUrlValid && publicKeyValid,
                        ) { Text("Lưu cấu hình") }
                        OutlinedButton(
                            onClick = { viewModel?.updateRegistry() },
                            enabled = manifestUrlValid && signatureUrlValid && publicKeyValid,
                        ) { Text("Kiểm tra cập nhật") }
                    }
                    registryStatus?.let { NoticeBanner(it, isError = registryStatusIsError) }
                }
            }

            SectionHeader("Quản lý tài nguyên", "Tiến trình Linux nền & dependency dùng chung")

            RuntimeCard(Modifier.fillMaxWidth()) {
                ResourceHeader(RuntimeIconName.Cpu, "Tiến trình nền", "${processes.size} tiến trình đang hoạt động", { viewModel?.refreshProcesses() })
                Spacer(Modifier.height(14.dp))
                if (processes.isEmpty()) {
                    Text("Hiện không có tiến trình nền nào được đăng ký.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    processes.forEachIndexed { index, process ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(process.toolId ?: process.id, style = MaterialTheme.typography.titleMedium)
                                Text("${process.type} · PID ${process.pid ?: "未知"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel?.stopProcess(process.id) }, enabled = !busy) { Text("Dừng") }
                        }
                    }
                }
            }

            RuntimeCard(Modifier.fillMaxWidth()) {
                ResourceHeader(RuntimeIconName.Package, "Runtime có thể dọn", "Số tham chiếu bằng 0", { viewModel?.refreshUnusedRuntimes() })
                Spacer(Modifier.height(8.dp))
                Text("Ở đây chỉ liệt kê các dependency dùng chung không được công cụ nào tham chiếu; việc dọn dẹp sẽ chạy dọn package manager bên trong Linux sandbox.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                if (unusedRuntimes.isEmpty()) {
                    Text("Hiện không có Runtime dùng chung nào dọn được an toàn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    unusedRuntimes.forEachIndexed { index, runtime ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(runtime.name.name, style = MaterialTheme.typography.titleMedium)
                                Text(runtime.version ?: "Không rõ phiên bản", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { cleanupTarget = runtime }, enabled = !busy) { Text("Dọn dẹp") }
                        }
                    }
                }
            }

            SectionHeader("Chẩn đoán Agent", "Luồng thực thi Agent, lệnh gọi công cụ và chi tiết lỗi được lưu nội bộ")
            RuntimeCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(RuntimeIconName.Shield)
                    Column(Modifier.weight(1f)) {
                        Text("Nhật ký gỡ lỗi nội bộ của Agent", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (agentLoggingEnabled) "Đã bật (kích thước file: ${agentLogSize.toReadableSize()})" else "Đã tắt (không ghi file nhật ký nội bộ)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (agentLogLocation.isNotBlank()) {
                            Text(
                                "File nhật ký: $agentLogLocation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = agentLoggingEnabled,
                        onCheckedChange = { arg0 -> viewModel?.setAgentLoggingEnabled(arg0) },
                    )
                }
                if (agentLoggingEnabled || agentLogSize > 0L) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                agentLogText = viewModel?.readAgentLogs().orEmpty()
                                showAgentLogDialog = true
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Xem nhật ký")
                        }
                        OutlinedButton(
                            onClick = {
                                val logs = viewModel?.readAgentLogs().orEmpty()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("TaiXu Agent Logs", logs))
                                Toast.makeText(context, "Đã sao chép nhật ký vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Sao chép tất cả")
                        }
                        TextButton(
                            onClick = { viewModel?.clearAgentLogs() },
                        ) {
                            Text("Xóa hết", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            SectionHeader("Chẩn đoán lệnh", "Chạy lệnh Shell một lần trong môi trường Linux cô lập")
            RuntimeCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(RuntimeIconName.Terminal)
                    Column(Modifier.weight(1f)) {
                        Text("Shell Runner", style = MaterialTheme.typography.titleMedium)
                        Text("Phù hợp chẩn đoán nhanh, không tạo phiên terminal lâu dài", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { arg0 -> viewModel?.onCommandInputChanged(arg0) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lệnh Shell") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = { viewModel?.runCommand() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    RuntimeIcon(RuntimeIconName.Play, Modifier.size(18.dp)); Spacer(Modifier.size(7.dp)); Text("Chạy lệnh")
                }
                commandResult?.let { result ->
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(Color(0xFF080D16)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("exit ${result.exitCode}  •  ${result.durationMs} ms", style = MaterialTheme.typography.labelMedium, color = if (result.exitCode == 0) Color(0xFF83EDC9) else Color(0xFFFFA2AE))
                        Text(result.stdout.ifBlank { "(no stdout)" }, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFFD9E2F1))
                        if (result.stderr.isNotBlank()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                            Text(result.stderr, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = Color(0xFFFFA2AE))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    cleanupTarget?.let { runtime ->
        RuntimeAlertDialog(
            onDismissRequest = { cleanupTarget = null },
            title = { Text("Dọn ${runtime.name.name}?") },
            text = { Text("Sẽ gỡ các Runtime dùng chung không được công cụ nào tham chiếu, thao tác này không thể tự khôi phục.") },
            confirmButton = { TextButton(onClick = { cleanupTarget = null; viewModel?.cleanupRuntime(runtime.id) }) { Text("Xác nhận dọn", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { cleanupTarget = null }) { Text("Hủy") } },
        )
    }
    if (showResetConfirmation) {
        var countdown by remember { mutableStateOf(3) }
        LaunchedEffect(Unit) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
        RuntimeAlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Đặt lại sandbox hiện tại?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Sẽ xóa các gói tự cài và sửa đổi hệ thống", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("• Không xóa code dự án trong /workspace", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showResetConfirmation = false; viewModel?.resetLinuxEnvironment() },
                    enabled = countdown == 0,
                ) {
                    Text(
                        if (countdown > 0) "Xác nhận đặt lại (${countdown}s)" else "Xác nhận đặt lại",
                        color = if (countdown == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    )
                }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Hủy") } },
        )
    }
    if (showRootfsUpdateConfirmation) {
        RuntimeAlertDialog(
            onDismissRequest = { showRootfsUpdateConfirmation = false },
            title = { Text("Cập nhật Linux RootFS?") },
            text = { Text("Sẽ tải và xác minh phiên bản mới trực tuyến, giữ lại /root và /opt/taixu. Trong lúc cập nhật, tiến trình Linux nền sẽ dừng; nếu thất bại sẽ tự khôi phục phiên bản cũ.") },
            confirmButton = { TextButton(onClick = { showRootfsUpdateConfirmation = false; viewModel?.updateRootfs() }) { Text("Xác nhận cập nhật") } },
            dismissButton = { TextButton(onClick = { showRootfsUpdateConfirmation = false }) { Text("Hủy") } },
        )
    }
    if (showAgentLogDialog) {
        RuntimeAlertDialog(
            onDismissRequest = { showAgentLogDialog = false },
            title = { Text("Nhật ký gỡ lỗi nội bộ của Agent") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        agentLogText.ifBlank { "Chưa có nhật ký" },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAgentLogDialog = false }) { Text("Đóng") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("TaiXu Agent Logs", agentLogText))
                    Toast.makeText(context, "Đã sao chép nhật ký", Toast.LENGTH_SHORT).show()
                }) { Text("Sao chép") }
            },
        )
    }
}

@Composable
private fun RuntimeControlCard(
    state: RuntimeState,
    rootfsVersion: String?,
    rootfsUpdate: RootfsUpdateInfo?,
    busy: Boolean,
    onInitialize: () -> Unit,
    onCancel: () -> Unit,
    onUpdate: () -> Unit,
    onCheckUpdate: () -> Unit,
    onHealthCheck: () -> Unit,
    onReset: () -> Unit,
) {
    val color = when (state) {
        RuntimeState.Ready -> MaterialTheme.colorScheme.secondary
        is RuntimeState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    RuntimeCard(Modifier.fillMaxWidth(), containerColor = color.copy(alpha = 0.08f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(RuntimeIconName.Terminal, color = color)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Linux Runtime", style = MaterialTheme.typography.titleLarge)
                Text("RootFS ${rootfsVersion ?: "未安装"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                rootfsUpdate?.let { info ->
                    Text(
                        if (info.hasUpdate) "Phát hiện có bản cập nhật" else "Đã mới nhất",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (info.hasUpdate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusBadge(describeState(state), color)
        }
        val initializing = state as? RuntimeState.Initializing
        if (initializing != null) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = { initializing.progress }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(displayStep(initializing.step), style = MaterialTheme.typography.bodySmall)
                Text("${(initializing.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            }
            initializing.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Hủy tải xuống") }
        } else {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onInitialize, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (state is RuntimeState.Error) "Thử khởi tạo lại" else if (state is RuntimeState.Ready) "Kiểm tra khởi tạo lại" else "Tải xuống và khởi tạo")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onHealthCheck,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text("Kiểm tra sức khỏe", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onCheckUpdate,
                    enabled = !busy && state is RuntimeState.Ready,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text("Kiểm tra cập nhật", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onUpdate,
                    enabled = !busy && state is RuntimeState.Ready,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Text("Cập nhật RootFS", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = onReset,
                enabled = !busy && state !is RuntimeState.Initializing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Khôi phục trạng thái ban đầu của Linux", color = MaterialTheme.colorScheme.error) }
        }
        (state as? RuntimeState.Error)?.let {
            Spacer(Modifier.height(12.dp))
            // 面向用户的提示与原始异常解耦：原始异常进日志，避免技术文本直接展示
            androidx.compose.runtime.LaunchedEffect(it.throwable) {
                Log.w("DeveloperScreen", "Runtime error: ${it.throwable.message}")
            }
            NoticeBanner("Khởi tạo Runtime thất bại, hãy kiểm tra mạng và dung lượng rồi bấm "Thử khởi tạo lại"", isError = true)
        }
    }
}

@Composable
private fun ResourceHeader(icon: RuntimeIconName, title: String, detail: String, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onRefresh) { Text("Làm mới") }
    }
}

private fun Long.toReadableSize(): String {
    if (this < 1024L) return "$this B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = this.toDouble()
    var index = -1
    while (value >= 1024 && index < units.lastIndex) { value /= 1024; index += 1 }
    return "%.1f %s".format(value, units[index])
}

private fun describeState(state: RuntimeState): String = when (state) {
    RuntimeState.NotInitialized -> "Chưa khởi tạo"
    is RuntimeState.Initializing -> "Đang khởi tạo"
    RuntimeState.Ready -> "Đã sẵn sàng"
    is RuntimeState.Error -> "Bất thường"
}

private fun displayStep(step: String): String = when (step) {
    "detectArchitecture" -> "Phát hiện kiến trúc thiết bị"
    "checkStorage" -> "Kiểm tra dung lượng trống"
    "createDirectories" -> "Tạo thư mục chạy"
    "Chuẩn bị PRoot" -> "Chuẩn bị thành phần khởi động PRoot"
    "configureRootfs" -> "Cấu hình hệ thống Linux"
    "configureDns" -> "Cấu hình DNS"
    "configureEnvironment" -> "Cấu hình biến môi trường"
    "createWorkspace" -> "Tạo không gian làm việc"
    "runHealthCheck" -> "Chạy kiểm tra sức khỏe"
    else -> step
}
