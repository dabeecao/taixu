package top.wkbin.taixu.ui.settings
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeCircularProgressIndicator as CircularProgressIndicator
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import top.wkbin.taixu.ui.components.RuntimeButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.HorizontalDivider
import top.wkbin.taixu.ui.components.RuntimeIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import top.wkbin.taixu.ui.components.RuntimeSlider as Slider
import androidx.compose.material3.Surface
import top.wkbin.taixu.ui.components.RuntimeSwitch as Switch
import androidx.compose.material3.SwitchDefaults
import top.wkbin.taixu.ui.settings.LocalizedText as Text
import top.wkbin.taixu.ui.components.RuntimeTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.wkbin.taixu.core.model.AgentPlugin
import top.wkbin.taixu.harness.ContextWindowPolicy
import top.wkbin.taixu.core.database.AiModelEntity
import top.wkbin.taixu.core.model.AgentSkill
import top.wkbin.taixu.core.model.AgentSubagent
import top.wkbin.taixu.core.model.AgentDepartments
import top.wkbin.taixu.ui.components.RuntimeCard
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeTopBar
import top.wkbin.taixu.ui.components.SectionHeader
import kotlinx.coroutines.delay
import top.wkbin.taixu.core.model.skill.ClawHubMarketItem
import top.wkbin.taixu.ui.settings.skill.SkillSecurityAuditDialog

