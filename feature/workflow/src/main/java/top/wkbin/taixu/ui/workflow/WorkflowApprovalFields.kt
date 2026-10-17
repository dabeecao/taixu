package top.wkbin.taixu.ui.workflow

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.wkbin.taixu.core.model.workflow.WorkflowExpansions
import top.wkbin.taixu.core.model.workflow.WorkflowApprovalTimeout

@Composable
internal fun WorkflowApprovalFields(config: MutableMap<String, String>) {
    OutlinedTextField(value = config[WorkflowApprovalTimeout.CONFIG_KEY].orEmpty(), onValueChange = {
        if (it.isBlank()) config.remove(WorkflowApprovalTimeout.CONFIG_KEY) else config[WorkflowApprovalTimeout.CONFIG_KEY] = it.trim()
    }, label = { Text("Giới hạn chờ phê duyệt tối đa (giây, tùy chọn)") },
        isError = WorkflowApprovalTimeout.validationError(config[WorkflowApprovalTimeout.CONFIG_KEY]) != null,
        supportingText = { Text("Để trống là không giới hạn; nhập 1–3600 giây, quá timeout xử lý theo chính sách thất bại. Timeout nút chung không áp dụng cho chờ phê duyệt.") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = config["prompt"].orEmpty(), onValueChange = { config["prompt"] = it },
        label = { Text("Mô tả xác nhận phê duyệt (bắt buộc)") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = config[WorkflowExpansions.SOURCE_KEY].orEmpty(), onValueChange = {
        if (it.isBlank()) config.remove(WorkflowExpansions.SOURCE_KEY) else config[WorkflowExpansions.SOURCE_KEY] = it.trim()
    }, label = { Text("ID nút nguồn kết quả mở rộng đồ thị động (tùy chọn)") },
        supportingText = { Text("Điền xong xác nhận đề xuất nodes/edges có cấu trúc của Agent đó; cần nối trực tiếp qua cạnh success.") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
}
