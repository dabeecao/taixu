package top.wkbin.taixu.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.wkbin.taixu.core.model.StorageMountBinding
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeButton as Button
import top.wkbin.taixu.ui.components.RuntimeTextButton as TextButton
import top.wkbin.taixu.ui.settings.LocalizedText as Text

@Composable
internal fun AddMountDialog(
    existingGuestPaths: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, hostPath: String, guestPath: String) -> Unit,
) {
    var name by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var hostPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("/storage/emulated/0/") }
    var guestPath by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("/mnt/") }

    val nameValid = name.isNotBlank()
    val trimmedHost = hostPath.trim()
    val hostCharInvalid = ':' in trimmedHost
    val hostRootAllowed = StorageMountBinding.isHostPathAllowed(trimmedHost)
    val hostPathValid = hostPath.isNotBlank() && !hostCharInvalid && hostRootAllowed

    // 与 SettingsViewModel.addCustomMountBinding 的入参归一保持一致：无前导 / 自动补
    val trimmedGuest = guestPath.trim().let { if (it.startsWith("/")) it else "/$it" }
    val guestDuplicate = trimmedGuest in existingGuestPaths
    val guestNormalized = StorageMountBinding.normalizeGuestPath(trimmedGuest)
    val guestCharInvalid = ':' in trimmedGuest
    val guestRootAllowed = guestNormalized != null && StorageMountBinding.isGuestPathAllowed(trimmedGuest)
    val guestPathValid = guestPath.isNotBlank() && guestNormalized != null &&
        guestRootAllowed && !guestCharInvalid && !guestDuplicate

    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm điểm gắn lưu trữ", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên điểm gắn") },
                    placeholder = { Text("Ví dụ: ảnh album, source dự án") },
                    isError = !nameValid,
                    supportingText = { if (!nameValid) Text("Vui lòng nhập tên điểm gắn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hostPath,
                    onValueChange = { hostPath = it },
                    label = { Text("Đường dẫn host (Android)") },
                    placeholder = { Text("/storage/emulated/0/...") },
                    isError = hostPath.isNotBlank() && !hostPathValid,
                    supportingText = {
                        when {
                            hostPath.isNotBlank() && hostCharInvalid -> Text("Đường dẫn host không được chứa ký tự không hợp lệ như dấu hai chấm")
                            hostPath.isNotBlank() && !hostRootAllowed ->
                                Text("Đường dẫn host phải nằm trong /storage/emulated/0")
                            else -> {}
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = guestPath,
                    onValueChange = { guestPath = it },
                    label = { Text("Đường dẫn gắn trong container (Linux)") },
                    placeholder = { Text("/mnt/my_folder") },
                    isError = guestPath.isNotBlank() && !guestPathValid,
                    supportingText = {
                        when {
                            guestPath.isNotBlank() && !guestPath.trim().startsWith("/") -> Text("Đường dẫn phải bắt đầu bằng /")
                            guestPath.isNotBlank() && guestNormalized == null -> Text("Đường dẫn container không được chứa ..")
                            guestPath.isNotBlank() && guestCharInvalid -> Text("Đường dẫn container không được chứa ký tự không hợp lệ như dấu hai chấm")
                            guestPath.isNotBlank() && !guestRootAllowed ->
                                Text("Đường dẫn container phải nằm trong /mnt hoặc /sdcard")
                            guestDuplicate -> Text("Đường dẫn container này đã được điểm gắn khác dùng")
                            else -> {}
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, hostPath, guestPath) },
                enabled = nameValid && hostPathValid && guestPathValid,
            ) {
                Text("Thêm điểm gắn")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}
