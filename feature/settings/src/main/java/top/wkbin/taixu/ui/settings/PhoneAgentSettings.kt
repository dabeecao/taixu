package top.wkbin.taixu.ui.settings
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.core.datastore.AgentPreferences
import top.wkbin.taixu.core.datastore.PhoneAgentEndpoint
import top.wkbin.taixu.core.tools.AgentModelConnectionTester
import top.wkbin.taixu.ui.components.RuntimeButton
import top.wkbin.taixu.ui.components.RuntimeCircularProgressIndicator
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton
import top.wkbin.taixu.ui.components.RuntimeTextButton
import top.wkbin.taixu.ui.components.RuntimeTopBar
import top.wkbin.taixu.ui.settings.LocalizedText as Text
class PhoneAgentSettingsViewModel(
    private val preferences: AgentPreferences,
    private val connectionTester: AgentModelConnectionTester,
) : ViewModel() {
    val config: Flow<PhoneAgentEndpoint> = preferences.phoneAgentConfig
    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    fun save(value: PhoneAgentEndpoint) {
        viewModelScope.launch { preferences.setPhoneAgentConfig(value) }
    }

    fun test(baseUrl: String, model: String, apiKey: String, onResult: (String) -> Unit) {
        val error = phoneAgentConfigError(baseUrl, model, apiKey)
        if (error != null) {
            onResult(error)
            return
        }
        viewModelScope.launch {
            _testing.value = true
            val result = runCatching {
                connectionTester.test(baseUrl.trim(), model.trim(), apiKey.trim())
            }.fold(
                onSuccess = { "Kết nối thành công" },
                onFailure = { it.message?.take(180) ?: "Kết nối thất bại, vui lòng kiểm tra địa chỉ endpoint, khóa và mạng" },
            )
            _testing.value = false
            onResult(result)
        }
    }
}

@Composable
internal fun PhoneAgentSettingsEntry(
    onOpen: () -> Unit,
    viewModel: PhoneAgentSettingsViewModel? = previewOrNull { koinViewModel() },
) {
    val config by viewModel?.config?.collectAsStateWithLifecycle(PhoneAgentEndpoint()) ?: previewStateOf(PhoneAgentEndpoint())
    SettingsRow(
        icon = RuntimeIconName.Android,
        title = "Mô hình điều khiển điện thoại",
        subtitle = "Endpoint, tên mô hình và khóa riêng, sẽ không hiện trong mô hình trò chuyện",
        value = config.model.ifBlank { "Chưa cấu hình" },
        onClick = onOpen,
    )
}

@Composable
fun PhoneAgentSettingsScreen(
    onBack: () -> Unit,
    viewModel: PhoneAgentSettingsViewModel? = previewOrNull { koinViewModel() },
) {
    val saved by viewModel?.config?.collectAsStateWithLifecycle(PhoneAgentEndpoint()) ?: previewStateOf(PhoneAgentEndpoint())
    var baseUrl by remember(saved) { mutableStateOf(saved.baseUrl) }
    var model by remember(saved) { mutableStateOf(saved.model) }
    var apiKey by remember(saved) { mutableStateOf(saved.apiKey) }
    var message by remember { mutableStateOf<String?>(null) }
    val testing by viewModel?.testing?.collectAsStateWithLifecycle() ?: previewStateOf(false)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { RuntimeTopBar("Mô hình điều khiển điện thoại", onBack) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .then(top.wkbin.taixu.ui.components.runtimeScrollViewport(padding))
                .verticalScroll(rememberScrollState()).padding(bottom = padding.calculateBottomPadding())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Chỗ này chỉ dùng cho màn hình ảo: xem màn hình, chạm, vuốt và nhập liệu. Model chính cho chat vẫn cấu hình ở 「Quản lý hồ sơ mô hình」, hai bên không ảnh hưởng nhau.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsGroup {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Địa chỉ giao diện") },
                        placeholder = { Text("https://open.bigmodel.cn/api/paas/v4") },
                        supportingText = { Text("Nền tảng mở ZhiPu dùng địa chỉ trên") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tên model") },
                        placeholder = { Text("autoglm-phone") },
                        supportingText = { Text("Tên mô hình điều khiển điện thoại của ZhiPu là autoglm-phone") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Khóa API") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                    )
                }
            }
            RuntimeOutlinedButton(
                onClick = { viewModel?.test(baseUrl, model, apiKey) { message = it } },
                enabled = !testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (testing) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuntimeCircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Đang kiểm tra…")
                    }
                } else {
                    Text("Kiểm tra kết nối")
                }
            }
            RuntimeButton(
                onClick = {
                    val error = phoneAgentConfigError(baseUrl, model, apiKey)
                    if (error != null) {
                        message = error
                    } else {
                        viewModel?.save(PhoneAgentEndpoint(baseUrl.trim(), model.trim(), apiKey.trim()))
                        message = "Đã lưu. Mô hình chính của trò chuyện sẽ không dùng tới đây."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) { Text("Lưu") }
            RuntimeTextButton(
                onClick = {
                    baseUrl = ""
                    model = ""
                    apiKey = ""
                    viewModel?.save(PhoneAgentEndpoint())
                    message = "Đã xóa"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Xóa") }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun phoneAgentConfigError(baseUrl: String, model: String, apiKey: String): String? {
    val url = baseUrl.trim()
    if (url.isBlank() || model.isBlank() || apiKey.isBlank()) return "Phải điền đầy đủ địa chỉ endpoint, tên mô hình và khóa"
    val local = url.startsWith("http://127.0.0.1") || url.startsWith("http://localhost")
    if (!url.startsWith("https://") && !local) return "外部接口必须使用 https://"
    return null
}
