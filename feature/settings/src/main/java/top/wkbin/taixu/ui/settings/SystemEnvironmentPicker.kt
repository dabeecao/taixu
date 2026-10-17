package top.wkbin.taixu.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.core.model.RemoteServer
import top.wkbin.taixu.ui.components.*

@Composable
internal fun SystemEnvironmentPicker(localCount: Int, onInstallLocal: () -> Unit, viewModel: RemoteServerViewModel? = previewOrNull { koinViewModel() }) {
    if (viewModel == null) {
        Text("Backend chạy", style = MaterialTheme.typography.titleMedium)
        return
    }
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val active by viewModel.activeId.collectAsStateWithLifecycle()
    val configurationError by viewModel.configurationError.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    var addRemote by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<RemoteServer?>(null) }
    var editing by remember { mutableStateOf<RemoteServer?>(null) }
    var chooseRemote by remember { mutableStateOf(false) }
    var choose by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        configurationError?.let { NoticeBanner(text = it, isError = true) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Backend chạy", style = MaterialTheme.typography.titleMedium)
            RuntimeButton(onClick = { choose = true }) { Text("Thêm hệ thống") }
        }
        NoticeBanner(text = "Hệ thống chính quyết định môi trường cho cuộc trò chuyện mới, chỉ số trang chủ và dự án xưởng. Cuộc trò chuyện hiện có giữ nguyên môi trường, file máy chủ không tự sao chép về điện thoại.")
        BackendChoiceCard("PRoot nội bộ", "Chạy Linux trên điện thoại, dùng không gian làm việc nội bộ.", RuntimeIconName.Linux, active == null) { viewModel.select(null) }
        BackendChoiceCard("Runtime SSH từ xa", servers.firstOrNull { it.id == active }?.name ?: "Dùng sức mạnh tính toán, toolchain và thư mục dự án của máy chủ.",
            RuntimeIconName.Server, active != null) {
            when (servers.size) {
                0 -> { viewModel.clear(); addRemote = true }
                1 -> viewModel.select(servers.single().id)
                else -> chooseRemote = true
            }
        }
        connection.message?.let { Text(it, color = if (connection.tested) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Hồ sơ kết nối (${servers.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            RuntimeTextButton(onClick = { viewModel.clear(); addRemote = true }) { Text("Thêm kết nối mới") }
        }
        if (servers.isEmpty()) Text("Sau khi thêm kết nối SSH, có thể chọn nó làm hệ thống chính.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        servers.forEach { server ->
            ConnectionProfileCard(server, server.id == active, onSelect = { viewModel.select(server.id) },
                onEdit = { viewModel.clear(); editing = server }, onRemove = { remove = server })
        }
    }
    if (choose) RuntimeAlertDialog(
        onDismissRequest = { choose = false }, title = { Text("Chọn nguồn hệ thống") },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RuntimeOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { choose = false; onInstallLocal() }) { Text("Cài Linux nội bộ") }
            RuntimeOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { choose = false; viewModel.clear(); addRemote = true }) { Text("Kết nối máy chủ") }
        } },
        confirmButton = { RuntimeTextButton(onClick = { choose = false }) { Text("Hủy") } },
    )
    if (addRemote) RemoteServerDialog(viewModel) { addRemote = false }
    editing?.let { target -> key(target.id) { RemoteServerDialog(viewModel, target) { editing = null } } }
    if (chooseRemote) RuntimeAlertDialog(onDismissRequest = { chooseRemote = false }, title = { Text("Chọn hồ sơ kết nối") },
        text = { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            servers.forEach { server -> RuntimeOutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.select(server.id); chooseRemote = false }) {
                Text(server.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } }
        } }, confirmButton = { RuntimeTextButton(onClick = { chooseRemote = false }) { Text("Hủy") } })
    remove?.let { server -> RuntimeAlertDialog(
        onDismissRequest = { remove = null }, title = { Text("Gỡ ${server.name}?") },
        text = { Text("Chỉ gỡ kết nối và credential lưu trên điện thoại, file máy chủ được giữ lại. Phiên hiện có cần kết thúc rồi chọn lại môi trường.") },
        confirmButton = { RuntimeTextButton(onClick = { viewModel.remove(server.id); remove = null }) { Text("Gỡ bỏ") } },
        dismissButton = { RuntimeTextButton(onClick = { remove = null }) { Text("Hủy") } },
    ) }
}

