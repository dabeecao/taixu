package top.wkbin.taixu.core.model.workflow

object BuiltinWorkflows {
    val all: List<WorkflowDefinition>
        get() = listOf(
            systemHealth,
            conditionBranchDemo,
            networkDiagnostic,
            releaseApk,
            installGeneratedApk,
            buildDoctor,
            buildRepair,
            reverseAudit,
            atomicCommit,
            hostAutomationLab,
            hostBroadcastDemo,
            hostAgentGuiPilot, VirtualScreenWorkflowActions.template(), MorningReportSentinel.definition,
        ).map(WorkflowLayout::arrange)

    fun find(id: String): WorkflowDefinition? = all.firstOrNull { it.id == id }

    private fun chain(vararg ids: String): List<WorkflowEdge> = ids.toList().zipWithNext().mapIndexed { index, pair ->
        WorkflowEdge("edge_$index", pair.first, "success", pair.second)
    }

    val systemHealth = WorkflowDefinition(
        id = "linux_system_health",
        name = "Kiểm tra môi trường & phần cứng sandbox Linux",
        description = "Kiểm tra một chạm không cần cấu hình: kiểm tra toàn diện kernel, kiến trúc, CPU, RAM, ổ đĩa của bản phân phối Linux trong sandbox cùng trạng thái toolchain thường dùng.",
        category = "Kiểm tra",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf linux_system_health"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu kiểm tra", canvasX = 40f, canvasY = 120f),
            WorkflowNode(
                "os_kernel",
                WorkflowNodeType.BASH_COMMAND,
                "Kiểm tra hệ thống & kernel",
                config = mapOf("command" to "echo '=== 1. 内核与架构 ===' && uname -a && echo '' && echo '=== 2. 发行版信息 ===' && (cat /etc/os-release | head -n 8 || cat /etc/issue || echo '未知发行版')"),
                canvasX = 280f,
                canvasY = 120f,
            ),
            WorkflowNode(
                "hardware",
                WorkflowNodeType.BASH_COMMAND,
                "Trạng thái RAM & lưu trữ",
                config = mapOf("command" to "echo '=== 1. 内存使用情况 ===' && (free -h || cat /proc/meminfo | head -n 4) && echo '' && echo '=== 2. 根目录磁盘空间 ===' && df -h /"),
                canvasX = 520f,
                canvasY = 120f,
            ),
            WorkflowNode(
                "toolchain",
                WorkflowNodeType.BASH_COMMAND,
                "Kiểm tra toolchain chính",
                config = mapOf("command" to "echo '=== 核心命令行工具排查 ===' && for cmd in bash sh git python3 python curl wget make gcc tar find grep awk sed; do which \$cmd 2>/dev/null && echo \"  ✔ \$cmd: 可用 (\$(which \$cmd))\" || echo \"  ✘ \$cmd: 未安装\"; done"),
                canvasX = 760f,
                canvasY = 120f,
            ),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Tạo báo cáo kiểm tra", canvasX = 1000f, canvasY = 120f),
        ),
        edges = chain("start", "os_kernel", "hardware", "toolchain", "done"),
    )

    val conditionBranchDemo = WorkflowDefinition(
        id = "condition_branch_demo",
        name = "Kiểm tra logic rẽ nhánh điều kiện (Demo)",
        description = "Kiểm tra môi trường cơ bản của sandbox, rẽ nhánh điều kiện tới 【nhánh A: môi trường bình thường】 hoặc 【nhánh B: môi trường bất thường】.",
        category = "Ví dụ",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf condition_branch_demo"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu kiểm tra", canvasX = 40f, canvasY = 160f),
            WorkflowNode(
                "check_env",
                WorkflowNodeType.BASH_COMMAND,
                "Kiểm tra môi trường sandbox cơ bản",
                config = mapOf("command" to "echo '=== 正在检测 Linux 沙箱根目录 ===' && test -d /etc && echo '✔ /etc 目录存在'"),
                canvasX = 280f,
                canvasY = 160f,
            ),
            WorkflowNode(
                "branch",
                WorkflowNodeType.CONDITION_BRANCH,
                "Rẽ nhánh điều kiện",
                description = "Rẽ luồng xử lý sang các nhánh khác nhau dựa trên kết quả kiểm tra từ node trước",
                config = mapOf("expression" to "exitCode == 0"),
                failurePolicy = FailurePolicy.CONTINUE,
                canvasX = 520f,
                canvasY = 160f,
            ),
            WorkflowNode(
                "branch_success",
                WorkflowNodeType.BASH_COMMAND,
                "【分支 A】 Môi trường ổn",
                config = mapOf("command" to "echo '==============================' && echo '🎉【分支 A 触发成功】' && echo '沙箱基础配置完全正常，继续执行业务操作。' && echo '=============================='"),
                canvasX = 780f,
                canvasY = 80f,
            ),
            WorkflowNode(
                "branch_failure",
                WorkflowNodeType.BASH_COMMAND,
                "【分支 B】 Cảnh báo môi trường bất thường",
                config = mapOf("command" to "echo '==============================' && echo '⚠️【分支 B 触发成功】' && echo '检测到沙箱缺少必要目录，进入降级与告警流程。' && echo '=============================='"),
                canvasX = 780f,
                canvasY = 240f,
            ),
            WorkflowNode(
                "done",
                WorkflowNodeType.TERMINAL_OUTPUT,
                "Hoàn tất tổng hợp kiểm tra",
                canvasX = 1040f,
                canvasY = 160f,
            ),
        ),
        edges = listOf(
            WorkflowEdge("edge_demo_1", "start", "output", "check_env"),
            WorkflowEdge("edge_demo_2", "check_env", "output", "branch"),
            WorkflowEdge("edge_demo_3", "branch", "output", "branch_success", conditionExpression = "exitCode == 0"),
            WorkflowEdge("edge_demo_4", "branch", "output", "branch_failure", conditionExpression = "exitCode != 0"),
            WorkflowEdge("edge_demo_5", "branch_success", "output", "done"),
            WorkflowEdge("edge_demo_6", "branch_failure", "output", "done"),
        ),
    )

    val networkDiagnostic = WorkflowDefinition(
        id = "network_dns_diagnostic",
        name = "Chẩn đoán kết nối mạng & DNS",
        description = "Kiểm tra mạng một chạm không cần cấu hình: kiểm tra tệp cấu hình DNS trong container, khả năng phân giải tên miền và kết nối dịch vụ HTTP công cộng.",
        category = "Mạng",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf network_dns_diagnostic"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu chẩn đoán", canvasX = 40f, canvasY = 120f),
            WorkflowNode(
                "dns_conf",
                WorkflowNodeType.BASH_COMMAND,
                "Xem cấu hình DNS",
                config = mapOf("command" to "echo '=== /etc/resolv.conf ===' && (cat /etc/resolv.conf || echo '无 resolv.conf') && echo '' && echo '=== /etc/hosts ===' && head -n 10 /etc/hosts"),
                canvasX = 280f,
                canvasY = 120f,
            ),
            WorkflowNode(
                "http_ping",
                WorkflowNodeType.BASH_COMMAND,
                "Kiểm tra request HTTP công cộng",
                config = mapOf("command" to "echo '=== 测试公网连通性 (HTTP HEAD) ===' && (curl -I -s -m 5 https://www.baidu.com | head -n 4 || curl -I -s -m 5 https://www.qq.com | head -n 4 || curl -I -s -m 5 https://www.bilibili.com | head -n 4 || echo 'curl 测试Hoàn tất')"),
                canvasX = 520f,
                canvasY = 120f,
            ),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Hoàn tất chẩn đoán mạng", canvasX = 760f, canvasY = 120f),
        ),
        edges = chain("start", "dns_conf", "http_ping", "done"),
    )

    val releaseApk = WorkflowDefinition(
        id = "release_apk_direct_install",
        name = "Đóng gói & cài đặt APK",
        description = "Kiểm tra kho mã, build APK Release ARM64, sau khi xác nhận sẽ giao cho host cài đặt.",
        category = "Android",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf release_apk_direct_install"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu", canvasX = 40f, canvasY = 80f),
            WorkflowNode("git", WorkflowNodeType.BASH_COMMAND, "Kiểm tra trạng thái Git", config = mapOf("command" to "git status --short"), canvasX = 260f, canvasY = 80f),
            WorkflowNode("build", WorkflowNodeType.TAIXU_BUILD, "Build APK Release", config = mapOf("projectType" to "android", "task" to "assembleRelease"), timeoutSeconds = 3600, canvasX = 500f, canvasY = 80f),
            WorkflowNode("approve", WorkflowNodeType.HUMAN_APPROVAL, "Xác nhận cài đặt", description = "Sau khi build xong, xác nhận có cài đặt lên thiết bị host không.", canvasX = 740f, canvasY = 80f),
            WorkflowNode("install", WorkflowNodeType.HOST_ACTION, "Cài đặt APK", config = mapOf("action" to "install-apk", "artifactFrom" to "build"), timeoutSeconds = 600, canvasX = 980f, canvasY = 80f),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Hoàn tất", canvasX = 1220f, canvasY = 80f),
        ),
        edges = chain("start", "git", "build", "approve", "install", "done"),
    )

    val buildDoctor = WorkflowDefinition(
        id = "mobile_build_doctor",
        name = "Chẩn đoán môi trường build di động",
        description = "Chạy taixu-build doctor và analyze, xuất báo cáo tương thích có thể hành động được.",
        category = "Chẩn đoán",
        isBuiltin = true,
        trigger = WorkflowTrigger.Proactive("BUILD_FAILED", suggestionLabel = "Chẩn đoán build thất bại"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu"),
            WorkflowNode("doctor", WorkflowNodeType.TAIXU_BUILD, "Chẩn đoán môi trường", config = mapOf("mode" to "doctor"), timeoutSeconds = 900),
            WorkflowNode("analyze", WorkflowNodeType.TAIXU_BUILD, "Phân tích project", config = mapOf("mode" to "analyze"), timeoutSeconds = 900),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Xem kết quả chẩn đoán"),
        ),
        edges = chain("start", "doctor", "analyze", "done"),
    )

    val installGeneratedApk = WorkflowDefinition(
        id = "install_generated_apk",
        name = "Cài đặt APK mới tạo nhất",
        description = "Dùng đường dẫn sản phẩm build, sau khi người dùng xác nhận sẽ giao cho host cài đặt.",
        category = "Android",
        isBuiltin = true,
        trigger = WorkflowTrigger.Proactive("APK_GENERATED", suggestionLabel = "Cài đặt APK vừa tạo"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Đọc APK", config = mapOf("requiredVariables" to "APK_PATH")),
            WorkflowNode("approve", WorkflowNodeType.HUMAN_APPROVAL, "Xác nhận cài đặt", description = "Xác nhận cài đặt APK vừa tạo lên thiết bị host: \${APK_PATH}", canvasX = 260f),
            WorkflowNode("install", WorkflowNodeType.HOST_ACTION, "Cài đặt APK", config = mapOf("action" to "install-apk"), timeoutSeconds = 600, canvasX = 520f),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Đã gửi yêu cầu cài đặt", canvasX = 780f),
        ),
        edges = chain("start", "approve", "install", "done"),
    )

    val reverseAudit = WorkflowDefinition(
        id = "android_reverse_audit",
        name = "Kiểm toán dịch ngược APK",
        description = "Sau khi giải nén và dịch ngược, Agent kiểm tra chỉ-đọc rồi xuất báo cáo rủi ro kèm bằng chứng; thư mục đầu ra đã có sẽ không bị ghi đè.",
        category = "Dịch ngược",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf android_reverse_audit"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Chọn APK", config = mapOf("requiredVariables" to "APK_PATH")),
            WorkflowNode("apktool", WorkflowNodeType.BASH_COMMAND, "Giải nén tài nguyên", config = mapOf("command" to "apktool d ${'$'}{APK_PATH} -o ./workflow-output/apktool"), timeoutSeconds = 1200),
            WorkflowNode("jadx", WorkflowNodeType.BASH_COMMAND, "Dịch ngược mã nguồn", config = mapOf("command" to "jadx -d ./workflow-output/jadx ${'$'}{APK_PATH}"), timeoutSeconds = 1800),
            WorkflowNode("audit", WorkflowNodeType.AGENT_INFERENCE, "Kiểm toán chỉ-đọc & báo cáo", config = mapOf("prompt" to "只读审计工作区 workflow-output/apktool 和 workflow-output/jadx。先索引并搜索清单、导出组件、网络配置及敏感数据处理，再阅读相关源码。不得修改文件或运行被审计程序。输出 Markdown 报告，列出文件与行号、证据、影响、建议及无法验证的事项；不要把推测写成已验证漏洞。"), timeoutSeconds = 1800),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Báo cáo kiểm toán"),
        ),
        edges = chain("start", "apktool", "jadx", "audit", "done"),
    )

    val atomicCommit = WorkflowDefinition(
        id = "git_atomic_smart_commit",
        name = "Xem lại & commit thay đổi",
        description = "Hiển thị thay đổi, sau khi người dùng xác nhận sẽ dùng thông điệp commit được truyền vào để tạo commit local; mặc định không push.",
        category = "Git",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf git_atomic_smart_commit"),
        defaultVariables = mapOf("COMMIT_MESSAGE" to "chore: update workspace"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu"),
            WorkflowNode("diff", WorkflowNodeType.BASH_COMMAND, "Xem lại thay đổi", config = mapOf("command" to "git status --short && git diff --stat && git diff && git diff --cached")),
            WorkflowNode("suggest", WorkflowNodeType.AGENT_INFERENCE, "Tạo gợi ý commit", config = mapOf("prompt" to "只读审阅以下 Git 变更，输出一条 Conventional Commit 建议和简短风险摘要。不得修改、暂存、提交或推送任何文件。变更：\n${'$'}{diff.output}")),
            WorkflowNode("approve", WorkflowNodeType.HUMAN_APPROVAL, "Xác nhận commit local", config = mapOf("requestedVariables" to "COMMIT_MESSAGE")),
            WorkflowNode("commit", WorkflowNodeType.BASH_COMMAND, "Tạo commit", config = mapOf("command" to "git add -A && git commit -m ${'$'}{COMMIT_MESSAGE}")),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Hoàn tất commit"),
        ),
        edges = chain("start", "diff", "suggest", "approve", "commit", "done"),
    )

    val buildRepair = WorkflowDefinition(
        id = "assisted_build_repair",
        name = "Sửa build sau khi xác nhận",
        description = "Chẩn đoán nguyên nhân thất bại, sau khi xác nhận sẽ giao Agent sửa tối thiểu, rồi hiển thị Diff và xác nhận build lại; không tự động commit hay push.",
        category = "Chẩn đoán",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf assisted_build_repair"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Nhập lỗi build", config = mapOf("requiredVariables" to "BUILD_ERROR")),
            WorkflowNode("diagnose", WorkflowNodeType.AGENT_INFERENCE, "Chẩn đoán chỉ-đọc", config = mapOf("prompt" to "只读检查当前工作区，根据以下错误说明根因和最小修复计划。不得修改文件。\n${'$'}{BUILD_ERROR}"), timeoutSeconds = 900),
            WorkflowNode("approve", WorkflowNodeType.HUMAN_APPROVAL, "Cho phép sửa tệp", description = "Xác nhận cho phép Agent sửa không gian làm việc hiện tại theo kết quả chẩn đoán. Hãy đảm bảo các sửa đổi quan trọng đã được sao lưu trước."),
            WorkflowNode("repair", WorkflowNodeType.SUBAGENT_DELEGATE, "Thực hiện sửa tối thiểu", config = mapOf("writePaths" to ".", "prompt" to "按以下诊断实施最小构建修复，保留用户已有修改；禁止删除项目、重置 Git、提交或推送。不能确定时停止并说明。诊断：\n${'$'}{diagnose.output}\n原错误：\n${'$'}{BUILD_ERROR}"), timeoutSeconds = 1800),
            WorkflowNode("diff", WorkflowNodeType.BASH_COMMAND, "Kiểm tra Diff sau sửa", config = mapOf("command" to "git status --short && git diff && git diff --cached")),
            WorkflowNode("rebuildApproval", WorkflowNodeType.HUMAN_APPROVAL, "Xác nhận build lại", description = "Xem lại bản sửa rồi khởi động build xác minh. Từ chối sẽ không hoàn tác các sửa đổi đã xong."),
            WorkflowNode("build", WorkflowNodeType.TAIXU_BUILD, "Xác minh build", config = mapOf("task" to "assembleDebug"), timeoutSeconds = 3600),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Kết quả xác minh sửa chữa"),
        ),
        edges = chain("start", "diagnose", "approve", "repair", "diff", "rebuildApproval", "build", "done"),
    )

    val hostAutomationLab = WorkflowDefinition(
        id = "host_automation_lab",
        name = "Phòng lab tự động hóa host",
        description = "Dò quyền → mở Cài đặt → chờ foreground → nhận diện màn hình → rẽ nhánh điều kiện → thông báo kết thúc. Cần Shizuku/Root mới chạy được toàn bộ chuỗi.",
        category = "Host",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf host_automation_lab"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu", canvasX = 40f, canvasY = 160f),
            WorkflowNode(
                "status",
                WorkflowNodeType.HOST_ACTION,
                "Kiểm tra chế độ quyền",
                config = mapOf("action" to "status"),
                canvasX = 240f,
                canvasY = 160f,
            ),
            WorkflowNode(
                "gate",
                WorkflowNodeType.CONDITION_BRANCH,
                "Đã có đặc quyền?",
                config = mapOf("expression" to "\${HOST_PRIVILEGED} == true"),
                failurePolicy = FailurePolicy.CONTINUE,
                canvasX = 460f,
                canvasY = 160f,
            ),
            WorkflowNode(
                "launch",
                WorkflowNodeType.HOST_ACTION,
                "Mở cài đặt hệ thống",
                config = mapOf("action" to "app_launch", "package" to "com.android.settings"),
                canvasX = 700f,
                canvasY = 60f,
            ),
            WorkflowNode(
                "wait",
                WorkflowNodeType.HOST_ACTION,
                "Chờ Cài đặt lên foreground",
                config = mapOf("action" to "wait_foreground", "package" to "com.android.settings", "timeoutSeconds" to "12"),
                canvasX = 920f,
                canvasY = 60f,
            ),
            WorkflowNode(
                "observe",
                WorkflowNodeType.HOST_ACTION,
                "Nhận diện control màn hình",
                config = mapOf("action" to "screen_observe", "onlyInteractive" to "true"),
                canvasX = 1140f,
                canvasY = 60f,
            ),
            WorkflowNode(
                "notify_ok",
                WorkflowNodeType.HOST_ACTION,
                "Thông báo: tự động hóa thành công",
                config = mapOf(
                    "action" to "notification",
                    "title" to "Workflow TaiXu",
                    "text" to "Phòng lab tự động hóa host đã hoàn tất nhận diện màn hình",
                ),
                canvasX = 1360f,
                canvasY = 60f,
            ),
            WorkflowNode(
                "degraded",
                WorkflowNodeType.HOST_ACTION,
                "Hạ cấp: chỉ hiện Toast",
                config = mapOf(
                    "action" to "toast",
                    "text" to "Không phát hiện Shizuku/Root, đã bỏ qua các bước tự động hóa đặc quyền",
                ),
                canvasX = 700f,
                canvasY = 260f,
            ),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Kết thúc lab", canvasX = 1580f, canvasY = 160f),
        ),
        edges = listOf(
            WorkflowEdge("e1", "start", "output", "status"),
            WorkflowEdge("e2", "status", "output", "gate"),
            WorkflowEdge("e3", "gate", "output", "launch", conditionExpression = "exitCode == 0"),
            WorkflowEdge("e4", "gate", "output", "degraded", conditionExpression = "exitCode != 0"),
            WorkflowEdge("e5", "launch", "output", "wait"),
            WorkflowEdge("e6", "wait", "output", "observe"),
            WorkflowEdge("e7", "observe", "output", "notify_ok"),
            WorkflowEdge("e8", "notify_ok", "output", "done"),
            WorkflowEdge("e9", "degraded", "output", "done"),
        ),
    )

    val hostBroadcastDemo = WorkflowDefinition(
        id = "host_broadcast_and_settings",
        name = "Demo broadcast + cài đặt hệ thống",
        description = "Ghi biến → gửi broadcast tùy chỉnh → đọc/ghi lại cài đặt độ sáng (cần đặc quyền). Có xác nhận thủ công trước thao tác phá hoại.",
        category = "Host",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf host_broadcast_and_settings"),
        defaultVariables = mapOf("DEMO_FLAG" to "taixu-workflow"),
        nodes = listOf(
            WorkflowNode("start", WorkflowNodeType.TRIGGER, "Bắt đầu"),
            WorkflowNode(
                "vars",
                WorkflowNodeType.SET_VARIABLE,
                "Chuẩn bị biến demo",
                config = mapOf(
                    "variables" to "BROADCAST_ACTION=top.wkbin.taixu.action.WORKFLOW_DEMO\nPAYLOAD=\${DEMO_FLAG}",
                ),
            ),
            WorkflowNode(
                "broadcast",
                WorkflowNodeType.HOST_ACTION,
                "Gửi broadcast demo",
                config = mapOf(
                    "action" to "send_broadcast",
                    "intentAction" to "\${BROADCAST_ACTION}",
                    "extras" to "source=workflow\npayload=\${PAYLOAD}\nflag:bool=true",
                ),
            ),
            WorkflowNode(
                "read_brightness",
                WorkflowNodeType.HOST_ACTION,
                "Đọc độ sáng hiện tại",
                config = mapOf(
                    "action" to "settings_get",
                    "namespace" to "system",
                    "key" to "screen_brightness",
                    "outputVariable" to "BRIGHTNESS",
                ),
            ),
            WorkflowNode(
                "approve",
                WorkflowNodeType.HUMAN_APPROVAL,
                "Xác nhận chỉnh độ sáng",
                description = "Sẽ đặt độ sáng thành 120 (có thể bỏ qua nếu từ chối). Giá trị hiện tại xem ở đầu ra node trước.",
                failurePolicy = FailurePolicy.CONTINUE,
            ),
            WorkflowNode(
                "gate",
                WorkflowNodeType.CONDITION_BRANCH,
                "Người dùng có phê duyệt?",
                config = mapOf("expression" to "exitCode == 0"),
                failurePolicy = FailurePolicy.CONTINUE,
            ),
            WorkflowNode(
                "write_brightness",
                WorkflowNodeType.HOST_ACTION,
                "Ghi độ sáng 120",
                config = mapOf(
                    "action" to "settings_put",
                    "namespace" to "system",
                    "key" to "screen_brightness",
                    "value" to "120",
                ),
            ),
            WorkflowNode(
                "delay",
                WorkflowNodeType.DELAY,
                "Chờ một chút",
                config = mapOf("seconds" to "1"),
            ),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Hoàn tất demo"),
        ),
        edges = listOf(
            WorkflowEdge("e1", "start", "output", "vars"),
            WorkflowEdge("e2", "vars", "output", "broadcast"),
            WorkflowEdge("e3", "broadcast", "output", "read_brightness"),
            WorkflowEdge("e4", "read_brightness", "output", "approve"),
            WorkflowEdge("e5", "approve", "output", "gate"),
            WorkflowEdge("e6", "gate", "output", "write_brightness", conditionExpression = "exitCode == 0"),
            WorkflowEdge("e7", "gate", "output", "done", conditionExpression = "exitCode != 0"),
            WorkflowEdge("e8", "write_brightness", "output", "delay"),
            WorkflowEdge("e9", "delay", "output", "done"),
        ),
    )

    /**
     * Agent-in-the-loop GUI pilot: privilege check → approve → optional launch →
     * AGENT_INFERENCE that drives host(screen_*) tools to chase [GUI_GOAL].
     */
    val hostAgentGuiPilot = WorkflowDefinition(
        id = "host_agent_gui_pilot",
        name = "Bay thử GUI bằng Agent",
        description = "Vòng lặp local dưới Shizuku/Root 「nhận diện màn hình → mô hình quyết định một bước → chạm/nhập」 để hoàn thành mục tiêu GUI. Mặc định: mở QQ → nhóm TaiXu → gửi văn bản bay thử. Hãy xem màn hình phê duyệt rồi mới chạy.",
        category = "Host",
        isBuiltin = true,
        trigger = WorkflowTrigger.Manual("/wf host_agent_gui_pilot"),
        defaultVariables = mapOf(
            "GUI_GOAL" to "打开 QQ，进入「太墟」相关 QQ 群聊，在输入框发送一句：「太墟牛逼（来自工作流）」。不要发红包、不要转账、不要改群设置。若找不到群名含「太墟」的群，停止并说明当前看到的会话列表。",
            "TARGET_PACKAGE" to "com.tencent.mobileqq",
        ),
        nodes = listOf(
            WorkflowNode(
                "start",
                WorkflowNodeType.TRIGGER,
                "Điền mục tiêu",
                config = mapOf("requiredVariables" to "GUI_GOAL"),
            ),
            WorkflowNode(
                "status",
                WorkflowNodeType.HOST_ACTION,
                "Kiểm tra Shizuku/Root",
                config = mapOf("action" to "status"),
            ),
            WorkflowNode(
                "priv_gate",
                WorkflowNodeType.CONDITION_BRANCH,
                "Đặc quyền đã sẵn sàng?",
                config = mapOf("expression" to "\${HOST_PRIVILEGED} == true"),
                failurePolicy = FailurePolicy.CONTINUE,
            ),
            WorkflowNode(
                "no_priv",
                WorkflowNodeType.HOST_ACTION,
                "Nhắc thiếu đặc quyền",
                config = mapOf(
                    "action" to "toast",
                    "text" to "Hãy cấp quyền trong Cài đặt và chuyển sang Shizuku hoặc Root, rồi thử lại workflow này",
                ),
            ),
            WorkflowNode(
                "approve",
                WorkflowNodeType.HUMAN_APPROVAL,
                "Xác nhận cho Agent điều khiển màn hình",
                description = "Agent sẽ dùng host(screen_observe/click/swipe/input_text/key/app_launch) để tự thao tác giao diện điện thoại nhằm hoàn thành GUI_GOAL.\n\n" +
                    "Mục tiêu mặc định sẽ mở QQ, vào nhóm TaiXu và gửi 「太墟牛逼（来自工作流）」. Hãy tự xem màn hình; nếu giao diện nhảy sang thanh toán/lì xì, hãy từ chối hoặc ngắt ngay.\n\n" +
                    "Từ chối thì kết thúc, không khởi động điều khiển.",
                failurePolicy = FailurePolicy.CONTINUE,
            ),
            WorkflowNode(
                "approve_gate",
                WorkflowNodeType.CONDITION_BRANCH,
                "Người dùng có phê duyệt?",
                config = mapOf("expression" to "exitCode == 0"),
                failurePolicy = FailurePolicy.CONTINUE,
            ),
            WorkflowNode(
                "launch",
                WorkflowNodeType.HOST_ACTION,
                "Mở app đích (tùy chọn)",
                config = mapOf(
                    "action" to "app_launch",
                    "package" to "\${TARGET_PACKAGE}",
                ),
                failurePolicy = FailurePolicy.CONTINUE,
            ),
            WorkflowNode(
                "wait_fg",
                WorkflowNodeType.DELAY,
                "Chờ giao diện ổn định",
                config = mapOf("seconds" to "2"),
            ),
            WorkflowNode(
                "pilot",
                WorkflowNodeType.HOST_ACTION,
                "Vòng lặp bay thử GUI",
                config = mapOf(
                    "action" to "gui_pilot",
                    "goal" to "\${GUI_GOAL}",
                    "package" to "\${TARGET_PACKAGE}",
                    "maxSteps" to "18",
                ),
                timeoutSeconds = 900,
            ),
            WorkflowNode(
                "notify",
                WorkflowNodeType.HOST_ACTION,
                "Thông báo kết thúc bay thử",
                config = mapOf(
                    "action" to "notification",
                    "title" to "Kết thúc bay thử GUI",
                    "text" to "Bay thử GUI bằng Agent đã kết thúc, hãy xem đầu ra workflow",
                ),
                failurePolicy = FailurePolicy.CONTINUE,
            ),
            WorkflowNode("done", WorkflowNodeType.TERMINAL_OUTPUT, "Báo cáo bay thử"),
            WorkflowNode("aborted", WorkflowNodeType.TERMINAL_OUTPUT, "Đã hủy hoặc thiếu đặc quyền"),
        ),
        edges = listOf(
            WorkflowEdge("e1", "start", "output", "status"),
            WorkflowEdge("e2", "status", "output", "priv_gate"),
            WorkflowEdge("e3", "priv_gate", "output", "approve", conditionExpression = "exitCode == 0"),
            WorkflowEdge("e4", "priv_gate", "output", "no_priv", conditionExpression = "exitCode != 0"),
            WorkflowEdge("e5", "no_priv", "output", "aborted"),
            WorkflowEdge("e6", "approve", "output", "approve_gate"),
            WorkflowEdge("e7", "approve_gate", "output", "launch", conditionExpression = "exitCode == 0"),
            WorkflowEdge("e8", "approve_gate", "output", "aborted", conditionExpression = "exitCode != 0"),
            WorkflowEdge("e9", "launch", "output", "wait_fg"),
            WorkflowEdge("e10", "wait_fg", "output", "pilot"),
            WorkflowEdge("e11", "pilot", "output", "notify"),
            WorkflowEdge("e12", "notify", "output", "done"),
        ),
    )
}
