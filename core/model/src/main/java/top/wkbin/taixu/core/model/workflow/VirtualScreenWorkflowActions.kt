package top.wkbin.taixu.core.model.workflow

/** Persisted IDs share the HOST tool's virtual-screen primitives. Coordinates are always 0–1000. */
object VirtualScreenWorkflowActions {
    private fun field(key: String, label: String, hint: String = "", required: Boolean = false) =
        HostWorkflowField(key, label, hint, required, multiline = key == "text")

    private fun action(id: String, label: String, description: String, vararg fields: HostWorkflowField) =
        HostWorkflowActionDef(
            id, label, "Màn hình ảo", HostWorkflowPrivilege.NONE, description,
            listOf(field("session", "Phiên màn hình ảo", "Để trống để cách ly theo workflow lần này; các bước trong cùng một lần chạy dùng chung màn hình")) + fields +
                field("wait_ms", "Chờ sau thao tác (ms)", "Mặc định 0; 0–600000, dùng được biến"),
        )

    val all = listOf(
        action("virtual_screen_ensure", "Tạo màn hình ảo", "Tạo màn hình ảo độc lập, cần dịch vụ Shower khả dụng."),
        action("virtual_screen_launch", "Mở app trên màn hình ảo", "Khởi động app theo tên package, không gọi mô hình.",
            field("package", "Tên package của app", "Ví dụ: com.android.settings hoặc \${PACKAGE}", true)),
        action("virtual_screen_click", "Chạm trên màn hình ảo", "Tọa độ tương đối 0–1000, không phụ thuộc thu phóng ảnh chụp.",
            field("x", "X (0–1000)", required = true), field("y", "Y (0–1000)", required = true)),
        action("virtual_screen_double_click", "Chạm đúp trên màn hình ảo", "Tọa độ tương đối 0–1000.",
            field("x", "X (0–1000)", required = true), field("y", "Y (0–1000)", required = true)),
        action("virtual_screen_long_press", "Nhấn giữ trên màn hình ảo", "Có thể chỉnh riêng tọa độ và thời gian giữ.",
            field("x", "X (0–1000)", required = true), field("y", "Y (0–1000)", required = true),
            field("duration_ms", "Thời gian giữ (ms)", "Mặc định 800; 200–5000")),
        action("virtual_screen_swipe", "Vuốt trên màn hình ảo", "Điểm đầu/cuối đều 0–1000, thời gian cử chỉ tách riêng với chờ sau thao tác.",
            field("x1", "X điểm đầu (0–1000)", required = true), field("y1", "Y điểm đầu (0–1000)", required = true),
            field("x2", "X điểm cuối (0–1000)", required = true), field("y2", "Y điểm cuối (0–1000)", required = true),
            field("duration_ms", "Thời gian cử chỉ (ms)", "Mặc định 300; 50–5000")),
        action("virtual_screen_input_text", "Nhập thêm trên màn hình ảo", "Chèn thêm vào focus hiện tại, giữ khoảng trắng đầu/cuối văn bản.",
            field("text", "Nội dung nhập", "Hỗ trợ nhiều dòng hoặc \${TEXT_1}", true)),
        action("virtual_screen_set_text", "Thay thế nội dung nhập", "Gửi phím chọn tất cả và dán; control đích cần hỗ trợ, văn bản rỗng để xóa sạch.",
            field("text", "Nội dung thay thế", "Hỗ trợ nhiều dòng hoặc \${TEXT_1}")),
        action("virtual_screen_key", "Nhấn phím trên màn hình ảo", "Gửi phím tới màn hình ảo.",
            field("key", "Phím", "back / home / enter / delete / paste / recents", true)),
        action("virtual_screen_wait", "Chờ trên màn hình ảo", "Chỉ chờ, không gọi mô hình.",
            field("duration_ms", "Thời gian chờ (ms)", "Mặc định 1000; 0–600000", true)),
        action("virtual_screen_close", "Đóng màn hình ảo", "Giải phóng màn hình của lần chạy này."),
    )

    fun template(): WorkflowDefinition = PhoneOperationWorkflow.create(
        id = "virtual_screen_template", name = "Mẫu thao tác màn hình ảo",
        operations = listOf(
            PhoneWorkflowOperation("virtual_screen_launch", mapOf("package" to "\${PACKAGE}"), 900),
        ),
    ).copy(
        isBuiltin = true,
        description = "Sao chép rồi thêm các node chạm, vuốt, nhập và chờ; từng bước có thể tinh chỉnh riêng. Phát lại bước cố định không gọi mô hình, hãy xác nhận trang app trước khi chạy.",
        defaultVariables = mapOf("PACKAGE" to "com.android.settings"),
    )
}
