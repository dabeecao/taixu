package top.wkbin.taixu.ui.settings
import top.wkbin.taixu.ui.components.previewOrNull
import top.wkbin.taixu.ui.components.previewStateOf
import org.koin.compose.viewmodel.koinViewModel
import top.wkbin.taixu.ui.settings.LocalizedText as Text
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.wkbin.taixu.ui.components.RuntimeAlertDialog
import top.wkbin.taixu.ui.components.RuntimeButton as Button
import top.wkbin.taixu.ui.components.RuntimeCircularProgressIndicator as CircularProgressIndicator
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconName
import top.wkbin.taixu.ui.components.RuntimeLinearProgressIndicator as LinearProgressIndicator
import top.wkbin.taixu.ui.components.RuntimeOutlinedButton as OutlinedButton
import top.wkbin.taixu.ui.components.RuntimeTextButton as TextButton
import top.wkbin.taixu.ui.components.RuntimeSettingsScaffold
import top.wkbin.taixu.ui.components.MarkdownText
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import top.wkbin.taixu.core.model.AppUpdateInfo
import top.wkbin.taixu.core.model.UpdateCheckState
/**
 * 二级子页 4：关于、版本更新与官方社区
 */
@Composable
fun AboutCommunityScreen(
    onBack: () -> Unit,
    onOpenSponsor: () -> Unit,
    viewModel: SettingsViewModel? = previewOrNull { koinViewModel() },
) {
    val autoCheckUpdates by viewModel?.autoCheckUpdates?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val updateCheckState by viewModel?.updateCheckState?.collectAsStateWithLifecycle() ?: previewStateOf(top.wkbin.taixu.core.model.UpdateCheckState.Idle)
    val downloadProgress by viewModel?.downloadProgress?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val isDownloading by viewModel?.isDownloading?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAboutDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showReleaseNotesDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val currentReleaseNotes by viewModel?.currentReleaseNotes?.collectAsStateWithLifecycle() ?: previewStateOf(null)
    val isLoadingReleaseNotes by viewModel?.isLoadingReleaseNotes?.collectAsStateWithLifecycle() ?: previewStateOf(false)
    val currentVersion = rememberAppVersion()

    // 版本更新弹窗
    when (val state = updateCheckState) {
        is UpdateCheckState.Success -> {
            if (state.info.hasUpdate) {
                UpdateInfoDialog(
                    info = state.info,
                    downloadProgress = downloadProgress,
                    isDownloading = isDownloading,
                    onDownload = { state.info.apkDownloadUrl?.let { viewModel?.downloadAndInstall(it) } },
                    onOpenBrowser = { openBrowser(context, state.info.releaseUrl) },
                    onDismiss = { viewModel?.clearUpdateState() },
                )
            } else {
                RuntimeAlertDialog(
                    onDismissRequest = { viewModel?.clearUpdateState() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RuntimeIcon(RuntimeIconName.Check, Modifier.size(22.dp), tint = successStatusColor())
                            Text("Đã là phiên bản mới nhất", fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text("Phiên bản TaiXu hiện tại v${state.info.currentVersion} đã là bản ổn định mới nhất, không cần cập nhật.")
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel?.clearUpdateState() }) {
                            Text("Xác nhận")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            viewModel?.clearUpdateState()
                            viewModel?.loadCurrentReleaseNotes(currentVersion)
                            showReleaseNotesDialog = true
                        }) {
                            Text("Nhật ký bản này")
                        }
                    },
                )
            }
        }
        is UpdateCheckState.Error -> {
            RuntimeAlertDialog(
                onDismissRequest = { viewModel?.clearUpdateState() },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RuntimeIcon(RuntimeIconName.Alert, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Kiểm tra cập nhật thất bại")
                    }
                },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel?.clearUpdateState() }) {
                        Text("Đã hiểu")
                    }
                },
            )
        }
        else -> Unit
    }

    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false },
            onOpenReleaseNotes = {
                showAboutDialog = false
                viewModel?.loadCurrentReleaseNotes(currentVersion)
                showReleaseNotesDialog = true
            },
        )
    }

    if (showReleaseNotesDialog) {
        VersionReleaseNotesDialog(
            version = currentVersion,
            releaseNotes = currentReleaseNotes,
            isLoading = isLoadingReleaseNotes,
            onDismiss = { showReleaseNotesDialog = false },
            onOpenHistory = { openBrowser(context, "https://github.com/wkbin/taixu/releases") },
        )
    }

    RuntimeSettingsScaffold(
        title = "Giới thiệu & cộng đồng", onBack = onBack,
    ) {
        item {
            Text(
                text = "Phiên bản & cập nhật ứng dụng",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            SettingsGroup {
                SettingsRow(
                    icon = RuntimeIconName.Update,
                    title = "Kiểm tra phiên bản mới",
                    subtitle = "Tự động phát hiện và nâng cấp trực tuyến dựa trên GitHub Releases",
                    value = if (updateCheckState is UpdateCheckState.Checking) "Đang kiểm tra…" else "v$currentVersion",
                    onClick = {
                        // 检查进行中禁止重复触发
                        if (updateCheckState !is UpdateCheckState.Checking) {
                            viewModel?.checkForUpdates(currentVersion)
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Document,
                    title = "Nhật ký cập nhật phiên bản",
                    subtitle = "Xem ghi chú cập nhật và điểm nổi bật của phiên bản hiện tại (v$currentVersion)",
                    onClick = {
                        viewModel?.loadCurrentReleaseNotes(currentVersion)
                        showReleaseNotesDialog = true
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ToggleRow(
                    icon = RuntimeIconName.Update,
                    title = "Tự động kiểm tra cập nhật khi khởi động",
                    subtitle = "Khi khởi động ứng dụng, lặng lẽ kiểm tra phiên bản mới trong nền",
                    checked = autoCheckUpdates,
                    change = { arg0 -> viewModel?.setAutoCheckUpdates(arg0) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Sparkles,
                    title = "Xem lại hướng dẫn tính năng",
                    subtitle = "Hiển thị lại hướng dẫn lần đầu như trung tâm plugin, xưởng, terminal đa phiên…",
                    onClick = {
                        viewModel?.replayFirstUseGuides()
                        Toast.makeText(
                            context,
                            "Đã đặt lại hướng dẫn tính năng, lần vào trang tương ứng sẽ hiển thị lại",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )
            }
        }

        item {
            Text(
                text = "Cộng đồng chính thức & mã nguồn mở",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            SettingsGroup {
                SettingsRow(
                    icon = RuntimeIconName.Github,
                    title = "Dự án mã nguồn mở GitHub",
                    subtitle = "https://github.com/wkbin/taixu · chào đón Star ủng hộ",
                    onClick = { openBrowser(context, "https://github.com/wkbin/taixu") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Qq,
                    title = "Nhóm QQ giao lưu chính thức",
                    subtitle = "Số nhóm: 964382207 · bấm để tham gia / sao chép số nhóm",
                    value = "964382207",
                    onClick = { joinQqGroup(context, "964382207") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Sponsor,
                    title = "Ủng hộ",
                    subtitle = "Ủng hộ TaiXu · tiếp sức phát triển mã nguồn mở",
                    onClick = onOpenSponsor,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsRow(
                    icon = RuntimeIconName.Info,
                    title = "Về TaiXu",
                    subtitle = "Sandbox Linux PRoot gốc Android và trung tâm kết đôi AI",
                    onClick = { showAboutDialog = true },
                )
            }
        }
    }
}

@Composable
private fun AboutAppDialog(
    onDismiss: () -> Unit,
    onOpenReleaseNotes: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = rememberAppVersion()
    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RuntimeIcon(name = RuntimeIconName.Package, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text("TaiXu", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sandbox Linux PRoot gốc Android và trung tâm lập trình kết đôi AI", style = MaterialTheme.typography.bodyMedium)
                Text("Phiên bản: v$appVersion (Material 3 Expressive)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Kiến trúc: aarch64 · chroot-less user-space virtualization", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Giấy phép: Apache-2.0 License", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onOpenReleaseNotes,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    RuntimeIcon(RuntimeIconName.Document, Modifier.size(16.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Nhật ký cập nhật bản này")
                }
                OutlinedButton(
                    onClick = { joinQqGroup(context, "964382207") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    RuntimeIcon(RuntimeIconName.Chat, Modifier.size(16.dp), MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Tham gia nhóm QQ (964382207)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Xác nhận") }
        },
    )
}

@Composable
private fun UpdateInfoDialog(
    info: AppUpdateInfo,
    downloadProgress: Float?,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit,
) {
    RuntimeAlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RuntimeIcon(RuntimeIconName.Refresh, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Phát hiện phiên bản mới v${info.latestVersion}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Phiên bản hiện tại: v${info.currentVersion}  ➔  Phiên bản mới nhất: v${info.latestVersion}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                if (info.releaseNotes.isNotBlank()) {
                    Text(
                        text = "Nhật ký cập nhật:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MarkdownText(
                            markdown = info.releaseNotes,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                if (isDownloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Đang tải gói cài đặt cập nhật...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        if (downloadProgress != null) {
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (info.apkDownloadUrl != null) {
                Button(
                    onClick = onDownload,
                    enabled = !isDownloading,
                ) {
                    Text(if (isDownloading) "Đang tải xuống…" else "Cập nhật ngay trong ứng dụng")
                }
            } else {
                Button(onClick = onOpenBrowser) {
                    Text("Đến GitHub để tải")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDownloading) {
                Text("Để sau")
            }
        },
    )
}

private fun joinQqGroup(context: Context, groupId: String = "964382207") {
    val uri = Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$groupId&card_type=group&source=qrcode")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        // 剪贴板兜底
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Nhóm giao lưu chính thức của TaiXu", groupId)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép số nhóm QQ: $groupId, mở QQ tìm để tham gia", Toast.LENGTH_LONG).show()
    }
}

private fun openBrowser(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("URL", url)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Đã sao chép liên kết: $url", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun VersionReleaseNotesDialog(
    version: String,
    releaseNotes: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    RuntimeAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RuntimeIcon(RuntimeIconName.Document, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Ghi chú cập nhật TaiXu v$version", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isLoading && releaseNotes.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(32.dp))
                    }
                } else if (!releaseNotes.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        MarkdownText(
                            markdown = releaseNotes,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                } else {
                    Text("Chưa có ghi chú cập nhật cho phiên bản này", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Xác nhận") }
        },
        dismissButton = {
            TextButton(onClick = onOpenHistory) { Text("Xem toàn bộ lịch sử") }
        },
    )
}
