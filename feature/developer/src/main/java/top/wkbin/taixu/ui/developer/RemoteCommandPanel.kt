package top.wkbin.taixu.ui.developer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.wkbin.taixu.runtime.remote.RemoteSystemService
import top.wkbin.taixu.ui.components.*

@Composable
internal fun RemoteCommandPanel(serverId: String, service: RemoteSystemService) {
    var command by remember { mutableStateOf("cat /etc/os-release") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val target = runCatching { service.server(serverId) }.getOrNull()
    RuntimeCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Lệnh máy chủ", style = MaterialTheme.typography.titleMedium)
            Text("Lệnh chạy trong thư mục làm việc của máy chủ hiện tại, dùng đúng quyền của người dùng kết nối.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(command, { command = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                label = { Text("Lệnh") }, maxLines = 5, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
            RuntimeButton(enabled = !busy && command.isNotBlank(), onClick = {
                val snapshot = command
                scope.launch {
                    busy = true; output = ""
                    try { output = service.execute(serverId, snapshot, target) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { output = "Thực thi thất bại, hãy kiểm tra kết nối máy chủ và quyền" }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Đang thực thi…" else "Thực thi") }
            if (busy) RuntimeLinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            if (output.isNotBlank()) Text(output, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        }
    }
}
