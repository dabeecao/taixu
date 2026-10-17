package top.wkbin.taixu.ui.settings
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import top.wkbin.taixu.ui.components.RuntimeFilledTextField
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.wkbin.taixu.runtime.FtpServiceState
import top.wkbin.taixu.ui.components.NoticeBanner
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeButton
import top.wkbin.taixu.ui.components.RuntimeCircularProgressIndicator
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton
import top.wkbin.taixu.ui.components.RuntimeSwitch
import top.wkbin.taixu.ui.components.RuntimeTextButton
import top.wkbin.taixu.ui.components.RuntimeSettingsScaffold
import top.wkbin.taixu.ui.settings.LocalizedText as Text
@Composable
fun FtpSettingsScreen(
    onBack: () -> Unit,
    viewModel: FtpSettingsViewModel? = previewOrNull { koinViewModel() },
) {
    val settings by viewModel?.settings?.collectAsStateWithLifecycle() ?: previewStateOf(FtpSettingsUiState())
    val serviceState by viewModel?.serviceState?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.FtpServiceState.Stopped("ubuntu"))
    val operating by viewModel?.operating?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val message by viewModel?.message?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val messageIsError by viewModel?.messageIsError?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val logs by viewModel?.logs?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val vpnActive by viewModel?.vpnActive?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val context = LocalContext.current

    var portText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(settings.port.toString()) }
    var usernameText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(settings.username) }
    var showPasswordDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showUsernameDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(settings.distroId, settings.port) { portText = settings.port.toString() }
    LaunchedEffect(settings.distroId, settings.username) { usernameText = settings.username }

    message?.let { current ->
        RuntimeAlertDialog(
            onDismissRequest = { viewModel?.consumeMessage() },
            title = { Text(if (messageIsError) "Thao tác FTP chưa hoàn tất" else "Dịch vụ tệp từ xa FTP") },
            text = { Text(current) },
            confirmButton = { RuntimeTextButton(onClick = { viewModel?.consumeMessage() }) { Text("Đã rõ") } },
        )
    }

    if (showPasswordDialog) {
        FtpPasswordDialog(
            passwordConfigured = settings.passwordConfigured,
            onDismiss = { showPasswordDialog = false },
            onSave = { password ->
                showPasswordDialog = false
                viewModel?.savePassword(password)
            },
            onClear = {
                showPasswordDialog = false
                viewModel?.clearPassword()
            },
        )
    }

    if (showUsernameDialog) {
        FtpUsernameDialog(
            currentUsername = settings.username,
            onDismiss = { showUsernameDialog = false },
            onSave = { user ->
                showUsernameDialog = false
                viewModel?.saveUsername(user)
            },
        )
    }

    val statusText = when (serviceState) {
        is FtpServiceState.Stopped -> "Đã dừng"
        is FtpServiceState.Starting -> "Đang khởi động"
        is FtpServiceState.Running -> "Đang chạy"
        is FtpServiceState.Failed -> "Khởi động thất bại"
    }
    val busy = operating || serviceState is FtpServiceState.Starting

    RuntimeSettingsScaffold(
        title = "Truy cập tệp từ xa FTP", onBack = onBack,
        statusText = statusText,
    ) {
        item {
            NoticeBanner(
                text = "FTP kết nối trực tiếp vào hệ thống tệp gốc (/) của container Linux hiện tại; trong cùng mạng Wi-Fi, có thể dùng FileZilla, Cyberduck trên máy tính hoặc Windows Explorer để tải lên/tải xuống tệp lớn và quản lý thư mục nhanh chóng.",
            )
        }

        item {
            SectionLabel("Trạng thái dịch vụ")
            SettingsGroup {
                SettingsRow(
                    icon = RuntimeIconName.Server,
                    title = "Bật dịch vụ FTP",
                    subtitle = "Giữ bật và tự khởi động cùng runtime Linux",
                    value = if (busy) {
                        "Đang xử lý..."
                    } else {
                        when (serviceState) {
                            is FtpServiceState.Running -> "Đang chạy"
                            is FtpServiceState.Starting -> "Đang khởi động"
                            is FtpServiceState.Failed -> "Thất bại"
                            is FtpServiceState.Stopped -> "Đã dừng"
                        }
                    },
                    trailing = {
                        if (busy) {
                            RuntimeCircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                            )
                        } else {
                            RuntimeSwitch(
                                checked = settings.enabled,
                                onCheckedChange = { arg0 -> viewModel?.toggleEnabled(arg0) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    },
                )
            }
            (serviceState as? FtpServiceState.Failed)?.let { failed ->
                SettingsDetailCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                ) {
                    Text(failed.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            SectionLabel("Địa chỉ kết nối và tham số cấu hình client")
            SettingsDetailCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tham số kết nối cho app bên ngoài (điền từng mục):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ParamRow(
                        label = "Cổng dịch vụ (Port)",
                        value = settings.port.toString(),
                        highlight = true,
                        onCopy = { copyText(context, settings.port.toString(), "Đã sao chép cổng FTP (${settings.port})") },
                    )
                    ParamRow(
                        label = "IP máy chủ nội bộ (Host - app khác trên điện thoại kết nối)",
                        value = "127.0.0.1",
                        highlight = true,
                        onCopy = { copyText(context, "127.0.0.1", "Đã sao chép IP nội bộ (127.0.0.1)") },
                    )
                    ParamRow(
                        label = "IP máy chủ mạng LAN (Host - máy tính/thiết bị ngoài kết nối)",
                        value = settings.connectionHost,
                        onCopy = { copyText(context, settings.connectionHost, "Đã sao chép IP mạng LAN (${settings.connectionHost})") },
                    )
                    ParamRow(
                        label = "Tên đăng nhập (Username)",
                        value = settings.username,
                        onCopy = { copyText(context, settings.username, "Đã sao chép tên người dùng (${settings.username})") },
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Text(
                        text = "URL kết nối nhanh:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    ParamRow(
                        label = "URL truy cập mạng LAN / máy tính",
                        value = settings.connectionUrl,
                        onCopy = { copyText(context, settings.connectionUrl, "Đã sao chép địa chỉ FTP mạng LAN") },
                    )
                    ParamRow(
                        label = "URL truy cập cho app khác trên điện thoại",
                        value = settings.localUrl,
                        onCopy = { copyText(context, settings.localUrl, "Đã sao chép địa chỉ FTP nội bộ") },
                    )

                    Text(
                        text = "Gợi ý sử dụng: khi thêm FTP trong trình quản lý tệp bên thứ ba trên điện thoại (như MT Manager/Solid Explorer/CX File Manager), host điền 127.0.0.1, cổng điền ${settings.port}; máy tính trong cùng Wi-Fi điền host là ${settings.connectionHost}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (vpnActive) {
            item {
                NoticeBanner(
                    text = "Phát hiện điện thoại đang kết nối VPN, VPN có thể chiếm lưu lượng mạng LAN khiến thiết bị ngoài không kết nối được. Nếu kết nối quá thời gian, hãy tạm dừng VPN hoặc thêm ứng dụng này vào danh sách trắng kết nối trực tiếp.",
                    isError = true,
                )
            }
        }

        item {
            SectionLabel("Xác thực đăng nhập và quyền")
            SettingsGroup {
                SettingsRow(
                    icon = RuntimeIconName.Admin,
                    title = "Tên đăng nhập",
                    subtitle = "Tên người dùng khi FTP client kết nối",
                    value = settings.username,
                    onClick = { showUsernameDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                val passwordSubtitle = if (settings.passwordConfigured) {
                    "Mật khẩu đã được cấu hình riêng và lưu mã hóa, chạm để sửa hoặc xóa"
                } else if (settings.sshPasswordConfigured) {
                    "Chưa đặt riêng, đang tự động dùng mật khẩu đăng nhập SSH"
                } else {
                    "Chưa đặt mật khẩu (đăng nhập không cần mật khẩu, client để trống hoặc điền gì cũng được)"
                }
                val passwordValue = if (settings.passwordConfigured) {
                    "Đã đặt"
                } else if (settings.sshPasswordConfigured) {
                    "Giống mật khẩu SSH"
                } else {
                    "Không cần mật khẩu"
                }
                SettingsRow(
                    icon = RuntimeIconName.Key,
                    title = "Mật khẩu đăng nhập",
                    subtitle = passwordSubtitle,
                    value = passwordValue,
                    onClick = { showPasswordDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Shield,
                    title = "Cho phép truy cập ẩn danh",
                    subtitle = if (settings.anonymousEnabled) "Client bên ngoài đăng nhập không cần tên người dùng/mật khẩu" else "Yêu cầu nhập tên người dùng và mật khẩu để xác thực",
                    trailing = {
                        RuntimeSwitch(
                            checked = settings.anonymousEnabled,
                            onCheckedChange = { arg0 -> viewModel?.toggleAnonymous(arg0) },
                            enabled = !busy,
                        )
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Storage,
                    title = "Chế độ bảo vệ chỉ đọc",
                    subtitle = if (settings.readOnly) "Chế độ chỉ đọc đang bật: cấm ghi, sửa, đổi tên hoặc xóa tệp từ bên ngoài" else "Chế độ đọc/ghi: cho phép tải lên, đổi tên và xóa tệp tự do từ bên ngoài",
                    trailing = {
                        RuntimeSwitch(
                            checked = settings.readOnly,
                            onCheckedChange = { arg0 -> viewModel?.toggleReadOnly(arg0) },
                            enabled = !busy,
                        )
                    },
                )
            }
        }

        item {
            SectionLabel("Cấu hình mạng")
            SettingsGroup {
                val portValue = portText.toIntOrNull()
                val portValid = portValue != null && portValue in 1024..65535
                SettingsPortEditor(
                    label = "Cổng FTP", value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    isError = portText.isNotBlank() && !portValid,
                    supportingText = if (portText.isNotBlank() && !portValid) "Cổng phải trong khoảng 1024–65535" else "Cổng mặc định: 2121 (khoảng 1024–65535)",
                ) {
                    RuntimeOutlinedButton(
                        onClick = { copyText(context, settings.port.toString(), "Đã sao chép cổng FTP (${settings.port})") },
                    ) {
                        RuntimeIcon(RuntimeIconName.Copy, Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Sao chép")
                    }
                    RuntimeOutlinedButton(
                        onClick = { viewModel?.savePort(portText) },
                        enabled = !busy && portValid && portText != settings.port.toString(),
                    ) {
                        Text("Lưu")
                    }
                }
            }
        }

        if (logs.isNotEmpty()) {
            item {
                SectionLabel("Nhật ký truy cập gần đây")
                SettingsDetailCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                ) {
                    Text(
                        logs.takeLast(12).joinToString("\n"),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

private fun copyText(context: Context, text: String, toast: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("FTP URL", text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

@Composable
private fun FtpPasswordDialog(
    passwordConfigured: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var password by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var confirmation by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val matches = password == confirmation
    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (passwordConfigured) "Đổi mật khẩu đăng nhập FTP" else "Đặt mật khẩu đăng nhập FTP") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Mật khẩu sẽ được lưu mã hóa qua Android Keystore, client bên ngoài cần nhập mật khẩu này khi kết nối.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RuntimeFilledTextField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    label = { Text("Mật khẩu mới") },
                    supportingText = { Text("Ít nhất 4 ký tự") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                RuntimeFilledTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it.take(128) },
                    label = { Text("Nhập lại mật khẩu") },
                    isError = confirmation.isNotEmpty() && !matches,
                    supportingText = {
                        if (confirmation.isNotEmpty() && !matches) Text("Hai lần nhập mật khẩu không khớp")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            RuntimeButton(
                onClick = { onSave(password) },
                enabled = password.length >= 4 && matches,
            ) { Text("Lưu") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (passwordConfigured) {
                    RuntimeTextButton(onClick = onClear) { Text("Xóa mật khẩu") }
                }
                RuntimeTextButton(onClick = onDismiss) { Text("Hủy") }
            }
        },
    )
}

@Composable
private fun FtpUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var username by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(currentUsername) }
    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi tên đăng nhập FTP") },
        text = { SettingsDialogForm {
            RuntimeFilledTextField(
                value = username,
                onValueChange = { username = it.take(32) },
                label = { Text("Tên người dùng") },
                supportingText = { Text("Mặc định là root") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } },
        confirmButton = {
            RuntimeButton(
                onClick = { onSave(username.trim().ifBlank { "root" }) },
                enabled = username.isNotBlank(),
            ) { Text("Lưu") }
        },
        dismissButton = {
            RuntimeTextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

@Composable
private fun ParamRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        RuntimeOutlinedButton(
            onClick = onCopy,
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            RuntimeIcon(RuntimeIconName.Copy, Modifier.size(13.dp))
            Spacer(Modifier.size(4.dp))
            Text("Sao chép", style = MaterialTheme.typography.labelSmall)
        }
    }
}
