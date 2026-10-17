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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
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
import top.wkbin.taixu.runtime.SshServiceState
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
fun SshSettingsScreen(
    onBack: () -> Unit,
    viewModel: SshSettingsViewModel? = previewOrNull { koinViewModel() },
) {
    val settings by viewModel?.settings?.collectAsStateWithLifecycle() ?: previewStateOf(SshSettingsUiState())
    val serviceState by viewModel?.serviceState?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.runtime.SshServiceState.Stopped("ubuntu"))
    val operating by viewModel?.operating?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val message by viewModel?.message?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val messageIsError by viewModel?.messageIsError?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val logs by viewModel?.logs?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val vpnActive by viewModel?.vpnActive?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val context = LocalContext.current

    var portText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(settings.port.toString()) }
    var authorizedKeysText by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(settings.authorizedKeys) }
    var showPasswordDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.distroId, settings.port) { portText = settings.port.toString() }
    LaunchedEffect(settings.distroId, settings.authorizedKeys) { authorizedKeysText = settings.authorizedKeys }

    message?.let { current ->
        RuntimeAlertDialog(
            onDismissRequest = { viewModel?.consumeMessage() },
            title = { Text(if (messageIsError) "Thao tác SSH chưa hoàn tất" else "Truy cập từ xa SSH") },
            text = { Text(current) },
            confirmButton = { RuntimeTextButton(onClick = { viewModel?.consumeMessage() }) { Text("Đã hiểu") } },
        )
    }

    if (showPasswordDialog) {
        PasswordSettingsDialog(
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

    val statusText = when (serviceState) {
        is SshServiceState.Stopped -> "已停止"
        is SshServiceState.Installing -> "正在安装 OpenSSH"
        is SshServiceState.Starting -> "正在启动"
        is SshServiceState.Running -> "运行中"
        is SshServiceState.Failed -> "启动失败"
    }
    val busy = operating || serviceState is SshServiceState.Installing || serviceState is SshServiceState.Starting

    RuntimeSettingsScaffold(
        title = "Truy cập từ xa SSH", onBack = onBack,
        statusText = statusText,
    ) {
        item {
            NoticeBanner(
                text = "SSH 按当前 Linux 发行版独立配置。首次启用会按需安装 OpenSSH；支持传统 root + 密码登录，也可使用公钥。",
            )
        }

        item {
            SectionLabel("服务状态")
            SettingsGroup {
                SettingsRow(
                    icon = RuntimeIconName.Server,
                    title = "Bật dịch vụ SSH",
                    subtitle = "Giữ bật và tự khởi động cùng Linux runtime",
                    value = if (busy) {
                        when (serviceState) {
                            is SshServiceState.Installing -> "正在安装 OpenSSH"
                            is SshServiceState.Starting -> "正在等待 SSH 服务就绪"
                            else -> "正在处理"
                        }
                    } else {
                        when (serviceState) {
                            is SshServiceState.Running -> "运行中"
                            is SshServiceState.Installing -> "安装中"
                            is SshServiceState.Starting -> "启动中"
                            is SshServiceState.Failed -> "失败"
                            is SshServiceState.Stopped -> if ((serviceState as SshServiceState.Stopped).installed) "已安装" else "未安装"
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
            (serviceState as? SshServiceState.Failed)?.let { failed ->
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
            SectionLabel("登录认证")
            SettingsGroup {
                SettingsRow(
                    icon = RuntimeIconName.Admin,
                    title = "Tên đăng nhập",
                    subtitle = "Tài khoản quản trị sandbox PRoot Linux",
                    value = "root",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Key,
                    title = "Mật khẩu đăng nhập",
                    subtitle = if (settings.passwordConfigured) "Mật khẩu đã được mã hóa và lưu, bấm để sửa hoặc xóa" else "Chưa thiết lập, phải cấu hình trước khi đăng nhập bằng mật khẩu",
                    value = if (settings.passwordConfigured) "Đã thiết lập" else "Chưa thiết lập",
                    onClick = { showPasswordDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Shield,
                    title = "Cho phép đăng nhập bằng mật khẩu",
                    subtitle = if (settings.passwordAuthEnabled) "Nhập mật khẩu tương ứng của root khi kết nối" else "Xác thực mật khẩu đã tắt",
                    trailing = {
                        RuntimeSwitch(
                            checked = settings.passwordAuthEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && !settings.passwordConfigured) {
                                    showPasswordDialog = true
                                } else {
                                    viewModel?.setPasswordAuthEnabled(enabled)
                                }
                            },
                            enabled = !busy,
                        )
                    },
                )
            }
        }

        item {
            SectionLabel("连接")
            SettingsDetailCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = settings.connectionCommand,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Lần đầu kết nối nhập yes để xác nhận fingerprint máy chủ, sau đó nhập mật khẩu đăng nhập root",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RuntimeOutlinedButton(
                        onClick = { copyText(context, settings.connectionCommand, "Đã sao chép lệnh SSH") },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        RuntimeIcon(RuntimeIconName.Copy, Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Sao chép lệnh")
                    }
                }
            }
        }

        if (vpnActive) {
            item {
                NoticeBanner(
                    text = "检测到手机正处于 VPN 连接中，当前 VPN 会接管本应用流量，同一局域网的电脑连接可能超时。请先关闭 VPN，或将本应用加入 VPN 直连/排除名单。",
                    isError = true,
                )
            }
        }

        item {
            SectionLabel("网络配置")
            SettingsGroup {
                val portValue = portText.toIntOrNull()
                val portValid = portValue != null && portValue in 1024..65535
                SettingsPortEditor(
                    label = "Cổng SSH", value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    isError = portText.isNotBlank() && !portValid,
                    supportingText = if (portText.isNotBlank() && !portValid) "端口需在 1024–65535 范围内" else "允许范围：1024–65535",
                ) {
                    RuntimeOutlinedButton(onClick = { viewModel?.savePort(portText) }, enabled = !busy && portValid) { Text("Lưu") }
                }
            }
        }

        item {
            SectionLabel("公钥登录（可选）")
            SettingsDetailCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "无需公钥也可以使用密码登录。需要免密连接时，每行添加一个 OpenSSH 公钥。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RuntimeFilledTextField(
                        value = authorizedKeysText,
                        onValueChange = { authorizedKeysText = it },
                        label = { Text("authorized_keys") },
                        placeholder = { Text("ssh-ed25519 AAAAC3... user@device") },
                        minLines = 4,
                        maxLines = 8,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    RuntimeButton(
                        onClick = { viewModel?.saveAuthorizedKeys(authorizedKeysText) },
                        enabled = !busy,
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        RuntimeIcon(RuntimeIconName.Save, Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Lưu khóa công khai")
                    }
                }
            }
        }

        if (logs.isNotEmpty()) {
            item {
                SectionLabel("最近日志")
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
    clipboard?.setPrimaryClip(ClipData.newPlainText("SSH command", text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

@Composable
private fun PasswordSettingsDialog(
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
        title = { Text(if (passwordConfigured) "Đổi mật khẩu đăng nhập SSH" else "Đặt mật khẩu đăng nhập SSH") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Mật khẩu sẽ được mã hóa bằng Android Keystore rồi áp dụng cho tài khoản root của bản Linux hiện tại.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RuntimeFilledTextField(
                    value = password,
                    onValueChange = { password = it.take(128) },
                    label = { Text("Mật khẩu mới") },
                    supportingText = { Text("Ít nhất 8 ký tự, không chứa dấu hai chấm") },
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
                enabled = password.length >= 8 && matches && ':' !in password,
            ) { Text("Lưu và bật") }
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
