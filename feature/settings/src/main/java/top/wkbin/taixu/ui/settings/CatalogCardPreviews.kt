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
import top.wkbin.taixu.core.database.AiModelEntity
import top.wkbin.taixu.core.model.QuickPhrase
import top.wkbin.taixu.ui.components.RuntimePreview

/** Populated, offline fixtures cover long names, selected states, and wrapping actions. */
@Preview(name = "卡片 · 浅色", widthDp = 360, heightDp = 900, locale = "zh")
@Preview(name = "卡片 · 深色大字体", widthDp = 360, heightDp = 900, fontScale = 1.3f,
    uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "zh")
@Composable
private fun CatalogCardPreview() {
    val profile = AiModelEntity(id = "preview", name = "本地开发与长文本模型档案", provider = "LOCAL",
        model = "local-coding-model-with-a-long-name", isActive = true, createdAt = 0L, contextTokens = 128_000)
    RuntimePreview {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ModelProfileCard(profile, onEdit = {}, onActivate = {}, onDelete = {}, onExport = {})
            ModelProfileCard(profile.copy(id = "inactive", isActive = false),
                onEdit = {}, onActivate = {}, onDelete = {}, onExport = {})
            QuickPhraseCard(QuickPhrase(id = "phrase", title = "Kiểm tra dự án hiện tại và tổng hợp kết quả build",
                description = "Prompt thường dùng để kiểm tra trạng thái dự án", content = "Kiểm tra cấu trúc dự án, mô tả trạng thái hiện tại và các việc cần xử lý thủ công.",
                iconName = "Code"), onToggle = {}, onEdit = {}, onDelete = {})
        }
    }
}
