package top.wkbin.taixu.core.model.workflow

/**
 * 晨报哨兵：每天定时对当前工作区做只读巡检（Git 状态 / 最近提交 / 未提交改动），
 * AI 汇总成一份晨间简报后投递系统通知。
 *
 * 由首页「晨报哨兵」卡片创建 DAILY 定时计划驱动；也可通过 /wf morning_report_sentinel 手动触发。
 */
object MorningReportSentinel {
    const val WORKFLOW_ID = "morning_report_sentinel"

    val definition: WorkflowDefinition = WorkflowDefinition(
        id = WORKFLOW_ID,
        name = "Lính gác bản tin sáng",
        description = "Tuần tra chỉ-đọc không gian làm việc hiện tại: thay đổi Git chưa commit, commit gần đây và thống kê sửa đổi, AI tổng hợp thành bản tin buổi sáng và đẩy thông báo hệ thống.",
        category = "Tuần tra",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf $WORKFLOW_ID"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu", canvasX = 40f, canvasY = 120f),
            WorkflowNode(
                "git_survey",
                WorkflowNodeType.BASH_COMMAND,
                "Kiểm tra Git",
                config = mapOf(
                    "command" to
                        "git rev-parse --is-inside-work-tree 2>/dev/null && " +
                        "{ echo '=== Thay đổi chưa commit ==='; git status --short; echo '=== Commit gần đây ==='; " +
                        "git log --oneline -8; echo '=== Thống kê thay đổi ==='; git diff --stat; } || " +
                        "echo 'Workspace hiện tại không phải repo git, bỏ qua kiểm tra Git lần này'",
                ),
                timeoutSeconds = 120,
                canvasX = 280f,
                canvasY = 120f,
            ),
            WorkflowNode(
                "summarize",
                WorkflowNodeType.AGENT_INFERENCE,
                "AI tổng hợp bản tin sáng",
                config = mapOf(
                    "prompt" to
                        "你是晨报哨兵。以下是对工作区（路径：\${'$'}{WORKSPACE_PATH}）的只读巡检输出。" +
                        "请汇总成一份简短的中文晨报，包含：1) 未提交变更概览（按模块/主题归类）；" +
                        "2) 最近提交进展；3) 今日建议关注事项（如无则省略）。" +
                        "严格只读分析：不得修改、暂存、提交或推送任何文件，不得执行任何写操作。\n" +
                        "巡检输出：\n\${'$'}{git_survey.output}",
                ),
                timeoutSeconds = 600,
                canvasX = 520f,
                canvasY = 120f,
            ),
            WorkflowNode(
                "notify",
                WorkflowNodeType.HOST_ACTION,
                "Đẩy thông báo bản tin sáng",
                config = mapOf(
                    "action" to "notification",
                    "title" to "Lính gác bản tin sáng",
                    "text" to "\${'$'}{summarize.output}",
                ),
                failurePolicy = FailurePolicy.CONTINUE,
                canvasX = 760f,
                canvasY = 120f,
            ),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Xong bản tin sáng", canvasX = 1000f, canvasY = 120f),
        ),
        edges = chain("start", "git_survey", "summarize", "notify", "done"),
    )

    private fun chain(vararg ids: String): List<WorkflowEdge> = ids.toList().zipWithNext().mapIndexed { index, pair ->
        WorkflowEdge("edge_$index", pair.first, "success", pair.second)
    }
}
