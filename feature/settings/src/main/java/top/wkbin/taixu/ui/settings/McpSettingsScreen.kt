package top.wkbin.taixu.ui.settings
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import top.wkbin.taixu.ui.components.RuntimeButton as Button
import top.wkbin.taixu.ui.components.RuntimeCircularProgressIndicator as CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import top.wkbin.taixu.ui.components.RuntimeIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import top.wkbin.taixu.ui.components.RuntimeRadioButton as RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import top.wkbin.taixu.ui.components.RuntimeSwitch as Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import top.wkbin.taixu.ui.settings.LocalizedText as Text
import top.wkbin.taixu.ui.components.RuntimeTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.wkbin.taixu.core.model.McpConnectionState
import top.wkbin.taixu.core.model.McpAuthMode
import top.wkbin.taixu.core.model.McpAuthState
import top.wkbin.taixu.core.model.McpServerConfig
import top.wkbin.taixu.core.model.McpTransportType
import top.wkbin.taixu.harness.mcp.server.AgentMcpAccess
import top.wkbin.taixu.ui.components.RuntimeCard
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeTopBar
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun McpSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel? = previewOrNull { koinViewModel() },
    catalogViewModel: McpCatalogViewModel? = previewOrNull { koinViewModel() },
) {
    val servers by viewModel?.mcpServers?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val toggleOverrides by viewModel?.mcpToggleOverrides?.collectAsStateWithLifecycle() ?: previewStateOf(emptyMap())
    val connectionStates by viewModel?.mcpConnectionStates?.collectAsStateWithLifecycle() ?: previewStateOf(emptyMap())
    val mcpAuthStates by viewModel?.mcpAuthStates?.collectAsStateWithLifecycle() ?: previewStateOf(emptyMap())
    val browserGates by viewModel?.browserGates?.collectAsStateWithLifecycle() ?: previewStateOf(BrowserGateState())
    // 未安装的按需能力：id → 待安装组件名。用于禁用开关并给出"先去安装"的直达提示。
    val mcpInstallBlocked by catalogViewModel?.installBlocked?.collectAsStateWithLifecycle() ?: previewStateOf(emptyMap())
    val mcpInstallNotice by catalogViewModel?.installNotice?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val screenScope = rememberCoroutineScope()
    val screenContext = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showLocalDiscoveryDialog by remember { mutableStateOf(false) }
    var viewingDetailServer by remember { mutableStateOf<McpServerConfig?>(null) }
    // 待确认删除的服务 id（破坏性操作二次确认）；McpServerConfig 非 Parcelable，仅保存 id
    var pendingDeleteServerId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val pendingDeleteServer = pendingDeleteServerId?.let { id -> servers.firstOrNull { it.id == id } }

    // 进入设置页时：对齐组件探针（决定哪些能力"还没装"）并探测一次已启用服务的连通性
    LaunchedEffect(Unit) {
        catalogViewModel?.refresh()
        viewModel?.refreshMcpConnections(probeSleeping = false)
    }

    // 未安装能力的提示只展示一次，避免每次重组都弹
    LaunchedEffect(mcpInstallNotice) {
        mcpInstallNotice?.let { message ->
            Toast.makeText(screenContext, message, Toast.LENGTH_LONG).show()
            catalogViewModel?.consumeInstallNotice()
        }
    }

    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            RuntimeTopBar(
                title = androidx.compose.ui.res.stringResource(top.wkbin.taixu.feature.settings.R.string.personal_mcp_title),
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(top.wkbin.taixu.ui.components.runtimeScrollViewport(padding)),
            contentPadding = top.wkbin.taixu.ui.components.runtimeScrollContentPadding(padding, top.wkbin.taixu.ui.components.runtimeContentPadding(horizontal = 16.dp, top = 16.dp, bottom = 32.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                McpClientSettingsSection()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Dịch vụ plugin MCP khả dụng (${servers.size})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { viewModel?.refreshMcpConnections() },
                            modifier = Modifier.size(30.dp),
                        ) {
                            RuntimeIcon(RuntimeIconName.Refresh, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(
                            onClick = {
                                showLocalDiscoveryDialog = true
                                viewModel?.discoverLocalMcpServers()
                            },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                RuntimeIcon(RuntimeIconName.Search, Modifier.size(14.dp), MaterialTheme.colorScheme.primary)
                                Text("Quét máy này", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        TextButton(onClick = { showAddDialog = true }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                RuntimeIcon(RuntimeIconName.Plus, Modifier.size(14.dp), MaterialTheme.colorScheme.primary)
                                Text("Thêm dịch vụ", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            val builtinServers = servers.filter { it.isBuiltin }
            val customServers = servers.filterNot { it.isBuiltin }

            if (builtinServers.isNotEmpty()) {
                item(key = "mcp_section_builtin") {
                    McpSectionHeader(
                        title = "MCP lõi hệ thống",
                        subtitle = "Tính năng hệ thống tích hợp sẵn của TaiXu, mặc định tắt; khi bật sẽ chạy xuyên suốt phiên, harness gọi được bất cứ lúc nào (không tham gia đánh thức bằng @)." +
                            "Các tính năng gắn nhãn «Cần cài đặt trước» đến từ gói thượng nguồn, vui lòng vào «Trung tâm công cụ → Kho năng lực MCP» để cài đặt theo nhu cầu",
                        count = builtinServers.size,
                    )
                }
                items(builtinServers, key = { it.id }) { server ->
                    McpServerItemCard(
                        server = server,
                        connectionState = connectionStates[server.id] ?: McpConnectionState.UNKNOWN,
                        error = viewModel?.mcpLastError(server.id),
                        togglePending = server.id in toggleOverrides,
                        requiredComponentName = mcpInstallBlocked[server.id],
                        onToggle = { enabled ->
                            if (catalogViewModel?.onToggleRequested(server.id, enabled) == true) {
                                viewModel?.toggleMcpServer(server.id, enabled)
                            }
                        },
                        onClickDetail = { viewingDetailServer = server },
                    )
                }
            }

            if (customServers.isNotEmpty()) {
                item(key = "mcp_section_custom") {
                    McpSectionHeader(
                        title = "MCP tùy chỉnh và bên thứ ba",
                        subtitle = "Dịch vụ MCP bên ngoài được nhập qua «Thêm dịch vụ», có thể đánh thức bằng @",
                        count = customServers.size,
                    )
                }
                items(customServers, key = { it.id }) { server ->
                    McpServerItemCard(
                        server = server,
                        connectionState = connectionStates[server.id] ?: McpConnectionState.UNKNOWN,
                        error = viewModel?.mcpLastError(server.id),
                        togglePending = server.id in toggleOverrides,
                        requiredComponentName = null,
                        onToggle = { enabled -> viewModel?.toggleMcpServer(server.id, enabled) },
                        onClickDetail = { viewingDetailServer = server },
                    )
                }
            }

            item(key = "mcp_section_browser_gates") {
                BrowserGatesCard(gates = browserGates, viewModel = viewModel)
            }

            item(key = "mcp_section_agent_server") {
                AgentServerCard(
                    state = (viewModel?.agentServerState?.collectAsStateWithLifecycle() ?: previewStateOf(AgentServerState())).value,
                    viewModel = viewModel,
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showAddDialog) {
        AddMcpServerDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newServer ->
                viewModel?.saveMcpServer(newServer)
                showAddDialog = false
            },
            onAddBatch = { serversList ->
                serversList.forEach { viewModel?.saveMcpServer(it) }
                showAddDialog = false
            },
        )
    }

    if (showLocalDiscoveryDialog) {
        LocalMcpDiscoveryDialog(
            state = (viewModel?.localMcpDiscovery?.collectAsStateWithLifecycle() ?: previewStateOf(LocalMcpDiscoveryState.Idle)).value,
            onDismiss = { showLocalDiscoveryDialog = false },
            onRetry = { viewModel?.discoverLocalMcpServers() },
            onAdd = { arg0 -> viewModel?.saveMcpServer(arg0) },
        )
    }

    pendingDeleteServer?.let { server ->
        RuntimeAlertDialog(
            onDismissRequest = { pendingDeleteServerId = null },
            title = { Text("Xóa dịch vụ MCP", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bạn có chắc muốn xóa «${server.name}» không? Sau khi xóa sẽ ngừng nạp năng lực công cụ của dịch vụ này cho Agent TaiXu; bật lại MCP lõi hệ thống là khôi phục được.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.deleteMcpServer(server.id)
                        pendingDeleteServerId = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Xác nhận xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteServerId = null }) { Text("Hủy") }
            },
        )
    }

    viewingDetailServer?.let { server ->
        McpServerDetailDialog(
            server = server,
            onDismiss = { viewingDetailServer = null },
            onDelete = {
                pendingDeleteServerId = server.id
                viewingDetailServer = null
            },
            onTest = {
                val res = viewModel?.testMcpServer(server) ?: Result.success(emptyList())
                viewModel?.refreshMcpConnections()
                res
            },
            authState = mcpAuthStates[server.id] ?: McpAuthState.Unsupported,
            onAuthorize = {
                screenScope.launch {
                    runCatching { viewModel?.beginMcpAuthorization(server) }
                        .onSuccess {
                            runCatching { screenContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                                .onFailure { error -> Toast.makeText(screenContext, "Khởi động ủy quyền thất bại: ${error.message}", Toast.LENGTH_SHORT).show() }
                        }
                        .onFailure { Toast.makeText(screenContext, "Khởi động ủy quyền thất bại: ${it.message}", Toast.LENGTH_SHORT).show() }
                }
            },
            onLogout = { viewModel?.logoutMcpAuthorization(server.id) },
        )
    }
}

/** 仅展示已通过 MCP 协议握手的 127.0.0.1 服务；用户确认后才写入配置。 */
@Composable
private fun LocalMcpDiscoveryDialog(
    state: LocalMcpDiscoveryState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onAdd: (McpServerConfig) -> Unit,
) {
    var addedIds by remember { mutableStateOf(emptySet<String>()) }
    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quét MCP trên máy này", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Đang kiểm tra các cổng lắng nghe trên 127.0.0.1 và xác thực bằng giao thức MCP. Chỉ hiển thị dịch vụ thật sự khả dụng, không tự động thêm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (state) {
                    LocalMcpDiscoveryState.Idle, LocalMcpDiscoveryState.Scanning -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Đang quét dịch vụ trên máy này…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    is LocalMcpDiscoveryState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    is LocalMcpDiscoveryState.Results -> {
                        if (state.servers.isEmpty()) {
                            Text(
                                "Không tìm thấy dịch vụ MCP khả dụng nào trên máy này. Hãy xác nhận dịch vụ đã khởi động và đang lắng nghe trên 127.0.0.1.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.servers.forEach { detected ->
                                RuntimeCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    contentPadding = PaddingValues(10.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                detected.serverUrl,
                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "Đã xác thực · ${detected.toolCount} công cụ",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        if (detected.server.id in addedIds) {
                                            Text("Đã thêm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    onAdd(detected.server)
                                                    addedIds = addedIds + detected.server.id
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            ) { Text("Thêm", style = MaterialTheme.typography.labelSmall) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                LocalMcpDiscoveryState.Scanning -> Unit
                else -> Button(onClick = onRetry) { Text("Quét lại") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}

/**
 * 浏览器 MCP 安全门禁卡片：内置浏览器服务的危险能力开关（最小权限，默认全关）。
 * 门禁为引擎池级/工具快照级配置，修改后需重启应用生效。
 */
@Composable
private fun BrowserGatesCard(gates: BrowserGateState, viewModel: SettingsViewModel?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "Cổng an toàn MCP trình duyệt",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Công tắc các năng lực nguy hiểm của dịch vụ trình duyệt tích hợp, mặc định tắt hết; sửa đổi cần khởi động lại ứng dụng mới có hiệu lực",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    RuntimeCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BrowserGateRow(
                title = "Kết nối từ xa",
                description = "Cho phép thiết bị mạng LAN kết nối dịch vụ MCP của trình duyệt (ràng buộc 0.0.0.0); khi tắt chỉ cho phép loopback nội bộ, vẫn bắt buộc xác thực Bearer",
                checked = gates.allowRemoteConnect,
                onCheckedChange = { arg0 -> viewModel?.setBrowserAllowRemoteConnect(arg0) },
            )
            BrowserGateRow(
                title = "Thực thi JS trên trang",
                description = "Cho phép Agent thực thi JavaScript tùy ý trong ngữ cảnh trang (browser.evaluate)",
                checked = gates.allowEvalJs,
                onCheckedChange = { arg0 -> viewModel?.setBrowserAllowEvalJs(arg0) },
            )
            BrowserGateRow(
                title = "Hook dạng inject",
                description = "Cho phép script inject chặn/sửa request mạng và hàm JS của trang (log/block/redirect/mock)",
                checked = gates.allowHooks,
                onCheckedChange = { arg0 -> viewModel?.setBrowserAllowHooks(arg0) },
            )
            BrowserGateRow(
                title = "Gỡ lỗi CDP (điểm dừng)",
                description = "Cho phép attach trang qua giao thức DevTools: breakpoint thật, đọc/ghi biến khi tạm dừng, chặn request cấp Worker; khi bật sẽ lộ DevTools socket của tiến trình này (về lý thuyết app khác trên cùng thiết bị có thể kết nối), chỉ dùng trong môi trường được kiểm soát",
                checked = gates.allowCdp,
                onCheckedChange = { arg0 -> viewModel?.setBrowserAllowCdp(arg0) },
            )
            BrowserGateRow(
                title = "Bảng gỡ lỗi trang (vConsole)",
                description = "Inject bảng nổi vConsole vào mỗi trang (console / mạng / bộ nhớ) để kiểm tra khi duyệt thủ công; không liên quan đến cổng kiểm soát công cụ của Agent, có hiệu lực với tab mới sau khi khởi động lại",
                checked = gates.allowVConsole,
                onCheckedChange = { arg0 -> viewModel?.setBrowserAllowVConsole(arg0) },
            )
        }
    }
}

@Composable
private fun BrowserGateRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * MCP 被控端（服务端）卡片：让外部 AI 客户端（Claude Desktop / Cursor 等）通过 MCP 控制本 App。
 *
 * 与上面的浏览器门禁不同，被控端是**独立端点**（默认 8890）且使用**持久化令牌**，
 * 配置变更由 AgentMcpBootstrap 监听偏好后即时重启，无需重启应用。
 */
@Composable
private fun AgentServerCard(state: AgentServerState, viewModel: SettingsViewModel?) {
    val context = LocalContext.current
    val lanIp = remember { detectLanIp() }
    // 配置变更后 Bootstrap 异步重启，延迟一拍再读实际监听状态，避免刚切换时显示过期状态
    var runtimeTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.enabled, state.port, state.allowRemote, state.token) {
        runtimeTick++
        delay(700)
        runtimeTick++
    }
    val running = remember(runtimeTick) { AgentMcpAccess.running }
    val boundPort = remember(runtimeTick) { AgentMcpAccess.port ?: state.port }

    var portText by remember(state.port) { mutableStateOf(state.port.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            "Phía máy chủ (bên bị điều khiển)",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Cho phép AI client bên ngoài (Claude Desktop / Cursor v.v.) điều khiển máy này qua MCP: đọc/ghi tệp không gian làm việc, thực thi lệnh sandbox, thao tác ứng dụng máy chủ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    RuntimeCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrowserGateRow(
                title = "Bật phía bị điều khiển",
                description = "Khởi động endpoint dịch vụ MCP độc lập trên máy này; tắt là ngừng lắng nghe ngay",
                checked = state.enabled,
                onCheckedChange = { arg0 -> viewModel?.setAgentServerEnabled(arg0) },
            )

            AnimatedVisibility(visible = state.enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BrowserGateRow(
                        title = "Cho phép kết nối LAN",
                        description = "Ràng buộc 0.0.0.0, thiết bị cùng mạng có thể kết nối; khi tắt chỉ cho phép loopback nội bộ, cả hai chế độ đều bắt buộc xác thực Bearer",
                        checked = state.allowRemote,
                        onCheckedChange = { arg0 -> viewModel?.setAgentServerAllowRemote(arg0) },
                    )
                    BrowserGateRow(
                        title = "Cho phép ghi và thực thi",
                        description = "Mở các thao tác ghi của write/edit/base/process/download và host; khi tắt chỉ cung cấp read và các action chỉ đọc của host/memory",
                        checked = state.allowWriteTools,
                        onCheckedChange = { arg0 -> viewModel?.setAgentServerAllowWriteTools(arg0) },
                    )

                    // 端口
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { input ->
                            portText = input.filter { it.isDigit() }.take(5)
                            portText.toIntOrNull()?.let({ arg0 -> viewModel?.setAgentServerPort(arg0) })
                        },
                        label = { Text("Cổng lắng nghe") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        supportingText = { Text("Mặc định 8890; khi bị chiếm sẽ tự động dời sang 10 cổng lân cận", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 运行状态与连接地址
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (running) successStatusColor() else MaterialTheme.colorScheme.outline),
                            )
                            Text(
                                if (running) "Đang chạy · Lắng nghe ${boundPort}" else "Đã dừng",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        val loopbackUrl = "http://127.0.0.1:$boundPort/mcp"
                        val lanUrl = lanIp?.let { "http://$it:$boundPort/mcp" }
                        CopyableLine(label = "Địa chỉ máy này", value = loopbackUrl, context = context)
                        if (state.allowRemote && lanUrl != null) {
                            CopyableLine(label = "Địa chỉ mạng LAN", value = lanUrl, context = context)
                        }
                    }

                    // 持久化令牌
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Token truy cập (Bearer)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = state.token.ifBlank { "Tự động tạo sau khi bật" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("mcp_agent_token", state.token))
                                        Toast.makeText(context, "Đã sao chép token", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = state.token.isNotBlank(),
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    RuntimeIcon(RuntimeIconName.Copy, Modifier.size(14.dp), MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Token được lưu bền vững, client bên ngoài có thể tái sử dụng lâu dài; sau khi đặt lại cần đồng bộ cập nhật cấu hình client",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { viewModel?.resetAgentServerToken() }) {
                                Text("Đặt lại token", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Text(
                        "Ví dụ cấu hình client bên ngoài: điền địa chỉ trên vào cấu hình MCP, và gửi kèm Authorization: Bearer <token> trong header request.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        "Sau khi bật sẽ hiển thị thông báo thường trú để giữ tiến trình nền, tránh bị hệ thống ngắt kết nối khi App chạy nền.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 一行「标签 + 等宽值 + 复制按钮」，用于展示连接地址。 */
@Composable
private fun CopyableLine(label: String, value: String, context: Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("mcp_agent_url", value))
                Toast.makeText(context, "Đã sao chép địa chỉ", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(24.dp),
        ) {
            RuntimeIcon(RuntimeIconName.Copy, Modifier.size(14.dp), MaterialTheme.colorScheme.outline)
        }
    }
}

/** 探测本机局域网 IPv4（用于展示可被外部设备访问的连接地址）；失败返回 null。 */
private fun detectLanIp(): String? = try {
    NetworkInterface.getNetworkInterfaces()
        ?.asSequence()
        ?.filter { !it.isLoopback && it.isUp }
        ?.flatMap { it.inetAddresses.asSequence() }
        ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
        ?.hostAddress
        ?.takeIf { it.isNotBlank() && it != "127.0.0.1" }
} catch (_: Exception) {
    null
}

/**
 * MCP 分栏标题：分组展示「系统核心 MCP（内置）」与「自定义与三方 MCP」。
 */
/**
 * 新增自定义 MCP 服务弹窗
 */
@Composable
private fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onAdd: (McpServerConfig) -> Unit,
    onAddBatch: (List<McpServerConfig>) -> Unit = { list -> list.forEach(onAdd) },
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    // 表单状态
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var transport by remember { mutableStateOf(McpTransportType.STDIO) }
    var command by remember { mutableStateOf("npx") }
    var argsStr by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("http://127.0.0.1:8000/sse") }
    var oauthEnabled by remember { mutableStateOf(false) }
    var oauthClientId by remember { mutableStateOf("") }
    var oauthAuthorizationEndpoint by remember { mutableStateOf("") }
    var oauthTokenEndpoint by remember { mutableStateOf("") }
    var oauthScope by remember { mutableStateOf("") }
    var oauthResource by remember { mutableStateOf("") }

    // JSON 模式状态
    var jsonText by remember {
        mutableStateOf(
            """
            {
              "mcpServers": {
                "sqlite": {
                  "command": "uvx",
                  "args": ["mcp-server-sqlite", "--db-path", "/opt/taixu/data/sqlite.db"]
                }
              }
            }
            """.trimIndent()
        )
    }

    val parsedServers = remember(jsonText) { parseMcpJson(jsonText) }

    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Thêm dịch vụ MCP", fontWeight = FontWeight.Bold)
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📝 Chế độ biểu mẫu", style = MaterialTheme.typography.labelMedium) },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📋 Nhập JSON", style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (selectedTab == 0) {
                    // 📝 表单模式
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên dịch vụ") },
                        placeholder = { Text("VD: sqlite / github / fetch") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Mô tả (tùy chọn)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Loại giao thức truyền tải",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val isStdio = transport == McpTransportType.STDIO
                            val isSse = transport == McpTransportType.SSE

                            // Stdio 选项卡
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { transport = McpTransportType.STDIO },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isStdio) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        "Stdio",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isStdio) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isStdio) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "Tiến trình sandbox",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isStdio) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }

                            // SSE 选项卡
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { transport = McpTransportType.SSE },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSse) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        "SSE",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSse) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSse) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        "HTTP từ xa",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isSse) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                        }
                    }

                    if (transport == McpTransportType.STDIO) {
                        OutlinedTextField(
                            value = command,
                            onValueChange = { command = it },
                            label = { Text("Lệnh thực thi (VD: npx / uvx / python3)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = argsStr,
                            onValueChange = { argsStr = it },
                            label = { Text("Tham số (cách nhau bằng dấu cách)") },
                            placeholder = { Text("-y @modelcontextprotocol/server-sqlite") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("URL endpoint HTTP / SSE") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("OAuth 2.0 + PKCE", style = MaterialTheme.typography.labelMedium)
                                Text("Chế độ mã ủy quyền; token được lưu mã hóa và không bị xuất ra ngoài", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = oauthEnabled, onCheckedChange = { oauthEnabled = it })
                        }
                        if (oauthEnabled) {
                            OutlinedTextField(oauthClientId, { oauthClientId = it }, label = { Text("OAuth client ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(oauthAuthorizationEndpoint, { oauthAuthorizationEndpoint = it }, label = { Text("Authorization endpoint") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(oauthTokenEndpoint, { oauthTokenEndpoint = it }, label = { Text("Token endpoint") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(oauthScope, { oauthScope = it }, label = { Text("Scope (tùy chọn)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(oauthResource, { oauthResource = it }, label = { Text("Resource (tùy chọn)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    // 📋 JSON 模式
                    Text(
                        "Dán trực tiếp cấu hình chuẩn của Claude Desktop / Cursor:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        label = { Text("Cấu hình MCP JSON") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        ),
                    )

                    if (parsedServers.isSuccess) {
                        val list = parsedServers.getOrThrow()
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "✅ Đã phân tích thành công ${list.size} dịch vụ: " + list.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    } else {
                        val errMsg = parsedServers.exceptionOrNull()?.message ?: "Lỗi phân tích cú pháp JSON"
                        Text(
                            "❌ $errMsg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        val id = "custom_" + System.currentTimeMillis()
                        val argsList = argsStr.trim().split(" ").filter { it.isNotBlank() }
                        onAdd(
                            McpServerConfig(
                                id = id,
                                name = name.trim(),
                                description = description.trim(),
                                transportType = transport,
                                command = command.trim(),
                                args = argsList,
                                serverUrl = serverUrl.trim(),
                                authMode = if (oauthEnabled) McpAuthMode.OAUTH else McpAuthMode.NONE,
                                oauthClientId = oauthClientId.trim(),
                                oauthAuthorizationEndpoint = oauthAuthorizationEndpoint.trim(),
                                oauthTokenEndpoint = oauthTokenEndpoint.trim(),
                                oauthScope = oauthScope.trim(),
                                oauthResource = oauthResource.trim(),
                                isEnabled = true,
                                isBuiltin = false,
                            )
                        )
                    },
                    enabled = name.isNotBlank() && (transport != McpTransportType.STDIO || command.isNotBlank()) &&
                        (!oauthEnabled || (oauthClientId.isNotBlank() && oauthAuthorizationEndpoint.isNotBlank() && oauthTokenEndpoint.isNotBlank())),
                ) {
                    Text("Thêm")
                }
            } else {
                Button(
                    onClick = {
                        val list = parsedServers.getOrNull()
                        if (!list.isNullOrEmpty()) {
                            onAddBatch(list)
                        }
                    },
                    enabled = parsedServers.isSuccess && parsedServers.getOrNull()?.isNotEmpty() == true,
                ) {
                    Text("Nhập (${parsedServers.getOrNull()?.size ?: 0} dịch vụ)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

/** 解析用户粘贴的各类 MCP JSON 格式 */
private fun parseMcpJson(rawJson: String): Result<List<McpServerConfig>> = runCatching {
    val trimmed = rawJson.trim()
    if (trimmed.isBlank()) error("Vui lòng nhập nội dung JSON")
    val element = Json.parseToJsonElement(trimmed)
    val list = mutableListOf<McpServerConfig>()
    val rootObj = element.jsonObject

    if (rootObj.containsKey("mcpServers")) {
        val serversObj = rootObj["mcpServers"]?.jsonObject ?: error("mcpServers không phải là đối tượng JSON hợp lệ")
        for ((serverName, serverVal) in serversObj) {
            val sObj = serverVal.jsonObject
            val url = sObj["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val command = sObj["command"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val args = sObj["args"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
            val env = sObj["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() }.orEmpty()
            val desc = sObj["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val authMode = if (sObj["authMode"]?.jsonPrimitive?.contentOrNull.equals("OAUTH", ignoreCase = true)) McpAuthMode.OAUTH else McpAuthMode.NONE
            val oauthClientId = sObj["oauthClientId"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val oauthAuthorizationEndpoint = sObj["oauthAuthorizationEndpoint"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val oauthTokenEndpoint = sObj["oauthTokenEndpoint"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val oauthScope = sObj["oauthScope"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val oauthResource = sObj["oauthResource"]?.jsonPrimitive?.contentOrNull.orEmpty()

            val isSse = url.isNotBlank() || (command.isBlank() && sObj.containsKey("url"))
            list.add(
                McpServerConfig(
                    id = "custom_${serverName.lowercase().replace(Regex("[^a-z0-9_]"), "_")}_${System.currentTimeMillis()}",
                    name = serverName,
                    description = desc.ifBlank { if (isSse) "Dịch vụ HTTP từ xa: $url" else "Dịch vụ Stdio cục bộ: $command ${args.joinToString(" ")}" },
                    transportType = if (isSse) McpTransportType.SSE else McpTransportType.STDIO,
                    command = command,
                    args = args,
                    env = env,
                    serverUrl = url,
                    authMode = authMode,
                    oauthClientId = oauthClientId,
                    oauthAuthorizationEndpoint = oauthAuthorizationEndpoint,
                    oauthTokenEndpoint = oauthTokenEndpoint,
                    oauthScope = oauthScope,
                    oauthResource = oauthResource,
                    isEnabled = true,
                    isBuiltin = false,
                )
            )
        }
    } else {
        // 单个对象
        val name = rootObj["name"]?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "custom_mcp" }
        val url = rootObj["url"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val command = rootObj["command"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val args = rootObj["args"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
        val env = rootObj["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() }.orEmpty()
        val desc = rootObj["description"]?.jsonPrimitive?.contentOrNull.orEmpty()

        val isSse = url.isNotBlank() || (command.isBlank() && rootObj.containsKey("url"))
        if (command.isBlank() && url.isBlank()) error("Cấu hình phải chứa trường command hoặc url")
        list.add(
            McpServerConfig(
                id = "custom_${name.lowercase().replace(Regex("[^a-z0-9_]"), "_")}_${System.currentTimeMillis()}",
                name = name,
                description = desc.ifBlank { if (isSse) "Dịch vụ HTTP từ xa: $url" else "Dịch vụ Stdio cục bộ: $command ${args.joinToString(" ")}" },
                transportType = if (isSse) McpTransportType.SSE else McpTransportType.STDIO,
                command = command,
                args = args,
                env = env,
                serverUrl = url,
                isEnabled = true,
                isBuiltin = false,
            )
        )
    }
    if (list.isEmpty()) error("Không tìm thấy cấu hình dịch vụ MCP hợp lệ")
    list
}
