package top.wkbin.taixu.ui.workflow

import top.wkbin.taixu.core.model.workflow.WorkflowNodeType
import top.wkbin.taixu.ui.components.RuntimeIconName

data class NodeTypeMeta(
    val type: WorkflowNodeType,
    val label: String,
    val icon: RuntimeIconName,
    val summary: String,
    val guide: String,
)

fun WorkflowNodeType.metadata(): NodeTypeMeta = when (this) {
    WorkflowNodeType.PLUGIN_ACTION -> NodeTypeMeta(this, "Hành động plugin", RuntimeIconName.Extension, "Gọi nút plugin đã đăng ký lúc biên dịch", "Điền danh tính plugin & mở rộng, tham số JSON. Mỗi lần chạy đều cần xác nhận, tác dụng phụ chưa rõ sẽ không tự phát lại.")
    WorkflowNodeType.TRIGGER -> NodeTypeMeta(
        type = this,
        label = "Trigger",
        icon = RuntimeIconName.Play,
        summary = "Cổng vào khởi động quy trình, hỗ trợ bấm thủ công hoặc kích hoạt bởi sự kiện ngoài",
        guide = "Là nút bắt đầu của đồ thị DAG. Khi bạn bấm “Chạy gỡ lỗi” ở trên cùng hoặc nhận tín hiệu kích hoạt, workflow bắt đầu chạy xuống từ đây.",
    )
    WorkflowNodeType.BASH_COMMAND -> NodeTypeMeta(
        type = this,
        label = "Dòng lệnh Shell",
        icon = RuntimeIconName.Terminal,
        summary = "Chạy một lệnh Shell hoặc script trong Linux sandbox PRoot",
        guide = "Có thể chạy mọi lệnh đã cài trong sandbox (như git, make, python, curl …). Hỗ trợ tham chiếu \${WORKSPACE_PATH} tới thư mục gốc project, hoặc dùng \${previous.output} để lấy output phía trước.",
    )
    WorkflowNodeType.PROCESS_SERVICE -> NodeTypeMeta(
        type = this,
        label = "Dịch vụ thường trú nền",
        icon = RuntimeIconName.Settings,
        summary = "Khởi động daemon thường trú nền của sandbox, không chặn các bước sau tiếp tục chạy",
        guide = "Phù hợp để khởi động http-server, Web dịch vụ hoặc tiến trình lắng nghe. Tiến trình ID sẽ tự lưu trong biến \${PROCESS_ID} để các bước sau tham chiếu.",
    )
    WorkflowNodeType.AGENT_INFERENCE -> NodeTypeMeta(
        type = this,
        label = "Suy luận AI Agent",
        icon = RuntimeIconName.Sparkles,
        summary = "Điều phối AI Agent kết hợp chỉ thị tác vụ và output phía trước để phân tích và cho ra kết quả",
        guide = "Giao chỉ thị tác vụ (Prompt) và sản phẩm đầu ra phía trước cho AI Agent xử lý. Hỗ trợ kết hợp \${previous.output} để review code, gỡ lỗi hoặc sinh patch sửa lỗi.",
    )
    WorkflowNodeType.SUBAGENT_DELEGATE -> NodeTypeMeta(
        type = this,
        label = "Ủy thác subagent",
        icon = RuntimeIconName.Hub,
        summary = "Ủy thác subagent chuyên trách độc lập thực hiện tác vụ chi tiết hơn",
        guide = "Ủy thác subagent vai trò cụ thể (như trình sinh unit test, chuyên viên phân tích lỗi build) xử lý tác vụ chuyên biệt, cô lập ngữ cảnh Agent chính.",
    )
    WorkflowNodeType.TAIXU_BUILD -> NodeTypeMeta(
        type = this,
        label = "Build offline TaiXu",
        icon = RuntimeIconName.Package,
        summary = "Gọi engine build offline trong sandbox để biên dịch đóng gói Android/C++/Rust",
        guide = "Gọi engine biên dịch taixu-build tích hợp trong sandbox. Hỗ trợ các kiểu android, cmake, cargo…, có thể cho ra trực tiếp gói cài APK hoặc file nhị phân thực thi.",
    )
    WorkflowNodeType.CONDITION_BRANCH -> NodeTypeMeta(
        type = this,
        label = "Rẽ nhánh có điều kiện",
        icon = RuntimeIconName.Link,
        summary = "Đánh giá biểu thức rồi rẽ theo đúng/sai",
        guide = "Hỗ trợ exitCode, output contains, \${biến} so sánh, cùng với && / ||. Khi đúng exitCode=0, ngược lại là 1; hãy dùng điều kiện bên cạnh hoặc success/failure cổng để rẽ nhánh.",
    )
    WorkflowNodeType.HUMAN_APPROVAL -> NodeTypeMeta(
        type = this,
        label = "Phê duyệt thủ công",
        icon = RuntimeIconName.Alert,
        summary = "Tạm dừng quy trình và bật hộp thoại xác nhận, chờ người xác nhận hoặc nhập tham số rồi tiếp tục",
        guide = "Điểm gác an toàn trước bước quan trọng (như phát hành, dọn phá hoại hay lên production). Khi quy trình chạy tới đây sẽ tự tạm dừng và bật popup, chờ bạn xác nhận thủ công hoặc điền tham số cần thiết.",
    )
    WorkflowNodeType.HOST_ACTION -> NodeTypeMeta(
        type = this,
        label = "Hành động hệ thống host",
        icon = RuntimeIconName.Android,
        summary = "Mở ứng dụng, gửi broadcast, đổi cài đặt, điều khiển màn hình và các năng lực host Android khác",
        guide = "Xuyên sandbox gọi Android: quản lý ứng dụng, Intent/broadcast, cài đặt hệ thống, tự động hóa GUI, thông báo và shell đặc quyền. Hành động gắn nhãn「cần đặc quyền」đòi hỏi Shizuku hoặc Root.",
    )
    WorkflowNodeType.DELAY -> NodeTypeMeta(
        type = this,
        label = "Chờ trễ",
        icon = RuntimeIconName.Speed,
        summary = "Tạm dừng vài giây rồi tiếp tục các nút sau",
        guide = "Để chờ chuyển màn hình, broadcast tới nơi hoặc cho người thời gian quan sát.",
    )
    WorkflowNodeType.SET_VARIABLE -> NodeTypeMeta(
        type = this,
        label = "Đặt biến",
        icon = RuntimeIconName.Tune,
        summary = "Tiêm biến KEY=value cho các nút tiếp theo",
        guide = "Mỗi dòng một phép gán, vế phải hỗ trợ \${VAR} nội suy. Biến được ghi sẽ vào ngữ cảnh toàn cục, điều kiện và hành động host có thể tham chiếu.",
    )
    WorkflowNodeType.TERMINAL_OUTPUT -> NodeTypeMeta(
        type = this,
        label = "Xuất lưu trữ kết quả",
        icon = RuntimeIconName.Check,
        summary = "Điểm cuối workflow, tự tổng hợp mọi nhật ký output, thời gian chạy và file sản phẩm",
        guide = "Nút kết thúc của quy trình DAG. Sẽ tự thu thập và hiển thị output chuẩn terminal, mã thoát và danh sách file sản phẩm của mọi giai đoạn phía trước.",
    )
}