enum class AgentSettingsCategory { EXECUTION, SUBAGENTS, SKILLS }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AgentSettingsScreen(
    onBack: () -> Unit,
    category: AgentSettingsCategory = AgentSettingsCategory.EXECUTION,
    viewModel: SettingsViewModel? = previewOrNull { koinViewModel() },
) {
    val thinkingExpanded by viewModel?.thinkingExpanded?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val translationModelStatus by viewModel?.translationModelStatus?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.common.translation.TranslationModelStatus.NotDownloaded)
    val thinkingAutoTranslate by viewModel?.thinkingAutoTranslate?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val chatRoundCollapse by viewModel?.chatRoundCollapse?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val customSystemPromptEnabled by viewModel?.customSystemPromptEnabled?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val customSystemPrompt by viewModel?.customSystemPrompt?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val agentCharName by viewModel?.agentCharName?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val agentUserName by viewModel?.agentUserName?.collectAsStateWithLifecycle() ?: previewStateOf("")
    val compactionEnabled by viewModel?.contextCompactionEnabled?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val maxToolRounds by viewModel?.maxToolRounds?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val roundLimitAutoContinuations by viewModel?.roundLimitAutoContinuations?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val autoWorkspaceCwd by viewModel?.autoWorkspaceCwd?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val commandOutputCompressionEnabled by viewModel?.commandOutputCompressionEnabled?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val baseCommandTimeoutSeconds by viewModel?.baseCommandTimeoutSeconds?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val approvalMode by viewModel?.approvalMode?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.model.ApprovalMode.ASSISTED)
    val runMode by viewModel?.runMode?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.model.RunMode.BUILD)
    val runningSendMode by viewModel?.runningSendMode?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.model.RunningSendMode.QUEUE)
    val maxToolsPerRound by viewModel?.maxToolsPerRound?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val maxConsecutiveFailures by viewModel?.maxConsecutiveFailures?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val contextBudgetTokens by viewModel?.contextBudgetTokens?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val effectiveContextBudget by viewModel?.effectiveContextBudget?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val activeModelDeclaredTokens by viewModel?.activeModelDeclaredTokens?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val contextFoldingRatioPercent by viewModel?.contextFoldingRatioPercent?.collectAsStateWithLifecycle() ?: previewStateOf(0)
    val skills by viewModel?.allSkills?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val subagents by viewModel?.allSubagents?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val autoSubagentDelegation by viewModel?.autoSubagentDelegationEnabled?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val models by viewModel?.models?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val plugins by viewModel?.allPlugins?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val skillArchiveMessage by viewModel?.skillArchiveMessage?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val skillArchiveMessageIsError by viewModel?.skillArchiveMessageIsError?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val skillEvolutionSuggestions by viewModel?.skillEvolutionSuggestions?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val marketSkills by viewModel?.clawHubMarketSkills?.collectAsStateWithLifecycle() ?: previewStateOf(emptyList())
    val isMarketLoading by viewModel?.isMarketLoading?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val isMarketOfflinePreset by viewModel?.isMarketOfflinePreset?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val pendingSkillInspection by viewModel?.pendingSkillInspection?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val preparingSkillId by viewModel?.preparingSkillId?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val isCommittingInstallation by viewModel?.isCommittingInstallation?.collectAsStateWithLifecycle() ?: previewStateOf(false)

    var showAddSkillDialog by remember { mutableStateOf(false) }
    var viewingSkillPrompt by remember { mutableStateOf<AgentSkill?>(null) }
    var editingSubagent by remember { mutableStateOf<AgentSubagent?>(null) }
    var showSubagentDialog by remember { mutableStateOf(false) }
    var deletingSubagent by remember { mutableStateOf<AgentSubagent?>(null) }
    var deletingSkill by remember { mutableStateOf<AgentSkill?>(null) }
    var subagentQuery by rememberSaveable { mutableStateOf("") }
    var skillQuery by rememberSaveable { mutableStateOf("") }
    var expandedDepartmentIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    val visibleSubagentGroups = remember(subagents, subagentQuery) {
        val query = subagentQuery.trim().lowercase()
        subagents.asSequence()
            .filter { profile ->
                query.isBlank() || profile.name.lowercase().contains(query) ||
                    profile.id.lowercase().contains(query) || profile.description.lowercase().contains(query)
            }
            .groupBy { it.departmentId }
            .entries
            .sortedBy { AgentDepartments.find(it.key).sortOrder }
    }
    val visibleSkills = remember(skills, skillQuery) {
        val query = skillQuery.trim().lowercase()
        if (query.isBlank()) {
            skills
        } else {
            skills.filter { skill ->
                skill.name.lowercase().contains(query) || skill.id.lowercase().contains(query) ||
                    skill.description.lowercase().contains(query) || skill.category.lowercase().contains(query)
            }
        }
    }
    val skillArchivePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.size == 1) {
            viewModel?.inspectLocalSkillZip(uris.first())
        } else if (uris.isNotEmpty()) {
            viewModel?.importSkillArchives(uris)
        }
    }
    val skillDirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) viewModel?.importSkillsFromTree(uri) }

    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            RuntimeTopBar(
                title = when (category) {
                    AgentSettingsCategory.EXECUTION -> "Agent thực thi & ngữ cảnh"
                    AgentSettingsCategory.SUBAGENTS -> "Vai trò subagent"
                    AgentSettingsCategory.SKILLS -> "Skills & plugin"
                },
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
            if (category == AgentSettingsCategory.EXECUTION) {
            // ---- 模块 1：模型思考与交互表现 ----
            item {
                SectionHeader(
                    title = "Luồng suy nghĩ & hiệu năng thực thi",
                    subtitle = "Điều khiển cách hiển thị quá trình suy nghĩ của các mô hình suy luận như DeepSeek và giới hạn thực thi công cụ",
                )
            }
            item {
                AgentBlockTitle("Hiển thị & dịch suy nghĩ")
            }
            item {
                AgentSettingsGroup {
                    AgentToggleRow(
                        icon = RuntimeIconName.Brain,
                        title = "Mặc định mở rộng quá trình suy nghĩ",
                        subtitle = if (thinkingExpanded) "Quá trình suy nghĩ mới trong màn hình trò chuyện sẽ được mở rộng mặc định" else "Quá trình suy nghĩ (kể cả nội dung đang sinh) mặc định thu gọn, chạm để mở rộng xem",
                        checked = thinkingExpanded,
                        onCheckedChange = { arg0 -> viewModel?.setThinkingExpanded(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AgentToggleRow(
                        icon = RuntimeIconName.Globe,
                        title = "Tự động dịch suy nghĩ tiếng Anh sang tiếng Trung khi mở rộng",
                        subtitle = if (thinkingAutoTranslate) "Khi mở khối suy nghĩ, nếu phát hiện tiếng Anh sẽ tự gọi mô hình offline cục bộ để dịch và hiển thị" else "Chỉ dịch khi chạm nút dịch trên khối suy nghĩ",
                        checked = thinkingAutoTranslate,
                        onCheckedChange = { arg0 -> viewModel?.setThinkingAutoTranslate(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AgentToggleRow(
                        icon = RuntimeIconName.Compress,
                        title = "Tự thu gọn suy nghĩ & quá trình công cụ ở các lượt cũ",
                        subtitle = if (chatRoundCollapse) "Ở các lượt đã kết thúc, suy nghĩ và gọi công cụ trên 2 bước sẽ được gom thành tóm tắt \"Mở rộng thêm\", chạm để xem; lượt đang chạy luôn hiển thị đầy đủ" else "Khi tắt, giữ luồng đơn tự nhiên: mọi suy nghĩ và gọi công cụ hiển thị phẳng từng dòng",
                        checked = chatRoundCollapse,
                        onCheckedChange = { arg0 -> viewModel?.setChatRoundCollapse(arg0) },
                    )
                }
            }
            item {
                AgentBlockTitle("Mô hình dịch offline cho suy nghĩ")
            }
            item {
                AgentSettingsGroup {
                    TranslationModelCard(
                        status = translationModelStatus,
                        onDownload = { viewModel?.downloadTranslationModel() },
                        onDelete = { viewModel?.deleteTranslationModel() },
                        onRetry = { viewModel?.downloadTranslationModel() },
                    )
                }
            }
            item {
                AgentBlockTitle("Ngữ cảnh thực thi")
            }
            item {
                AgentSettingsGroup {
                    AgentToggleRow(
                        icon = RuntimeIconName.FolderOpen,
                        title = "Tự chèn đường dẫn không gian làm việc liên kết",
                        subtitle = "Khi phiên trò chuyện gắn với không gian làm việc, lệnh base mặc định chạy với thư mục đó làm đường dẫn làm việc (cwd)",
                        checked = autoWorkspaceCwd,
                        onCheckedChange = { arg0 -> viewModel?.setAutoWorkspaceCwd(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AgentToggleRow(
                        icon = RuntimeIconName.Compress,
                        title = "Nén thông minh đầu ra lệnh của Agent",
                        subtitle = "Giảm tiêu hao ngữ cảnh khi Agent đọc git, tìm kiếm, log test & build; terminal và tệp gốc không bị ảnh hưởng",
                        checked = commandOutputCompressionEnabled,
                        onCheckedChange = { arg0 -> viewModel?.setCommandOutputCompressionEnabled(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ApprovalModeSelectorRow(
                        mode = approvalMode,
                        onModeChange = { arg0 -> viewModel?.setApprovalMode(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RunModeSelectorRow(
                        mode = runMode,
                        onModeChange = { arg0 -> viewModel?.setRunMode(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RunningSendModeSelectorRow(
                        mode = runningSendMode,
                        onModeChange = { arg0 -> viewModel?.setRunningSendMode(arg0) },
                    )
                }
            }
            item {
                AgentBlockTitle("Giới hạn gọi công cụ")
            }
            item {
                AgentSettingsGroup {
                    BaseCommandTimeoutSliderRow(
                        currentValue = baseCommandTimeoutSeconds,
                        onValueChange = { arg0 -> viewModel?.setBaseCommandTimeoutSeconds(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    RoundsSliderRow(
                        currentValue = maxToolRounds,
                        onValueChange = { arg0 -> viewModel?.setMaxToolRounds(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AutoContinuationSliderRow(
                        currentValue = roundLimitAutoContinuations,
                        roundsPerSegment = maxToolRounds,
                        onValueChange = { arg0 -> viewModel?.setRoundLimitAutoContinuations(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ToolsPerRoundSliderRow(
                        currentValue = maxToolsPerRound,
                        onValueChange = { arg0 -> viewModel?.setMaxToolsPerRound(arg0) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ConsecutiveFailuresSliderRow(
                        currentValue = maxConsecutiveFailures,
                        onValueChange = { arg0 -> viewModel?.setMaxConsecutiveFailures(arg0) },
                    )
                }
            }

            // ---- 模块 2：系统提示词与人设自定义 ----
            item {
                SectionHeader(
                    title = "Tùy chỉnh system prompt & nhân cách",
                    subtitle = "Tùy biến nhân cách ban đầu, mô tả môi trường và chèn biến macro động cho Agent",
                )
            }
            item {
                AgentBlockTitle("Chỉnh sửa prompt")
            }
            item {
                SystemPromptCustomCard(
                    enabled = customSystemPromptEnabled,
                    onEnabledChange = { arg0 -> viewModel?.setCustomSystemPromptEnabled(arg0) },
                    prompt = customSystemPrompt,
                    onPromptChange = { arg0 -> viewModel?.setCustomSystemPrompt(arg0) },
                    charName = agentCharName,
                    onCharNameChange = { arg0 -> viewModel?.setAgentCharName(arg0) },
                    userName = agentUserName,
                    onUserNameChange = { arg0 -> viewModel?.setAgentUserName(arg0) },
                )
            }

            // ---- 模块 2：上下文记忆与智能压缩 ----
            item {
                SectionHeader(
                    title = "Bộ nhớ ngữ cảnh & nén thông minh",
                    subtitle = "Tối ưu tiêu hao token cho tác vụ dài, tránh chạm trần cửa sổ ngữ cảnh của mô hình",
                )
            }
            item {
                AgentBlockTitle("Chiến lược nén")
            }
            item {
                AgentSettingsGroup {
                    AgentToggleRow(
                        icon = RuntimeIconName.Compress,
                        title = "Bật nén ngữ cảnh thông minh (Context Compaction)",
                        subtitle = "Tự tạo tóm tắt có cấu trúc khi lịch sử vượt ngưỡng gập token, giữ nguyên văn gần đây, trạng thái tác vụ và dấu vết tệp quan trọng",
                        checked = compactionEnabled,
                        onCheckedChange = { arg0 -> viewModel?.setContextCompactionEnabled(arg0) },
                    )
                    if (compactionEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ContextBudgetSliderRow(
                            currentValue = contextBudgetTokens,
                            declaredTokens = activeModelDeclaredTokens,
                            onValueChange = { arg0 -> viewModel?.setContextBudgetTokens(arg0) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ContextFoldingRatioSliderRow(
                            currentValue = contextFoldingRatioPercent,
                            budget = effectiveContextBudget,
                            declaredTokens = activeModelDeclaredTokens,
                            onValueChange = { arg0 -> viewModel?.setContextFoldingRatioPercent(arg0) },
                        )
                    }
                }
            }

            }

            if (category == AgentSettingsCategory.SUBAGENTS) {
            // ---- 模块 3：内置子智能体 ----
            item {
                SectionHeader(
                    title = "Agent phát triển phần mềm (${subagents.count { it.isEnabled }}/${subagents.size} đã bật)",
                    subtitle = "Agency Agents · 9 phòng ban R&D · Giấy phép MIT · Prompt đầy đủ tích hợp offline",
                )
            }
            item {
                AgentBlockTitle("Chiến lược ủy thác tự động")
            }
            item {
                AgentSettingsGroup {
                    AgentToggleRow(
                        icon = RuntimeIconName.Bot,
                        title = "Tự nhận biết và chia nhỏ tác vụ",
                        subtitle = if (autoSubagentDelegation) {
                            "Tác vụ phức tạp, có thể song song sẽ do agent chính tự quyết định có ủy thác hay không"
                        } else {
                            "Chỉ ủy thác khi bạn yêu cầu rõ ràng chạy song song hoặc phối hợp subagent"
                        },
                        checked = autoSubagentDelegation,
                        onCheckedChange = { arg0 -> viewModel?.setAutoSubagentDelegationEnabled(arg0) },
                    )
                }
            }
            item {
                AgentBlockTitle("Danh sách vai trò")
            }
            item {
                OutlinedTextField(
                    value = subagentQuery,
                    onValueChange = { subagentQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm kiếm Agent") },
                    placeholder = { Text("Tên, định danh vai trò hoặc trách nhiệm") },
                    leadingIcon = { RuntimeIcon(RuntimeIconName.Search, Modifier.size(18.dp)) },
                    singleLine = true,
                )
            }
            item {
                Button(
                    onClick = {
                        editingSubagent = null
                        showSubagentDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RuntimeIcon(RuntimeIconName.Plus, Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Thêm vai trò subagent", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
            visibleSubagentGroups.forEach { (departmentId, profiles) ->
                val expanded = subagentQuery.isNotBlank() || departmentId in expandedDepartmentIds
                item(key = "department:$departmentId") {
                    AgentDepartmentCard(
                        departmentId = departmentId,
                        enabledCount = profiles.count { it.isEnabled },
                        agentCount = profiles.size,
                        expanded = expanded,
                        onClick = {
                            if (subagentQuery.isBlank()) {
                                expandedDepartmentIds = if (expanded) {
                                    expandedDepartmentIds - departmentId
                                } else {
                                    expandedDepartmentIds + departmentId
                                }
                            }
                        },
                    )
                }
                if (expanded) {
                    items(profiles, key = { "subagent:${it.id}" }) { profile ->
                        SubagentProfileCard(
                            profile = profile,
                            modelLabel = profile.defaultModelId?.let { modelId ->
                                models.firstOrNull { it.id == modelId }?.let { model ->
                                    "${model.name} · ${profile.defaultModelVariant ?: model.model.substringBefore(',').trim()}"
                                } ?: "Hồ sơ mô hình đã bị xóa"
                            } ?: "Theo agent chính",
                            onToggle = { enabled -> viewModel?.toggleSubagent(profile.id, enabled) },
                            onEdit = {
                                editingSubagent = profile
                                showSubagentDialog = true
                            },
                            onDelete = { deletingSubagent = profile },
                        )
                    }
                }
            }

            }

            if (category == AgentSettingsCategory.SKILLS) {
            // ---- 模块 4：Skill 专精技能库 ----
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionHeader(
                        title = "Thư viện skill chuyên sâu (${skills.count { it.isEnabled }}/${skills.size} đã bật)",
                        subtitle = "Chèn quy chuẩn chuyên môn và hướng dẫn thao tác vào system prompt của Agent",
                    )
                }
            }
            item {
                AgentBlockTitle("Tiến hóa skill")
            }
            item {
                AgentSettingsGroup {
                    AgentToggleRow(
                        icon = RuntimeIconName.Sparkles,
                        title = "Đề xuất lưu trữ hoặc sửa skill sau trò chuyện",
                        subtitle = if (skillEvolutionSuggestions) {
                            "Sau khi hoàn thành một lượt việc có gọi công cụ, có thể đề xuất tạo skill mới hoặc sửa skill tùy chỉnh hiện có"
                        } else {
                            "Đã tắt: không còn hiện thẻ đề xuất tiến hóa skill sau trò chuyện"
                        },
                        checked = skillEvolutionSuggestions,
                        onCheckedChange = { arg0 -> viewModel?.setSkillEvolutionSuggestions(arg0) },
                    )
                }
            }
            item {
                AgentBlockTitle("Quản lý skill")
            }
            item {
                OutlinedButton(onClick = { skillArchivePicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RuntimeIcon(RuntimeIconName.FolderDownload, Modifier.size(16.dp))
                        Text("Nhập Skill từ ZIP (hỗ trợ chọn nhiều)")
                    }
                }
            }
            item {
                OutlinedButton(onClick = { viewModel?.scanSkillDirectories() }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RuntimeIcon(RuntimeIconName.FolderOpen, Modifier.size(16.dp))
                        Text("Quét thư mục Skill để nhập hàng loạt")
                    }
                }
                Text(
                    text = "Có thể copy nguyên thư mục skills của các công cụ như rikkahub, aicode vào attachments/skills hoặc thư mục skills của không gian làm việc, khởi động lại app sẽ tự nhập hàng loạt; copy khi app đang chạy thì chạm nút trên để quét ngay, skill đã nhập sẽ không bị đăng ký trùng",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
            item {
                OutlinedButton(onClick = { skillDirPicker.launch(null) }, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RuntimeIcon(RuntimeIconName.Search, Modifier.size(16.dp))
                        Text("Nhập từ thư mục tự chọn (bất kỳ đâu)")
                    }
                }
                Text(
                    text = "Chọn bất kỳ thư mục nào trong trình quản lý tệp (hỗ trợ lồng nhau ở mọi độ sâu, kể cả thư mục con Skill trong thư mục Skill), tự tìm mọi thư mục skill chứa SKILL.md và nhập",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
            item {
                Button(
                    onClick = { showAddSkillDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RuntimeIcon(RuntimeIconName.Sparkles, Modifier.size(16.dp), MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Thêm Skill tùy chỉnh", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = skillQuery,
                    onValueChange = { skillQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tìm kiếm Skill") },
                    placeholder = { Text("Tên, định danh, phân loại hoặc mô tả") },
                    leadingIcon = { RuntimeIcon(RuntimeIconName.Search, Modifier.size(18.dp)) },
                    singleLine = true,
                )
            }

            items(visibleSkills, key = { it.id }) { skill ->
                SkillCard(
                    skill = skill,
                    onToggle = { enabled -> viewModel?.toggleSkill(skill.id, enabled) },
                    onViewPrompt = { viewingSkillPrompt = skill },
                    onDelete = if (!skill.isBuiltin) { { deletingSkill = skill } } else null,
                )
            }
            if (visibleSkills.isEmpty()) {
                item {
                    Text(
                        text = "Không có skill nào khớp với \"${skillQuery.trim()}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ---- 模块 4.5：ClawHub 技能生态市场 ----
            item {
                SectionHeader(
                    title = "Chợ sinh thái skill ClawHub",
                    subtitle = if (isMarketOfflinePreset) {
                        "Chế độ tuyển chọn offline: dưới đây là các gói sinh thái tuyển chọn tích hợp sẵn (chợ từ xa chưa kết nối); trước khi cài vẫn do engine cục bộ kiểm tra an toàn tĩnh và tương thích"
                    } else {
                        "Các gói skill sinh thái chuẩn cộng đồng được tuyển chọn, trước khi tải do engine cục bộ kiểm tra an toàn tĩnh và tương thích"
                    },
                )
            }

            if (isMarketLoading && marketSkills.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                items(marketSkills, key = { "clawhub_" + it.id }) { marketItem ->
                    ClawHubMarketSkillCard(
                        item = marketItem,
                        isPreparing = preparingSkillId == marketItem.id,
                        onInstall = { viewModel?.prepareInstallMarketSkill(marketItem.id) },
                    )
                }
            }

            // ---- 模块 5：Plugin 插件生态管理 ----
            item {
                SectionHeader(
                    title = "Quản lý sinh thái Plugin (${plugins.count { it.isEnabled }}/${plugins.size} đang chạy)",
                    subtitle = "Phát hiện runtime sandbox, chặn thao tác nguy hiểm và khả năng mở rộng",
                )
            }

            item {
                AgentBlockTitle("Plugin đã cài")
            }
            items(plugins, key = { it.id }) { plugin ->
                PluginCard(
                    plugin = plugin,
                    onToggle = { enabled -> viewModel?.togglePlugin(plugin.id, enabled) },
                )
            }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    pendingSkillInspection?.let { inspection ->
        SkillSecurityAuditDialog(
            inspection = inspection,
            isCommitting = isCommittingInstallation,
            onConfirmInstall = { viewModel?.confirmSkillInstallation() },
            onDismiss = { viewModel?.dismissSkillInspection() },
        )
    }

    skillArchiveMessage?.let { message ->
        RuntimeAlertDialog(
            onDismissRequest = { viewModel?.clearSkillArchiveMessage() },
            title = { Text(if (!skillArchiveMessageIsError) "Nhập Skill thành công" else "Nhập Skill thất bại", fontWeight = FontWeight.Bold) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { viewModel?.clearSkillArchiveMessage() }) { Text("Đã hiểu") } },
        )
    }

    // 查看 Skill 完整提示词弹窗
    viewingSkillPrompt?.let { skill ->
        RuntimeAlertDialog(
            onDismissRequest = { viewingSkillPrompt = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(skill.name, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(skill.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Prompt hướng dẫn đã chèn vào hệ thống (System Prompt):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            skill.systemPrompt,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingSkillPrompt = null }) {
                    Text("Đóng")
                }
            },
        )
    }

    // 新增自定义 Skill 弹窗
    if (showAddSkillDialog) {
        AddSkillDialog(
            onDismiss = { showAddSkillDialog = false },
            onConfirm = { name, desc, prompt, cmd ->
                viewModel?.addCustomSkill(name, desc, prompt, cmd)
                showAddSkillDialog = false
            },
        )
    }

    if (showSubagentDialog) {
        SubagentEditorDialog(
            profile = editingSubagent,
            models = models,
            existingIds = subagents.mapTo(mutableSetOf()) { it.id },
            onDismiss = {
                showSubagentDialog = false
                editingSubagent = null
            },
            onConfirm = { roleId, name, description, prompt, defaultModelId, defaultModelVariant ->
                viewModel?.saveSubagent(
                    editingSubagent,
                    roleId,
                    name,
                    description,
                    prompt,
                    defaultModelId,
                    defaultModelVariant,
                )
                showSubagentDialog = false
                editingSubagent = null
            },
        )
    }

    deletingSkill?.let { skill ->
        RuntimeAlertDialog(
            onDismissRequest = { deletingSkill = null },
            title = { Text("Xóa Skill", fontWeight = FontWeight.Bold) },
            text = { Text("Xác nhận xóa “${skill.name}”? Thư mục tài nguyên script của skill cũng sẽ bị dọn sạch, không thể khôi phục sau khi xóa.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel?.deleteCustomSkill(skill.id)
                        deletingSkill = null
                    },
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSkill = null }) { Text("Hủy") }
            },
        )
    }

    deletingSubagent?.let { profile ->
        RuntimeAlertDialog(
            onDismissRequest = { deletingSubagent = null },
            title = { Text("Xóa subagent", fontWeight = FontWeight.Bold) },
            text = { Text("Xác nhận xóa “${profile.name}”? Sau khi xóa, vai trò này sẽ không còn tham gia ủy thác tác vụ.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel?.deleteSubagent(profile.id)
                        deletingSubagent = null
                    },
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSubagent = null }) { Text("Hủy") }
            },
        )
    }
}

@Composable
private fun RoundsSliderRow(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
) {
    var sliderVal by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Giới hạn số vòng công cụ tối đa mỗi lượt", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                "${sliderVal.toInt()} vòng",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "Ngăn mô hình rơi vào vòng lặp vô hạn ở tác vụ phức tạp; khi hết vòng sẽ chuyển sang điểm kiểm tra tự chạy tiếp bên dưới",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderVal,
            onValueChange = { sliderVal = it },
            onValueChangeFinished = { onValueChange(sliderVal.toInt()) },
            valueRange = 10f..300f,
            steps = 28, // 10, 20, 30 ... 300
        )
    }
}

@Composable
private fun AutoContinuationSliderRow(
    currentValue: Int,
    roundsPerSegment: Int,
    onValueChange: (Int) -> Unit,
) {
    var sliderVal by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }
    val continuations = sliderVal.toInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Tự chạy tiếp khi hết vòng", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                if (continuations == 0) "Tắt" else "$continuations lần",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            if (continuations == 0) {
                "Hết vòng sẽ dừng chờ bạn xác nhận; phù hợp khi muốn kiểm soát từng đoạn"
            } else {
                "Khi hết vòng, mô hình sẽ thu gọn và ghi lại tiến độ rồi tự chạy tiếp, không cần bạn bấm tiếp tục;" +
                    "Tổng ngân sách tối đa ${roundsPerSegment * (continuations + 1)} vòng"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderVal,
            onValueChange = { sliderVal = it },
            onValueChangeFinished = { onValueChange(sliderVal.toInt()) },
            valueRange = 0f..10f,
            steps = 9, // 0, 1, 2 ... 10
        )
    }
}

@Composable
private fun ContextBudgetSliderRow(
    currentValue: Int,
    declaredTokens: Int?,
    onValueChange: (Int) -> Unit,
) {
    var sliderVal by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ngân sách token ngữ cảnh tối đa", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                "${sliderVal.toInt()} tok",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            if (declaredTokens != null) {
                "Mô hình đang dùng đã có giới hạn ngữ cảnh ${declaredTokens / 1000}K (cấu hình rõ ràng hoặc tự nhận diện), thanh trượt này tạm không có hiệu lực;" +
                    "Chỉ khi không nhận diện được mô hình và chưa cấu hình contextTokens, giá trị này mới làm ngân sách dự phòng."
            } else {
                "Chưa nhận diện được giới hạn ngữ cảnh của mô hình đang dùng, giá trị này là ngân sách thực tế;" +
                    "Lịch sử phiên dài vượt ngân sách sẽ tự gập nội dung cũ."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderVal,
            onValueChange = { sliderVal = it },
            onValueChangeFinished = { onValueChange(sliderVal.toInt()) },
            valueRange = 8000f..2000000f,
            steps = 49, // 步长约 4 万 tok
        )
    }
}

/**
 * 「历史折叠线比例」滑块。
 *
 * 让历史在预算的一部分处就开始折叠，而不是等到预算减预留的硬线才动手——
 * 长会话可借此显著降低单次请求的 input token 量（省费用、降首字延迟）。
 */
@Composable
private fun ContextFoldingRatioSliderRow(
    currentValue: Int,
    budget: Int,
    declaredTokens: Int?,
    onValueChange: (Int) -> Unit,
) {
    var sliderVal by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }
    // 实时预览：按当前比例算出的折叠线，让用户直观看到「拖到多少就按多少折叠」。
    // 关键：budget 必须是**实际生效预算**（模型档案 contextTokens 优先，再钳到 MAX_CONTEXT_BUDGET），
    // 并且扣减引擎同款 system/输出/工具 schema 预留，否则设置页会高于真实折叠触发线。
    val previewSystemTokens = ContextWindowPolicy.estimateReservedPromptTokens(
        pureChat = false,
        toolDisabled = false,
    )
    val previewLimit = remember(sliderVal, budget, previewSystemTokens) {
        ContextWindowPolicy.foldingLimitFor(
            budget = budget,
            ratioPercent = sliderVal.toInt(),
            systemTokens = previewSystemTokens,
        ).coerceAtLeast(0)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Tỉ lệ ngưỡng gập lịch sử", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                "${sliderVal.toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "Lịch sử bắt đầu gập tại \"ngân sách × tỉ lệ\"; mặc định 100%, khớp với các harness phổ biến" +
                " contextWindow - reserveTokens - toolSchemaReserve - systemTokens." +
                "Giảm tỉ lệ để lịch sử vào tóm tắt sớm hơn, tiết kiệm input token; mô hình cửa sổ lớn không còn bị trần 96K." +
                "Khi đi qua Nginx/trung chuyển, hãy chỉnh client_max_body_size trên 20m nếu không có thể gặp HTTP 413.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Với cài đặt hiện tại, ngưỡng gập khoảng ${previewLimit / 1000}K tok",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
        )
        // 说明这个预览数按什么预算折算，避免用户误以为它基于「上下文预算上限」滑块。
        Text(
            if (declaredTokens != null) {
                "Tính theo cửa sổ mô hình đang có hiệu lực ${budget / 1000}K" +
                    "(cấu hình rõ ràng hoặc tự thích ứng)" +
                    (if (declaredTokens > ContextWindowPolicy.MAX_CONTEXT_BUDGET) {
                        ", engine giới hạn ở ${ContextWindowPolicy.MAX_CONTEXT_BUDGET / 1000}K"
                    } else {
                        ""
                    }) +
                    ". Đã trừ phần dự trữ cho system prompt/đầu ra/schema công cụ trong bản xem trước."
            } else {
                "Tính theo giá trị của thanh trượt \"Ngân sách token ngữ cảnh tối đa\" ở trên (hiện chưa nhận diện được giới hạn ngữ cảnh mô hình)." +
                    "Đã trừ phần dự trữ cho system prompt/đầu ra/schema công cụ trong bản xem trước."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderVal,
            onValueChange = { sliderVal = it },
            onValueChangeFinished = { onValueChange(sliderVal.toInt()) },
            // 与 SettingsDataStore.setContextFoldingRatioPercent 的 coerceIn(10, 100) 对齐
            valueRange = 10f..100f,
            steps = 8, // 步长 10%
        )
    }
}

@Composable
private fun ToolsPerRoundSliderRow(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
) {
    var sliderVal by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Số lần gọi công cụ tối đa mỗi vòng", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                "${sliderVal.toInt()}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "Ngăn mô hình bùng nổ gọi công cụ một lúc làm cạn ngữ cảnh hoặc lặp mất kiểm soát",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderVal,
            onValueChange = { sliderVal = it },
            onValueChangeFinished = { onValueChange(sliderVal.toInt()) },
            valueRange = 1f..30f,
            steps = 29,
        )
    }
}

@Composable
private fun ConsecutiveFailuresSliderRow(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
) {
    var sliderVal by remember(currentValue) { mutableFloatStateOf(currentValue.toFloat()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Ngưỡng ngắt mạch khi thất bại liên tiếp", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                "${sliderVal.toInt()} vòng",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            "Chủ động dừng khi nhiều vòng gọi công cụ liên tiếp đều thất bại, tránh quay vòng lặp vô ích",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = sliderVal,
            onValueChange = { sliderVal = it },
            onValueChangeFinished = { onValueChange(sliderVal.toInt()) },
            valueRange = 2f..30f,
            steps = 28,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillCard(
    skill: AgentSkill,
    onToggle: (Boolean) -> Unit,
    onViewPrompt: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    RuntimeCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = if (skill.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    skill.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        skill.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val cmd = skill.triggerCommand
                if (cmd != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            cmd,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Text(skill.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onViewPrompt, contentPadding = PaddingValues(0.dp)) {
                    Text("Xem prompt hướng dẫn (Prompt)", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.weight(1f))
                if (skill.isImmutable) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            "Thường trú lõi hệ thống",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } else {
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(32.dp),
                            contentDescription = "Xóa Skill",
                        ) {
                            RuntimeIcon(RuntimeIconName.Trash, Modifier.size(16.dp), MaterialTheme.colorScheme.error)
                        }
                    }
                    Switch(
                        checked = skill.isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClawHubMarketSkillCard(
    item: ClawHubMarketItem,
    isPreparing: Boolean,
    onInstall: () -> Unit,
) {
    RuntimeCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = MaterialTheme.colorScheme.outlineVariant,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        item.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        "v${item.version}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Tác giả: ${item.author} · ★ ${item.stars}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                when {
                    isPreparing -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                Text("Đang kiểm tra…", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    item.isInstalled -> {
                        OutlinedButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("Đã cài", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onInstall,
                            enabled = !isPreparing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                RuntimeIcon(name = RuntimeIconName.Shield, modifier = Modifier.size(14.dp))
                                Text("Kiểm tra & cài", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PluginCard(
    plugin: AgentPlugin,
    onToggle: (Boolean) -> Unit,
) {
    RuntimeCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        borderColor = if (plugin.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            plugin.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text("v${plugin.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "Tác giả: ${plugin.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            Text(plugin.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (plugin.permissions.isNotEmpty()) {
                val visiblePermissions = plugin.permissions.take(4)
                val hiddenCount = plugin.permissions.size - visiblePermissions.size
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Quyền:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    visiblePermissions.forEach { perm ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                perm,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 140.dp).padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (hiddenCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                "cùng ${plugin.permissions.size} quyền khác",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSkillDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, systemPrompt: String, command: String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var cmd by remember { mutableStateOf("") }

    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Skill tùy chỉnh", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên skill (vd: Chuyên gia biên dịch Rust)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả ngắn") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cmd,
                    onValueChange = { cmd = it },
                    label = { Text("Lệnh kích hoạt (tùy chọn, vd /rust)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Quy tắc system prompt (System Prompt)") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, desc, prompt, cmd) },
                enabled = name.isNotBlank() && prompt.isNotBlank(),
            ) {
                Text("Thêm & bật")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
    )
}

@Composable
private fun SubagentEditorDialog(
    profile: AgentSubagent?,
    models: List<AiModelEntity>,
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (
        roleId: String,
        name: String,
        description: String,
        systemPrompt: String,
        defaultModelId: String?,
        defaultModelVariant: String?,
    ) -> Unit,
) {
    var roleId by remember(profile?.id) { mutableStateOf(profile?.id.orEmpty()) }
    var name by remember(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var description by remember(profile?.id) { mutableStateOf(profile?.description.orEmpty()) }
    var prompt by remember(profile?.id) { mutableStateOf(profile?.systemPrompt.orEmpty()) }
    var defaultModelId by remember(profile?.id) { mutableStateOf(profile?.defaultModelId) }
    var defaultModelVariant by remember(profile?.id) { mutableStateOf(profile?.defaultModelVariant) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val normalizedId = roleId.trim().lowercase()
        .replace(Regex("[^a-z0-9_-]+"), "_")
        .trim('_')
    val duplicateId = normalizedId.isNotBlank() && normalizedId != profile?.id && normalizedId in existingIds
    val canSave = normalizedId.isNotBlank() && name.isNotBlank() && prompt.isNotBlank() && !duplicateId

    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (profile == null) "Thêm vai trò subagent" else "Sửa vai trò subagent", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = roleId,
                    onValueChange = { roleId = it },
                    label = { Text("Định danh vai trò") },
                    supportingText = {
                        Text(if (duplicateId) "Định danh vai trò này đã tồn tại" else "Chỉ hỗ trợ chữ cái, số, gạch dưới và gạch ngang")
                    },
                    isError = duplicateId,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên hiển thị") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Tóm tắt trách nhiệm") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Prompt hướng dẫn vai trò") },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mô hình mặc định", style = MaterialTheme.typography.labelMedium)
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { modelMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val selected = models.firstOrNull { it.id == defaultModelId }
                            Text(
                                selected?.let { "${it.name} · ${defaultModelVariant ?: it.model.substringBefore(',').trim()}" }
                                    ?: if (defaultModelId == null) "Theo agent chính" else "Hồ sơ mô hình đã bị xóa",
                                modifier = Modifier.weight(1f),
                            )
                            RuntimeIcon(RuntimeIconName.ChevronDown, Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Theo agent chính") },
                                onClick = {
                                    defaultModelId = null
                                    defaultModelVariant = null
                                    modelMenuExpanded = false
                                },
                            )
                            models.forEach { model ->
                                model.model.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach { variant ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(model.name)
                                                Text(
                                                    "${model.provider} · $variant",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        },
                                        onClick = {
                                            defaultModelId = model.id
                                            defaultModelVariant = variant
                                            modelMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Mô hình được chỉ định rõ khi ủy thác tác vụ vẫn được ưu tiên hơn cài đặt này",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(normalizedId, name, description, prompt, defaultModelId, defaultModelVariant)
                },
                enabled = canSave,
            ) {
                Text(if (profile == null) "Thêm & bật" else "Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemPromptCustomCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    prompt: String,
    onPromptChange: (String) -> Unit,
    charName: String,
    onCharNameChange: (String) -> Unit,
    userName: String,
    onUserNameChange: (String) -> Unit,
) {
    // 仅在首次进入时以持久化值初始化缓冲，避免 remember(prompt) 键在每次 Datastore 回写时重置输入；
    // 输入经 400ms 防抖后再写入 Datastore，避免每次按键都落盘
    var textBuffer by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(prompt) }
    androidx.compose.runtime.LaunchedEffect(textBuffer) {
        if (textBuffer != prompt) {
            delay(400)
            onPromptChange(textBuffer)
        }
    }

    // 称呼输入同样走 400ms 防抖落盘；留空即回退默认值
    var charNameBuffer by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(charName) }
    androidx.compose.runtime.LaunchedEffect(charNameBuffer) {
        if (charNameBuffer != charName) {
            delay(400)
            onCharNameChange(charNameBuffer)
        }
    }
    var userNameBuffer by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(userName) }
    androidx.compose.runtime.LaunchedEffect(userNameBuffer) {
        if (userNameBuffer != userName) {
            delay(400)
            onUserNameChange(userNameBuffer)
        }
    }

    var showDefaultPromptDialog by remember { mutableStateOf(false) }
    var defaultPromptAsset by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val androidContext = androidx.compose.ui.platform.LocalContext.current
    if (showDefaultPromptDialog) {
        androidx.compose.runtime.LaunchedEffect(showDefaultPromptDialog) {
            if (defaultPromptAsset.isBlank()) {
                defaultPromptAsset = runCatching {
                    androidContext.assets.open("prompts/system/core.md").bufferedReader().use { it.readText() }
                }.getOrDefault("(Không tải được system prompt mặc định)")
            }
        }
        RuntimeAlertDialog(
            onDismissRequest = { showDefaultPromptDialog = false },
            title = { Text("System prompt mặc định (core.md)", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        defaultPromptAsset,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultPromptDialog = false }) { Text("Đã hiểu") }
            },
        )
    }

    val defaultPromptTemplate = """
You are a helpful and expert AI assistant called {{char}}, based on model {{model_name}}.

## Persona
- You refer to yourself as {{char}} in conversations.
- You address the user as {{user}}.

## System & Device Context
- Time: {{cur_datetime}}
- Locale: {{locale}}
- Timezone: {{timezone}}
- Device: {{device_info}}
- System: {{system_version}}
- Battery: {{battery_level}}

## Instructions
- Always think carefully before answering.
- Follow user instructions precisely and write clean code.
    """.trimIndent()

    val availableVariables = listOf(
        "{{cur_date}}" to "Ngày",
        "{{cur_time}}" to "Giờ",
        "{{cur_datetime}}" to "Ngày giờ",
        "{{model_name}}" to "Tên mô hình",
        "{{model_id}}" to "ID mô hình",
        "{{locale}}" to "Locale",
        "{{timezone}}" to "Múi giờ",
        "{{device_info}}" to "Thông tin thiết bị",
        "{{system_version}}" to "Phiên bản hệ thống",
        "{{battery_level}}" to "Pin",
        "{{char}}" to "Tên trợ lý",
        "{{user}}" to "Tên người dùng",
    )

    RuntimeCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    RuntimeIcon(RuntimeIconName.Prompt, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("System prompt tùy chỉnh (System Prompt)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Ghi đè System Prompt mặc định toàn cục, hỗ trợ chèn biến macro động", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                )
            }

            // 称呼配置独立于自定义提示词开关：默认提示词同样生效
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Xưng hô & nhân cách", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { showDefaultPromptDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        RuntimeIcon(RuntimeIconName.Visibility, Modifier.size(12.dp))
                        Text("Xem system prompt mặc định", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = charNameBuffer,
                    onValueChange = { charNameBuffer = it },
                    label = { Text("Cách mô hình tự xưng") },
                    placeholder = { Text("TaiXu") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = userNameBuffer,
                    onValueChange = { userNameBuffer = it },
                    label = { Text("Cách gọi người dùng") },
                    placeholder = { Text("Người dùng") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                "Thay đổi có hiệu lực ngay cho mọi cuộc trò chuyện (để trống để về mặc định); các biến macro {{char}} / {{user}} trong prompt tùy chỉnh cũng dùng hai giá trị này.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (enabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Nội dung mẫu prompt:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            textBuffer = defaultPromptTemplate
                            onPromptChange(defaultPromptTemplate)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RuntimeIcon(RuntimeIconName.Refresh, Modifier.size(12.dp))
                            Text("Đặt lại mẫu", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = textBuffer,
                    onValueChange = { textBuffer = it },
                    placeholder = { Text("Nhập system prompt tùy chỉnh tại đây, hỗ trợ các biến macro như {{cur_datetime}}...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5,
                    maxLines = 10,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )

                Text("Chạm để chèn nhanh biến macro động:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    availableVariables.forEach { (variable, label) ->
                        SuggestionChip(
                            onClick = {
                                val updated = if (textBuffer.isBlank()) variable else "$textBuffer $variable"
                                textBuffer = updated
                                onPromptChange(updated)
                            },
                            label = {
                                Text("$label: $variable", style = MaterialTheme.typography.labelSmall)
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
