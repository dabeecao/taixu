package top.wkbin.taixu.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.wkbin.taixu.core.model.McpConnectionState
import top.wkbin.taixu.core.model.McpServerConfig
import top.wkbin.taixu.ui.components.RuntimePreview

/** No ViewModels, services, credentials, or connection probes are created by these fixtures. */
@Preview(name = "Trạng thái dịch vụ · sáng", widthDp = 360, heightDp = 900, locale = "zh")
@Preview(name = "Trạng thái dịch vụ · chữ lớn nền tối", widthDp = 360, heightDp = 900, fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "zh")
@Composable
private fun PreferenceStatePreview() {
    RuntimePreview {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            McpServerItemCard(
                server = McpServerConfig(id = "online", name = "Dịch vụ phân tích file & dự án không gian làm việc",
                    description = "Cung cấp khả năng truy vấn và phân tích file của không gian làm việc hiện tại", isEnabled = true),
                connectionState = McpConnectionState.ONLINE, error = null, togglePending = false,
                requiredComponentName = null, onToggle = {}, onClickDetail = {})
            McpServerItemCard(
                server = McpServerConfig(id = "blocked", name = "Dịch vụ công cụ đang chờ cài đặt", isEnabled = false),
                connectionState = McpConnectionState.UNKNOWN, error = null, togglePending = false,
                requiredComponentName = "Component mẫu", onToggle = {}, onClickDetail = {})
            McpServerItemCard(
                server = McpServerConfig(id = "offline", name = "Dịch vụ ngoài offline", isEnabled = true),
                connectionState = McpConnectionState.OFFLINE, error = "Tạm thời không kết nối được, hãy kiểm tra cấu hình dịch vụ trong chi tiết.",
                togglePending = false, requiredComponentName = null, onToggle = {}, onClickDetail = {})
            EditorToggleRow("Tự thích ứng theo năng lực model", "Hiển thị tùy chọn liên quan theo năng lực model hiện tại cung cấp", true, onCheckedChange = {})
        }
    }
}
