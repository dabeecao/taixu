package top.wkbin.taixu.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AgentPlugin(
    val id: String,
    val name: String,
    val description: String,
    val version: String = "1.0.0",
    val author: String = "TaiXu Core Team",
    val permissions: List<String> = emptyList(),
    val iconName: String = "Package",
    val isEnabled: Boolean = true,
    val isBuiltin: Boolean = true,
)

object BuiltinPlugins {
    val presets: List<AgentPlugin> = listOf(
        AgentPlugin(
            id = "proot_health_probe",
            name = "Probe chẩn đoán sandbox PRoot",
            description = "Dò tìm thời gian thực hệ thống tệp ảo, tiến trình và bộ nhớ Linux PRoot, tự hỗ trợ tạo gợi ý chẩn đoán khi bất thường",
            version = "1.0.0",
            author = "TaiXu System",
            permissions = listOf("Chạy probe trạng thái hệ thống", "Thu thập chỉ số chẩn đoán PRoot"),
            iconName = "Terminal",
            isEnabled = true,
            isBuiltin = true,
        ),
        AgentPlugin(
            id = "destructive_cmd_interceptor",
            name = "Bộ chặn kiểm duyệt lệnh nguy hiểm",
            description = "Kiểm tra an toàn trước và sửa tham số cho lệnh phá hủy như rm -rf /, mkfs, dd, tránh làm hại sandbox hoặc không gian làm việc",
            version = "1.1.0",
            author = "Security Lab",
            permissions = listOf("Kiểm duyệt bảo mật lệnh", "Chặn thao tác nguy hiểm"),
            iconName = "Alert",
            isEnabled = true,
            isBuiltin = true,
        ),
        AgentPlugin(
            id = "web_search_augment",
            name = "Tăng cường tìm kiếm web và lập chỉ mục tài liệu",
            description = "Bổ sung cho Agent kho tri thức nhà phát triển bên ngoài, tài liệu gói Debian và chỉ mục lỗi thường gặp",
            version = "0.9.0",
            author = "Community",
            permissions = listOf("Yêu cầu mạng", "Lập chỉ mục tri thức"),
            iconName = "Globe",
            isEnabled = false,
            isBuiltin = true,
        ),
        AgentPlugin(
            id = "host_bridge",
            name = "Cầu nối host · ADB tích hợp",
            description = "Trong sandbox truy cập năng lực máy chủ qua 127.0.0.1:7980: cài APK lén, thực thi Shell đặc quyền. Ưu tiên ADB gỡ lỗi không dây tích hợp (không cần Root hay Shizuku ngoài), cũng có thể dùng Shizuku/Root.",
            version = "1.0.0",
            author = "TaiXu System",
            permissions = listOf(
                "本地回环网络监听 (127.0.0.1)",
                "安装未知来源应用 (REQUEST_INSTALL_PACKAGES)",
                "无线调试配对与连接 (Android 11+)",
            ),
            iconName = "Cable",
            isEnabled = true,
            isBuiltin = true,
        ),
    )
}
