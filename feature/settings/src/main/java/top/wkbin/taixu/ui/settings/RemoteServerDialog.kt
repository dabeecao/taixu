package top.wkbin.taixu.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.wkbin.taixu.core.model.*
import top.wkbin.taixu.ui.components.*
import java.util.UUID

@Composable
internal fun RemoteServerDialog(viewModel: RemoteServerViewModel, existing: RemoteServer? = null, onDismiss: () -> Unit) {
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var host by remember { mutableStateOf(existing?.host.orEmpty()) }
    var port by remember { mutableStateOf((existing?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(existing?.username ?: "root") }
    var workspace by remember { mutableStateOf(existing?.workspace ?: "/root") }
    var keyMode by remember { mutableStateOf(existing?.authentication == SshAuthentication.PRIVATE_KEY) }
    var secret by remember { mutableStateOf("") }
    var phrase by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var showSecret by remember { mutableStateOf(false) }
    val serverId = remember { existing?.id ?: UUID.randomUUID().toString() }
    fun server() = RemoteServer(serverId, name.trim(), host.trim(), port.toIntOrNull() ?: 0,
        username.trim(), if (keyMode) SshAuthentication.PRIVATE_KEY else SshAuthentication.PASSWORD, workspace.trim(),
        existing?.takeIf { it.host == host.trim() && it.port == port.toIntOrNull() }?.fingerprint.orEmpty())
    val valid = name.isNotBlank() && host.isNotBlank() && (port.toIntOrNull() ?: 0) in 1..65535 &&
        username.isNotBlank() && workspace.startsWith('/') && secret.isNotBlank()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(existing?.id) {
        if (existing != null) {
            try { val credentials = viewModel.credentials(existing.id); secret = credentials.secret; phrase = credentials.passphrase }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { importError = "读取凭证失败，请重新填写认证信息" }
        }
    }
    val importKey = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (true) {
                            val count = it.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 65536)
                            output.write(buffer, 0, count)
                        }
                        String(output.toByteArray(), Charsets.UTF_8)
                    } ?: error("文件不可读")
                }
                secret = content
                importError = null
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { importError = "私钥读取失败（最大 64 KiB）" }
        }
    }
    LaunchedEffect(state.saved) { if (state.saved) { secret = ""; phrase = ""; onDismiss(); viewModel.clear() } }
    if (state.fingerprint != null) {
        RuntimeAlertDialog(
            onDismissRequest = { viewModel.clear() },
            title = { Text("Xác nhận danh tính máy chủ") },
            text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Vui lòng đối chiếu vân tay máy chủ SSH do quản trị viên cung cấp. Chỉ gửi mật khẩu hoặc dùng xác thực khóa riêng sau khi xác nhận.")
                Spacer(Modifier.height(12.dp))
                Text(state.fingerprint.orEmpty(), style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { RuntimeTextButton(onClick = viewModel::confirmFingerprint) { Text("Vân tay khớp, kết nối") } },
            dismissButton = { RuntimeTextButton(onClick = viewModel::clear) { Text("Hủy") } },
        )
        return
    }
    RuntimeAlertDialog(
        onDismissRequest = { if (!state.busy) { viewModel.clear(); onDismiss() } },
        title = { Text(if (existing == null) "Thêm kết nối" else "Sửa kết nối") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sau khi lưu hồ sơ kết nối SSH, có thể chọn làm hệ thống chính. Mỗi hồ sơ lưu thông tin xác thực riêng.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tên") }, singleLine = true, enabled = !state.busy)
                OutlinedTextField(host, { host = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Địa chỉ máy chủ") }, singleLine = true, enabled = !state.busy)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(port, { port = it }, modifier = Modifier.weight(1f), label = { Text("Cổng") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, enabled = !state.busy)
                    OutlinedTextField(username, { username = it }, modifier = Modifier.weight(1f), label = { Text("Tên người dùng") }, singleLine = true, enabled = !state.busy)
                }
                OutlinedTextField(workspace, { workspace = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Thư mục làm việc") }, supportingText = { Text("Thư mục đã tồn tại trên máy chủ, cần cho phép người dùng hiện tại truy cập") }, singleLine = true, enabled = !state.busy)
                HorizontalDivider()
                Text("Cách xác thực", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuntimeOutlinedButton(modifier = Modifier.weight(1f), onClick = { if (keyMode) { keyMode = false; secret = "" } }, enabled = !state.busy) { Text(if (!keyMode) "✓ Mật khẩu" else "Mật khẩu") }
                    RuntimeOutlinedButton(modifier = Modifier.weight(1f), onClick = { if (!keyMode) { keyMode = true; secret = "" } }, enabled = !state.busy) { Text(if (keyMode) "✓ Khóa SSH" else "Khóa SSH") }
                }
                OutlinedTextField(secret, { secret = it }, modifier = Modifier.fillMaxWidth(), label = { Text(if (keyMode) "Khóa riêng PEM / OpenSSH" else "Mật khẩu") },
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(), singleLine = !keyMode, maxLines = 5, enabled = !state.busy,
                    trailingIcon = { RuntimeTextButton(onClick = { showSecret = !showSecret }) { Text(if (showSecret) "Ẩn" else "Hiện") } })
                if (keyMode) {
                    RuntimeTextButton(onClick = { importKey.launch(arrayOf("*/*")) }, enabled = !state.busy) { Text("Nhập khóa riêng từ tệp") }
                    OutlinedTextField(phrase, { phrase = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Mật khẩu khóa riêng (tùy chọn)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), enabled = !state.busy)
                }
                HorizontalDivider()
                Text("Kiểm tra kết nối", style = MaterialTheme.typography.labelLarge)
                Text("Kiểm tra không lưu cấu hình. Lần kết nối đầu cần đối chiếu vân tay máy chủ, máy chủ cần bật SSH / SFTP.", style = MaterialTheme.typography.bodySmall)
                RuntimeOutlinedButton(enabled = valid && !state.busy, onClick = { viewModel.test(server(), SshCredentials(secret, phrase)) }) { Text("Kiểm tra kết nối") }
                (state.message ?: importError)?.let { Text(it, color = if (state.tested) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                if (state.busy) RuntimeLinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { RuntimeTextButton(enabled = valid && !state.busy, onClick = {
            viewModel.connect(server(), SshCredentials(secret, phrase))
        }) { Text(if (state.busy) "Đang xác minh…" else "Lưu") } },
        dismissButton = { RuntimeTextButton(enabled = !state.busy, onClick = { viewModel.clear(); onDismiss() }) { Text("Hủy") } },
    )
}
