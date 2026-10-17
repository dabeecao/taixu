package top.wkbin.taixu.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.wkbin.taixu.feature.settings.R
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimePreview

/** Mirrors the four navigation entries in the user's density comparison, without a ViewModel. */
@Preview(name = "默认密度 · 360dp", widthDp = 360, heightDp = 760, locale = "zh")
@Preview(name = "小屏 · 320dp", widthDp = 320, heightDp = 760, locale = "zh")
@Preview(name = "大字体 · 360dp", widthDp = 360, heightDp = 760, fontScale = 1.3f, locale = "zh")
@Preview(name = "English", widthDp = 360, heightDp = 760, locale = "en")
@Composable
private fun SettingsDensityPreview() {
    RuntimePreview {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            PersonalizationSection("Hồ sơ mô hình & nhà cung cấp") {
                PreferenceSurface(last = false) {
                    SettingsRow(RuntimeIconName.Model, "Quản lý hồ sơ mô hình",
                        stringResource(R.string.settings_nav_models_summary), value = "Chưa cấu hình", onClick = {})
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(RuntimeIconName.Cpu, "LLM cục bộ",
                        stringResource(R.string.settings_nav_local_summary), onClick = {})
                }
                PreferenceSurface(first = false, last = false) {
                    SettingsRow(RuntimeIconName.Chat, "Cụm từ nhanh và lệnh thường dùng",
                        stringResource(R.string.settings_nav_phrases_summary), value = "14 cụm từ đã bật", onClick = {})
                }
                PreferenceSurface(first = false) {
                    SettingsRow(RuntimeIconName.Speed, "Thống kê dữ liệu & phân tích mức dùng",
                        stringResource(R.string.settings_nav_stats_summary), onClick = {})
                }
            }
        }
    }
}
