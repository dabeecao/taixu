package top.wkbin.taixu.ui.developer
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.wkbin.taixu.runtime.bridge.adb.EmbeddedAdbManager
import top.wkbin.taixu.ui.components.IconTile
import top.wkbin.taixu.ui.components.NoticeBanner
import top.wkbin.taixu.ui.components.RuntimeButton as Button
import top.wkbin.taixu.ui.components.RuntimeCard
import top.wkbin.taixu.ui.components.RuntimeFilledTonalButton as FilledTonalButton
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeLinearProgressIndicator as LinearProgressIndicator
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton as OutlinedButton
import top.wkbin.taixu.ui.components.RuntimeSwitch as Switch
import top.wkbin.taixu.ui.components.RuntimeTopBar
import top.wkbin.taixu.ui.components.SectionHeader
import top.wkbin.taixu.ui.components.StatusBadge
import top.wkbin.taixu.ui.developer.LocalizedText as Text
/**
 * 独立的无线 ADB 与日志抓取工作台页面。
 *
 * 提供：
 * 1. mDNS 自动发现端点展示与自动重连状态；
 * 2. TLS + SPAKE2 一键安全配对（只需在系统无线调试输入一次配对码）；
 * 3. Logcat 多维过滤（包名/Tag/优先级/关键词/行数）与实时抓取预览；
 * 4. 日志复制与设备缓冲区清空。
 */
