package top.wkbin.taixu.ui.workflow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import top.wkbin.taixu.core.model.workflow.WorkflowDefinition
import top.wkbin.taixu.core.model.workflow.WorkflowDefinitionImport
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeButton
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton

@Composable
internal fun WorkflowCatalogEntry(onCreate: () -> Unit, onImport: (WorkflowDefinition) -> Unit) {
    var importing by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Sắp xếp các thao tác build, chẩn đoán, dịch ngược và Git thành DAG có thể quan sát và tạm dừng.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RuntimeButton(onClick = onCreate, modifier = Modifier.weight(1f)) { Text("Tạo mới", maxLines = 1) }
            RuntimeOutlinedButton(onClick = { importing = true }, modifier = Modifier.weight(1f)) { Text("Nhập JSON", maxLines = 1) }
        }
    }
    if (importing) WorkflowImportDialog(onDismiss = { importing = false }, onImport = { definition ->
        importing = false
        onImport(definition)
    })
}

@Composable
private fun WorkflowImportDialog(onDismiss: () -> Unit, onImport: (WorkflowDefinition) -> Unit) {
    var raw by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<WorkflowDefinition?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) reading = false
        else scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val coroutine = currentCoroutineContext()
                    coroutine.ensureActive()
                    val stream = resolver.openInputStream(uri) ?: error("无法打开所选 JSON 文件")
                    WorkflowDocumentTransfer.read(stream) { coroutine.ensureActive() }
                }
                require(text.isNotBlank()) { "JSON 文件为空" }
                if (raw != text) {
                    preview = null
                    error = null
                    raw = text
                    checking = true
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { fileError = error.message ?: "读取 JSON 文件失败" }
            finally { reading = false }
        }
    }
    LaunchedEffect(raw) {
        val input = raw
        preview = null
        error = null
        if (input.isBlank()) { checking = false; return@LaunchedEffect }
        checking = true
        delay(250)
        val result = withContext(Dispatchers.Default) { runCatching { WorkflowDefinitionImport.parse(input) } }
        if (raw != input) return@LaunchedEffect
        preview = result.getOrNull()
        error = result.exceptionOrNull()?.message
        checking = false
    }
    RuntimeAlertDialog(onDismissRequest = onDismiss, title = { Text("Nhập JSON workflow") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Chọn tệp JSON UTF-8 hoặc dán định nghĩa đầy đủ; sau khi kiểm tra sẽ đưa vào trình chỉnh sửa dưới dạng workflow mới, rồi lưu và chạy. Việc nhập được kích hoạt thủ công; ID nút và tham chiếu biến giữ nguyên.")
            RuntimeOutlinedButton(enabled = !reading, modifier = Modifier.fillMaxWidth(), onClick = {
                if (reading) return@RuntimeOutlinedButton
                reading = true
                fileError = null
                try { picker.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }
                catch (_: Exception) { reading = false; fileError = "无法打开系统文件选择窗口" }
            }) { Text(if (reading) "Đang đọc…" else "Chọn tệp JSON", maxLines = 1) }
            OutlinedTextField(value = raw, onValueChange = { if (!reading) { raw = it; preview = null; error = null; fileError = null; checking = it.isNotBlank() } },
                enabled = !reading, label = { Text("JSON workflow") }, minLines = 4, maxLines = 8, isError = error != null || fileError != null,
                modifier = Modifier.fillMaxWidth())
            if (checking) Text("Đang kiểm tra…", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            fileError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            preview?.let { Text("${it.name}\n${it.nodes.size} nút · ${it.edges.size} kết nối · Thực thi đồng thời tối đa ${it.maxParallelNodes}",
                style = MaterialTheme.typography.bodyMedium) }
        }
    }, dismissButton = { RuntimeOutlinedButton(onClick = onDismiss) { Text("Hủy", maxLines = 1) } },
        confirmButton = { RuntimeButton(enabled = preview != null && !checking && !reading && fileError == null,
            onClick = { if (!checking && !reading && fileError == null) preview?.let(onImport) }) { Text("Nhập và chỉnh sửa", maxLines = 1) } })
}
