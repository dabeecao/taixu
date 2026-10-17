package top.wkbin.taixu.ui.workflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.wkbin.taixu.core.model.workflow.*
import top.wkbin.taixu.ui.components.*

@Composable
internal fun WorkflowHistoryCard(
    entry: WorkflowHistoryEntry,
    onRerun: (WorkflowHistoryEntry) -> Unit,
    onHistory: (WorkflowRuntimeState) -> Unit,
    onResume: (WorkflowRuntimeState, Set<String>) -> Unit,
) {
    val run = entry.state
    val plan = remember(run) { WorkflowRecovery.inspect(run) }
    var showResume by remember { mutableStateOf(false) }
    RuntimeCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${run.definition.name} · ${workflowStatusLabel(run.status)} · ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(run.startedAt ?: 0))}",
                style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (entry.triggerSource == "SCHEDULE") "Kích hoạt bởi lịch định giờ" else "Kích hoạt thủ công",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!plan.canResume && run.status in setOf(WorkflowRunStatus.FAILED, WorkflowRunStatus.CANCELLED)) {
                Text(plan.blockedReason.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plan.canResume) RuntimeOutlinedButton(onClick = { showResume = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("Tiếp tục", maxLines = 1)
                }
                RuntimeOutlinedButton(onClick = { onRerun(entry) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text("Chạy lại", maxLines = 1) }
                RuntimeOutlinedButton(onClick = { onHistory(run) }, contentPadding = PaddingValues(9.dp)) { RuntimeIcon(RuntimeIconName.List, Modifier.size(16.dp)) }
            }
        }
    }
    if (showResume) WorkflowResumeDialog(run, plan, onDismiss = { showResume = false }) { retry ->
        showResume = false
        onResume(run, retry)
    }
}

@Composable
private fun WorkflowResumeDialog(
    run: WorkflowRuntimeState,
    plan: WorkflowRecoveryPlan,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var retries by remember(run) { mutableStateOf(emptySet<String>()) }
    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tiếp tục ${run.definition.name}") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tái dùng ${plan.cachedOutputs.size} kết quả đã lưu, chạy ${plan.pendingNodeIds.size} nút còn lại.")
                Text("Hãy đối chiếu file không gian làm việc, trạng thái ứng dụng và sản phẩm đã tạo trước. Tiếp tục sẽ dùng lại kế hoạch gốc và các phê duyệt đã xong; phê duyệt đang chờ sẽ được yêu cầu lại.")
                Text("Không gian làm việc: ${run.context?.workspacePath.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                run.definition.nodes.filter { it.id in plan.pendingNodeIds }.forEach { node ->
                    if (node.id in plan.retryNodeIds) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${node.title}: thất bại hoặc kết quả chưa chắc chắn. Hãy bật sau khi xác nhận có thể thử lại an toàn.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            RuntimeSwitch(checked = node.id in retries, onCheckedChange = { checked ->
                                retries = if (checked) retries + node.id else retries - node.id
                            })
                        }
                    } else Text("Chờ thực thi: ${node.title}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = { RuntimeOutlinedButton(onClick = onDismiss) { Text("Hủy") } },
        confirmButton = { RuntimeButton(onClick = { onConfirm(retries) }, enabled = retries == plan.retryNodeIds) { Text("Tiếp tục thực thi") } },
    )
}