@Composable
fun AdbLogcatScreen(
    onBack: () -> Unit,
    viewModel: DeveloperViewModel? = previewOrNull { koinViewModel() },
) {
    val adbState by viewModel?.adbState?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.bridge.adb.EmbeddedAdbManager.ConnectionState.Disconnected)
    val adbDiscovery by viewModel?.adbDiscovery?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.bridge.adb.EmbeddedAdbManager.DiscoveryState())
    val adbBusy by viewModel?.adbBusy?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val adbMessage by viewModel?.adbMessage?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val logcatOutput by viewModel?.logcatOutput?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val adbNotificationEnabled by viewModel?.adbNotificationEnabled?.collectAsStateWithLifecycle() ?: previewStateOf(false)

    var pairingCode by rememberSaveable { mutableStateOf("") }
    var explicitPort by rememberSaveable { mutableStateOf("") }
    var logcatPackage by rememberSaveable { mutableStateOf("top.wkbin.taixu") }
    var logcatTag by rememberSaveable { mutableStateOf("") }
    var logcatKeyword by rememberSaveable { mutableStateOf("") }
    var logcatLines by rememberSaveable { mutableStateOf("200") }
    var logcatPriority by rememberSaveable { mutableStateOf('V') }

    val context = LocalContext.current
    val connected = adbState as? EmbeddedAdbManager.ConnectionState.Connected

    LaunchedEffect(adbState) {
        if (adbState is EmbeddedAdbManager.ConnectionState.Connected) {
            pairingCode = ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { RuntimeTopBar("ADB không dây và lấy log", onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(top.wkbin.taixu.ui.components.runtimeScrollViewport(innerPadding))
                .verticalScroll(rememberScrollState()).padding(bottom = innerPadding.calculateBottomPadding())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── 1. 连接状态与概览 ──────────────────────────────────────────
            RuntimeCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(
                        icon = RuntimeIconName.Terminal,
                        color = if (connected != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Máy khách ADB không dây tích hợp", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when (val current = adbState) {
                                EmbeddedAdbManager.ConnectionState.Disconnected -> "Chưa kết nối"
                                EmbeddedAdbManager.ConnectionState.Discovering -> "Đang tự dò cổng gỡ lỗi qua mDNS…"
                                EmbeddedAdbManager.ConnectionState.Pairing -> "Đang ghép nối an toàn…"
                                EmbeddedAdbManager.ConnectionState.Connecting -> "Đang thiết lập kết nối ADB…"
                                is EmbeddedAdbManager.ConnectionState.Connected -> "Đã kết nối: ${current.host}:${current.port}"
                                is EmbeddedAdbManager.ConnectionState.Failed -> current.message
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (adbState is EmbeddedAdbManager.ConnectionState.Failed) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StatusBadge(
                        text = when (adbState) {
                            is EmbeddedAdbManager.ConnectionState.Connected -> "Đã kết nối"
                            EmbeddedAdbManager.ConnectionState.Discovering -> "Đang dò"
                            EmbeddedAdbManager.ConnectionState.Pairing -> "Đang ghép nối"
                            EmbeddedAdbManager.ConnectionState.Connecting -> "Đang kết nối"
                            is EmbeddedAdbManager.ConnectionState.Failed -> "Lỗi"
                            EmbeddedAdbManager.ConnectionState.Disconnected -> "Chưa sẵn sàng"
                        },
                        color = when (adbState) {
                            is EmbeddedAdbManager.ConnectionState.Connected -> MaterialTheme.colorScheme.secondary
                            is EmbeddedAdbManager.ConnectionState.Failed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                }

                if (adbBusy || adbState == EmbeddedAdbManager.ConnectionState.Discovering || adbState == EmbeddedAdbManager.ConnectionState.Connecting) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Tự dò mDNS: tìm thấy ${adbDiscovery.pairingEndpoints.size} điểm ghép nối · ${adbDiscovery.connectEndpoints.size} điểm kết nối",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Trợ lý ghép nối trên thanh thông báo",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        )
                        Text(
                            "Luôn hiển thị ô nhập mã ghép nối trên thanh thông báo hệ thống, nhập nhanh không cần chuyển màn hình",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = adbNotificationEnabled, onCheckedChange = { viewModel?.setAdbNotificationEnabled(it) },
                    )
                }
            }

            // ── 2. 配对与重连控制台 ──────────────────────────────────────────
            SectionHeader("Ghép nối và kết nối an toàn", "Khóa được lưu bền trong thư mục riêng của ứng dụng; sau một lần ghép nối không cần dò cổng nữa")
            RuntimeCard(Modifier.fillMaxWidth()) {
                NoticeBanner(
                    text = "💡 Gợi ý ghép nối nhanh qua thanh thông báo: khi Android hiện popup \"Ghép nối thiết bị bằng mã ghép nối\", việc rời khỏi Cài đặt sẽ đóng popup và làm mã hết hiệu lực. Sau khi bật \"Trợ lý ghép nối trên thanh thông báo\" ở trên, bạn có thể kéo thanh thông báo ngay trong popup tùy chọn nhà phát triển để nhập mã 6 chữ số và gửi, không cần chuyển màn hình!",
                    isError = false,
                )
                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }.onFailure {
                            Toast.makeText(context, "Không mở trực tiếp được tùy chọn nhà phát triển, hãy bật thủ công trong cài đặt hệ thống", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Mở tùy chọn nhà phát triển hệ thống (bật gỡ lỗi không dây)")
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { value -> pairingCode = value.filter(Char::isDigit).take(6) },
                        modifier = Modifier.weight(1.1f),
                        label = { Text("Mã ghép nối 6 chữ số") },
                        placeholder = { Text("Ví dụ: 123456") },
                        supportingText = { Text("Điền khi ghép nối lần đầu") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = explicitPort,
                        onValueChange = { value -> explicitPort = value.filter(Char::isDigit).take(5) },
                        modifier = Modifier.weight(0.9f),
                        label = { Text("Cổng kết nối (tùy chọn)") },
                        placeholder = { Text("Ví dụ: 41235") },
                        supportingText = { Text("Để trống để mDNS tự dò") },
                        singleLine = true,
                    )
                }
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel?.pairWirelessAdb(pairingCode, explicitPort.toIntOrNull()) },
                        enabled = !adbBusy && pairingCode.length == 6 && (adbDiscovery.pairingEndpoints.isNotEmpty() || explicitPort.isNotBlank()),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Hoàn tất ghép nối và kết nối")
                    }
                    OutlinedButton(
                        onClick = { viewModel?.connectWirelessAdb(explicitPort.toIntOrNull()) },
                        enabled = !adbBusy && (adbDiscovery.connectEndpoints.isNotEmpty() || explicitPort.isNotBlank() || adbState is EmbeddedAdbManager.ConnectionState.Connected),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (explicitPort.isNotBlank()) "Kết nối cổng đã chỉ định" else "Kết nối lại")
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(
                        onClick = { viewModel?.restartAdbDiscovery() },
                        enabled = !adbBusy,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("Dò lại cổng", style = MaterialTheme.typography.labelSmall)
                    }
                }

                adbMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── 3. 日志抓取工作台 ───────────────────────────────────────────
            SectionHeader("Lấy log Logcat", "Chỉ định tên gói ứng dụng, tag và điều kiện lọc để lấy và phân tích log ngay tại chỗ")
            RuntimeCard(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = logcatPackage,
                    onValueChange = { logcatPackage = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tên gói ứng dụng mục tiêu (để trống để lấy log toàn hệ thống)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { logcatPackage = "top.wkbin.taixu" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("Điền TaiXu", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { logcatPackage = "" },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text("Nhật ký toàn hệ thống", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = logcatTag,
                        onValueChange = { logcatTag = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Lọc tag (tùy chọn)") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = logcatLines,
                        onValueChange = { logcatLines = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.weight(0.6f),
                        label = { Text("Số dòng") },
                        singleLine = true,
                    )
                }
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = logcatKeyword,
                    onValueChange = { logcatKeyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Lọc từ khóa (tùy chọn, không phân biệt hoa/thường)") },
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))

                Text("Mức độ log", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf('V' to "Verbose", 'D' to "Debug", 'I' to "Info", 'W' to "Warn", 'E' to "Error", 'F' to "Fatal").forEach { (priority, _) ->
                        if (priority == logcatPriority) {
                            Button(
                                onClick = { logcatPriority = priority },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                            ) {
                                Text(priority.toString())
                            }
                        } else {
                            OutlinedButton(
                                onClick = { logcatPriority = priority },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                            ) {
                                Text(priority.toString())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel?.captureLogcat(
                                logcatPackage,
                                logcatTag,
                                logcatPriority,
                                logcatKeyword,
                                logcatLines.toIntOrNull() ?: 200,
                            )
                        },
                        enabled = connected != null && !adbBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Lấy log")
                    }
                    OutlinedButton(
                        onClick = { viewModel?.clearDeviceLogcat() },
                        enabled = connected != null && !adbBusy,
                    ) {
                        Text("Xóa bộ đệm")
                    }
                }

                if (logcatOutput.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text("Kết quả", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    logcatOutput,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Logcat", logcatOutput))
                            Toast.makeText(context, "Đã sao chép log vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sao chép toàn bộ log")
                    }
                }
            }

            // ── 4. 沙箱 CLI 与 Agent 联动说明 ──────────────────────────────
            SectionHeader("Terminal sandbox và phối hợp Agent", "Gọi trực tiếp trong dòng lệnh Linux hoặc cuộc trò chuyện AI")
            RuntimeCard(Modifier.fillMaxWidth()) {
                Text(
                    "• Lệnh terminal: đã cài sẵn logcat-grabber <tên gói>, logcat-tail <tên gói> và logcat-export <tên gói> <đường dẫn>\n" +
                    "• Giao diện máy chủ: gọi trực tiếp xuyên sandbox qua taixu-host logcat\n" +
                    "• Phối hợp AI: trong cuộc trò chuyện, hãy yêu cầu Agent \"lấy giúp tôi log crash\", hệ thống sẽ tự gọi công cụ host.logcat để phân tích",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
