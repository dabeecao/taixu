package top.wkbin.taixu.ui.workflow

import android.content.ClipData
import android.os.PersistableBundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.wkbin.taixu.core.model.workflow.WorkflowDefinition
import top.wkbin.taixu.core.model.workflow.WorkflowDefinitionExport
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeButton
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton

@Composable
internal fun WorkflowExportButton(definition: WorkflowDefinition, isDirty: Boolean) {
    var snapshot by remember { mutableStateOf<Pair<WorkflowDefinition, Boolean>?>(null) }
    RuntimeOutlinedButton(onClick = { snapshot = definition to isDirty }, modifier = Modifier.fillMaxWidth()) { Text("Xuất JSON", maxLines = 1) }
    snapshot?.let { (source, dirty) -> WorkflowExportDialog(source, dirty, onDismiss = { snapshot = null }) }
}

@Composable
private fun WorkflowExportDialog(definition: WorkflowDefinition, isDirty: Boolean, onDismiss: () -> Unit) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var raw by remember(definition) { mutableStateOf<String?>(null) }
    var error by remember(definition) { mutableStateOf<String?>(null) }
    var copying by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(definition) {
        val result = withContext(Dispatchers.Default) { runCatching { WorkflowDefinitionExport.encode(definition) } }
        raw = result.getOrNull()
        error = result.exceptionOrNull()?.message
    }
    RuntimeAlertDialog(onDismissRequest = onDismiss, title = { Text("Xuất workflow JSON") }, text = {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (isDirty) "Xuất bản nháp chỉnh sửa đã áp dụng, gồm thay đổi chưa lưu." else "Xuất định nghĩa workflow trong trình chỉnh sửa hiện tại.")
            Text("JSON chứa cấu hình nút và biến mặc định, kiểm tra nội dung ở đây trước khi sao chép; nhập lại sẽ tạo workflow thủ công mới.", style = MaterialTheme.typography.bodySmall)
            raw?.let { text ->
                Text("${definition.nodes.size} nút · ${definition.edges.size} kết nối · ${text.length} ký tự", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(value = text.take(24_000), onValueChange = {}, readOnly = true,
                    label = { Text("Xem trước JSON") }, minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth())
                if (text.length > 24_000) Text("Xem trước 24000 ký tự đầu, “Sao chép toàn bộ JSON” sẽ sao chép toàn bộ nội dung.", style = MaterialTheme.typography.bodySmall)
            }
            if (raw == null && error == null) Text("Đang tạo…", style = MaterialTheme.typography.bodySmall)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            if (copied) Text("Đã sao chép toàn bộ JSON", style = MaterialTheme.typography.bodySmall)
            WorkflowSaveDocumentButton(raw, definition.name)
        }
    }, dismissButton = { RuntimeOutlinedButton(onClick = onDismiss) { Text("Đóng", maxLines = 1) } }, confirmButton = {
        RuntimeButton(enabled = raw != null && !copying, onClick = {
            val text = raw ?: return@RuntimeButton
            copying = true
            scope.launch {
                try {
                    val clip = ClipData.newPlainText("workflow_definition", text).apply {
                        description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
                    }
                    clipboard.setClipEntry(ClipEntry(clip))
                    copied = true
                    error = null
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Sao chép thất bại, thử lại" }
                finally { copying = false }
            }
        }) { Text(if (copying) "Đang sao chép" else "Sao chép JSON đầy đủ", maxLines = 1) }
    })
}
