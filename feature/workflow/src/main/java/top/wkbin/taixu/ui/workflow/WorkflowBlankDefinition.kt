package top.wkbin.taixu.ui.workflow

import java.util.UUID
import top.wkbin.taixu.core.model.workflow.*

internal fun blankWorkflowDefinition() = WorkflowDefinition(
    id = "workflow_${UUID.randomUUID().toString().take(8)}", name = "Workflow mới",
    description = "Thêm và nối các node trên canvas", category = "Tùy chỉnh",
    nodes = listOf(
        WorkflowNode("start", WorkflowNodeType.TRIGGER, "Kích hoạt thủ công", canvasX = 80f, canvasY = 160f),
        WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Hoàn tất", canvasX = 420f, canvasY = 160f),
    ), edges = listOf(WorkflowEdge("edge_start_done", "start", "output", "done")),
)
