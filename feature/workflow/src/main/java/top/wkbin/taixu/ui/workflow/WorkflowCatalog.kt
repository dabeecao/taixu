package top.wkbin.taixu.ui.workflow
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import top.wkbin.taixu.ui.components.RuntimeSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import top.wkbin.taixu.core.model.workflow.NodeRunStatus
import top.wkbin.taixu.core.database.WorkflowScheduleEntity
import top.wkbin.taixu.core.model.workflow.WorkflowDefinition
import top.wkbin.taixu.core.model.workflow.WorkflowRunStatus
import top.wkbin.taixu.core.model.workflow.WorkflowRuntimeState
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeButton
import top.wkbin.taixu.ui.components.RuntimeCard
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton
import top.wkbin.taixu.ui.components.RuntimeTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun WorkflowCatalog(
    definitions: List<WorkflowDefinition>,
    onRun: (WorkflowDefinition) -> Unit,
    onEdit: (WorkflowDefinition) -> Unit,
    onDelete: (WorkflowDefinition) -> Unit,
    onCreate: () -> Unit,
    onImport: (WorkflowDefinition) -> Unit,
    history: List<WorkflowHistoryEntry>,
    onHistory: (WorkflowRuntimeState) -> Unit,
    onRerun: (WorkflowHistoryEntry) -> Unit,
    onResume: (WorkflowRuntimeState, Set<String>) -> Unit,
    backgroundRuns: List<WorkflowRuntimeState>,
    onViewBackgroundRun: (String) -> Unit,
    schedules: List<WorkflowScheduleEntity> = emptyList(),
    onNewSchedule: () -> Unit = {},
    onEditSchedule: (WorkflowScheduleEntity) -> Unit = {},
    onToggleSchedule: (String, Boolean) -> Unit = { _, _ -> },
    onDeleteSchedule: (String) -> Unit = {},
    modifier: Modifier = Modifier, bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    var deleteRequested by remember { mutableStateOf<WorkflowDefinition?>(null) }
    LazyColumn(
        modifier = modifier,
        contentPadding = top.wkbin.taixu.ui.components.runtimeContentPadding(horizontal = 16.dp, top = 16.dp, bottom = 32.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { WorkflowCatalogEntry(onCreate, onImport) }
        if (backgroundRuns.isNotEmpty()) {
            item(key = "background_banner") {
                RuntimeCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RuntimeIcon(RuntimeIconName.Play, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (backgroundRuns.size == 1) "1 workflow đang chạy nền" else "${backgroundRuns.size} workflow đang chạy nền",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                backgroundRuns.joinToString("、") { it.definition.name },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        RuntimeOutlinedButton(
                            onClick = { onViewBackgroundRun(backgroundRuns.first().executionId) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("Xem", maxLines = 1)
                        }
                    }
                }
            }
        }
        items(definitions, key = { it.id }) { workflow ->
            RuntimeCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, containerColor = MaterialTheme.colorScheme.surfaceContainer, contentPadding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RuntimeIcon(RuntimeIconName.Hub, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(workflow.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${workflow.category} · ${workflow.nodes.size} nút", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(workflow.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!workflow.isBuiltin) {
                            RuntimeOutlinedButton(onClick = { deleteRequested = workflow }, contentPadding = PaddingValues(9.dp)) {
                                RuntimeIcon(RuntimeIconName.Trash, Modifier.size(16.dp))
                            }
                        }
                        RuntimeOutlinedButton(onClick = { onEdit(workflow) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                            RuntimeIcon(RuntimeIconName.Edit, Modifier.size(16.dp))
                            Text(if (workflow.isBuiltin) "Tạo bản sao" else "Chỉnh sửa", maxLines = 1)
                        }
                        RuntimeButton(onClick = { onRun(workflow) }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                            RuntimeIcon(RuntimeIconName.Play, Modifier.size(16.dp))
                            Text("Chạy", maxLines = 1)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Lịch định giờ",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                    RuntimeOutlinedButton(onClick = onNewSchedule, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        RuntimeIcon(RuntimeIconName.Plus, Modifier.size(16.dp))
                        Text("Tạo lịch mới", maxLines = 1)
                    }
                }
            }
            if (schedules.isEmpty()) {
                item {
                    Text(
                        "Chưa có lịch định giờ nào. Hãy đặt workflow chạy định giờ mỗi ngày, tuần tra định kỳ hoặc kích hoạt trễ một lần; đến giờ dù ứng dụng không chạy, hệ thống cũng sẽ đánh thức để thực thi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(schedules, key = { "schedule:${it.id}" }) { schedule ->
                val workflowName = definitions.firstOrNull { it.id == schedule.workflowId }?.name ?: schedule.workflowId
                RuntimeCard(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, containerColor = MaterialTheme.colorScheme.surfaceContainer, contentPadding = PaddingValues(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RuntimeIcon(RuntimeIconName.Refresh, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(schedule.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "$workflowName · ${scheduleRepeatLabel(schedule)}" +
                                        schedule.nextRunAt?.let { next ->
                                            " · Lần tới ${java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(next))}"
                                        }.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = schedule.enabled, onCheckedChange = { onToggleSchedule(schedule.id, it) })
                        }
                        schedule.lastExecutionId?.let {
                            Text(
                                "Lần kích hoạt trước: ${schedule.lastRunAt?.let { last -> java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(last)) } ?: "—"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RuntimeOutlinedButton(onClick = { onDeleteSchedule(schedule.id) }, contentPadding = PaddingValues(9.dp)) {
                                RuntimeIcon(RuntimeIconName.Trash, Modifier.size(16.dp))
                            }
                            RuntimeOutlinedButton(onClick = { onEditSchedule(schedule) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                                RuntimeIcon(RuntimeIconName.Edit, Modifier.size(16.dp))
                                Text("Chỉnh sửa", maxLines = 1)
                            }
                        }
                    }
                }
            }
        if (history.isNotEmpty()) item { Text("Lần chạy gần đây (${history.size} mục gần nhất)", style = MaterialTheme.typography.titleMedium) }
        items(history, key = { "history:${it.state.executionId}" }) { entry ->
            WorkflowHistoryCard(entry, onRerun, onHistory, onResume)
        }
    }
    deleteRequested?.let { workflow ->
        RuntimeAlertDialog(
            onDismissRequest = { deleteRequested = null },
            title = { Text("Xóa workflow?") },
            text = { Text("“${workflow.name}” và lịch sử chạy của nó sẽ bị xóa vĩnh viễn, không thể hoàn tác.") },
            dismissButton = { RuntimeOutlinedButton(onClick = { deleteRequested = null }) { Text("Hủy") } },
            confirmButton = {
                RuntimeButton(
                    onClick = {
                        deleteRequested = null
                        onDelete(workflow)
                    },
                ) { Text("Xóa") }
            },
        )
    }
}

