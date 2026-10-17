package top.wkbin.taixu.ui.workflow

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.wkbin.taixu.core.model.workflow.WorkflowResultSchema

@Composable
internal fun WorkflowAgentFields(config: MutableMap<String, String>, delegate: Boolean) {
    OutlinedTextField(value = config["prompt"].orEmpty(), onValueChange = { config["prompt"] = it },
        label = { Text(if (delegate) "Yêu cầu tác vụ con (bắt buộc)" else "Yêu cầu tác vụ / prompt (bắt buộc)") },
        supportingText = { Text("Hỗ trợ dùng \${previous.output} để tham chiếu output nút phía trước") },
        minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = config["role"].orEmpty(), onValueChange = { config["role"] = it },
        label = { Text("Vai trò Agent (tùy chọn)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    if (!delegate) OutlinedTextField(value = config["modelId"].orEmpty(), onValueChange = { config["modelId"] = it },
        label = { Text("ID cấu hình mô hình cố định (tùy chọn)") }, supportingText = { Text("Để trống dùng WORKFLOW_MODEL_ID do runtime chọn") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = config[WorkflowResultSchema.CONFIG_KEY].orEmpty(), onValueChange = {
        if (it.isBlank()) config.remove(WorkflowResultSchema.CONFIG_KEY) else config[WorkflowResultSchema.CONFIG_KEY] = it
    }, label = { Text("JSON Schema kết quả có cấu trúc (tùy chọn)") },
        supportingText = { Text("Mở rộng đồ thị động cần khai báo nodes và edges; xem chi tiết và ví dụ trong tài liệu workflow.") },
        minLines = 2, maxLines = 6, modifier = Modifier.fillMaxWidth())
}
