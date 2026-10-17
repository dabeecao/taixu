package top.wkbin.taixu.ui.settings
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.ui.settings.LocalizedText as Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.res.stringResource
import top.wkbin.taixu.feature.settings.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.wkbin.taixu.core.model.ExecutionMode
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeCircularProgressIndicator as CircularProgressIndicator
import top.wkbin.taixu.ui.components.RuntimeIconButton as IconButton
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeTextButton as TextButton
/**
 * 二级子页 2：Linux 容器沙箱与存储
 */
@Composable
fun LinuxEnvironmentSettingsScreen(
    onBack: () -> Unit,
    onOpenDistroManagement: () -> Unit,
    onOpenStorageMounts: () -> Unit,
    onOpenStorageUsage: () -> Unit,
    onOpenAppManagement: () -> Unit,
    onOpenEnvironmentVariables: () -> Unit,
    onOpenSshSettings: () -> Unit,
    onOpenFtpSettings: () -> Unit,
    viewModel: SettingsViewModel? = previewOrNull { koinViewModel() },
) {
    val executionMode by viewModel?.executionMode?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.model.ExecutionMode.PROOT)
    val effectiveExecutionMode by viewModel?.effectiveExecutionMode?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.model.ExecutionMode.PROOT)
    val privilegeState by viewModel?.privilegeState?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.privilege.PrivilegeState())
    val switchingMode by viewModel?.switchingMode?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val installedDistros by viewModel?.installedDistros?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val webChatStatus by viewModel?.webChatStatus?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.webchat.WebChatServerStatus())

    var showExecutionModeDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showWebChatDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var privilegeResultMessage by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    // Web 开关切换进行中指示，防止重复点击
    var webChatToggling by remember { mutableStateOf(false) }
    LaunchedEffect(webChatStatus.isRunning) { webChatToggling = false }

    if (showExecutionModeDialog) {
        ExecutionModeDialog(
            currentMode = executionMode,
            switching = switchingMode,
            onSelectMode = { mode ->
                showExecutionModeDialog = false
                viewModel?.switchExecutionMode(mode) { success, msg ->
                    privilegeResultMessage = if (success) null else msg
                }
            },
            onDismiss = { showExecutionModeDialog = false },
        )
    }

    privilegeResultMessage?.let { errorMsg ->
        RuntimeAlertDialog(
            onDismissRequest = { privilegeResultMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuntimeIcon(RuntimeIconName.Alert, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                    Text("Ủy quyền chế độ chạy không đạt")
                }
            },
            text = { Text(errorMsg, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { privilegeResultMessage = null }) {
                    Text("Đã hiểu")
                }
            },
        )
    }

    if (showWebChatDialog) {
        WebChatBridgeDialog(
            status = webChatStatus,
            toggling = webChatToggling,
            onToggle = { enabled ->
                webChatToggling = true
                viewModel?.toggleWebChatServer(enabled)
            },
            onDismiss = { showWebChatDialog = false },
        )
    }

    PersonalizationScaffold(stringResource(R.string.personal_linux_title), onBack) {
        item {
            PersonalizationSection("Hệ thống container & quản lý sandbox") {
                PreferenceSurface(first = true, last = false) {
                    SettingsRow(
                        icon = RuntimeIconName.Server,
                        title = "Quản lý distro Linux",
                        subtitle = "Nhiều sandbox cùng tồn tại · kéo image · chuyển hệ thống chính một chạm",
                        value = "${installedDistros.size} hệ thống",
                        onClick = onOpenDistroManagement,
                    )
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(
                        icon = RuntimeIconName.Storage,
                        title = "Quản lý lưu trữ",
                        subtitle = "Phân tích mức dùng theo Linux, plugin, dự án và Skills",
                        onClick = onOpenStorageUsage,
                    )
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(
                        icon = RuntimeIconName.SdCard,
                        title = "Mount & chia sẻ lưu trữ",
                        subtitle = "Ánh xạ lưu trữ host PRoot (-b /sdcard)",
                        onClick = onOpenStorageMounts,
                    )
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(
                        icon = RuntimeIconName.Key,
                        title = "Biến môi trường",
                        subtitle = "Tiêm biến người dùng cho terminal, Agent và công cụ",
                        onClick = onOpenEnvironmentVariables,
                    )
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(
                        icon = RuntimeIconName.Network,
                        title = "Truy cập từ xa SSH",
                        subtitle = "Xác thực khóa công khai · lắng nghe cổng & LAN · khởi động cùng runtime",
                        onClick = onOpenSshSettings,
                    )
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(
                        icon = RuntimeIconName.FolderOpen,
                        title = "Truy cập file từ xa FTP",
                        subtitle = "Mount thư mục gốc Linux (/) · kết nối trực tiếp FileZilla / trình quản lý file",
                        onClick = onOpenFtpSettings,
                    )
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(
                        icon = RuntimeIconName.Globe,
                        title = "Bàn cộng tác Web TaiXu Agent",
                        subtitle = if (webChatStatus.isRunning) {
                            "Đang chạy · ${webChatStatus.accessUrl} (PIN: ${webChatStatus.pinCode})"
                        } else {
                            "Dùng trình duyệt máy tính trong cùng Wi-Fi để truy cập TaiXu Agent và không gian làm việc"
                        },
                        value = if (webChatStatus.isRunning) "Đã bật" else "Chưa bật",
                        onClick = { showWebChatDialog = true },
                    )
                }
                PreferenceSurface(first = false, last = true) {
                    SettingsRow(
                        icon = RuntimeIconName.Package,
                        title = "Quản lý ứng dụng",
                        subtitle = "Đồng bộ ứng dụng hệ thống/người dùng, xem trạng thái tắt, đóng băng và hạn chế mạng nền",
                        onClick = onOpenAppManagement,
                    )
                }
            }
        }

        item {
            PersonalizationSection("Đặc quyền tầng hệ thống") {
                PreferenceSurface(first = true, last = true) {
                    SettingsRow(
                        icon = RuntimeIconName.Key,
                        title = "Chế độ đặc quyền chạy hệ thống",
                        subtitle = if (executionMode != effectiveExecutionMode) {
                            "${executionMode.shortLabel} ưu tiên tạm chưa khả dụng: ${privilegeState.reason}"
                        } else {
                            "PRoot userspace sandbox · Shizuku · Root"
                        },
                        value = if (executionMode == effectiveExecutionMode) {
                            effectiveExecutionMode.shortLabel
                        } else {
                            "${effectiveExecutionMode.shortLabel} (đã hạ cấp)"
                        },
                        onClick = { showExecutionModeDialog = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutionModeDialog(
    currentMode: ExecutionMode,
    switching: Boolean,
    onSelectMode: (ExecutionMode) -> Unit,
    onDismiss: () -> Unit,
) {
    RuntimeAlertDialog(
        onDismissRequest = { if (!switching) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RuntimeIcon(RuntimeIconName.Shield, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Chọn chế độ chạy hệ thống", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Chuyển chế độ đặc quyền sẽ tự phát hiện ủy quyền; ủy quyền thành công sẽ giải phóng ngay các năng lực hệ thống & phần cứng nâng cao tương ứng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (switching) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(12.dp))
                        Text("Đang dò đặc quyền và xin ủy quyền…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                ExecutionMode.entries.forEach { mode ->
                    ExecutionModeOptionItem(
                        mode = mode,
                        selected = currentMode == mode,
                        enabled = !switching,
                        onClick = { onSelectMode(mode) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !switching) {
                Text("Đóng")
            }
        },
    )
}

@Composable
private fun ExecutionModeOptionItem(
    mode: ExecutionMode,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (selected) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            "Đang kích hoạt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = mode.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Yêu cầu: ${mode.requiredPrivilege}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
