package top.wkbin.taixu.ui.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.wkbin.taixu.ui.components.*

@Composable
internal fun TerminalRestoreWarnings(viewModel: TerminalViewModel?) {
    if (viewModel == null) return
    val failures by viewModel.restoreFailures.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    if (failures.isEmpty()) return
    RuntimeTextButton(onClick = { expanded = true }) { Text("${failures.size} Terminal chưa khôi phục · Xem bản ghi") }
    if (expanded) RuntimeAlertDialog(
        onDismissRequest = { expanded = false },
        title = { Text("Terminal chưa khôi phục") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Đã giữ nguyên danh tính kết nối ban đầu. Các Terminal này sẽ không chuyển sang máy chủ khác; xóa bản ghi chỉ xóa cấu hình Terminal trên máy này.", style = MaterialTheme.typography.bodySmall)
                RuntimeTextButton(onClick = viewModel::retryFailedRestorations) { Text("Thử khôi phục lại") }
                failures.forEach { failure ->
                    Text(failure.label, style = MaterialTheme.typography.titleSmall)
                    Text(failure.message, style = MaterialTheme.typography.bodySmall)
                    RuntimeTextButton(onClick = { viewModel.removeFailedRestoration(failure.id) }) { Text("Xóa bản ghi") }
                }
            }
        },
        confirmButton = { RuntimeTextButton(onClick = { expanded = false }) { Text("Đóng") } },
    )
}
