package top.wkbin.taixu.ui.settings.stats.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.wkbin.taixu.core.model.StatsLedger
import top.wkbin.taixu.core.model.StatsLedgerGroupDim
import top.wkbin.taixu.core.model.StatsLedgerGroupRow
import top.wkbin.taixu.core.model.StatsTrendDay
import top.wkbin.taixu.ui.components.RuntimeIcon
import top.wkbin.taixu.ui.components.RuntimeIconButton as IconButton
import top.wkbin.taixu.ui.components.RuntimeIconName
import java.util.Locale

/**
 * 「用量」分区：账本口径的核心数字。
 *
 * 只放账本给得出的**真实计量值**；估算口径的 token 三项已从这里退场（由构成条承担），
 * 底部一行只留估算口径独有的**行为指标**，因此与页面其它分区不重复。
 */
@Composable
fun LedgerUsagePanel(
    ledger: StatsLedger,
    conversations: Int,
    messages: Int,
    launches: Int,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsSectionCard(
        title = "Mức dùng",
        modifier = modifier,
        trailing = {
            IconButton(onClick = onOpenDetail, modifier = Modifier.size(24.dp)) {
                RuntimeIcon(
                    name = RuntimeIconName.OpenInNew,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                LedgerMetricRow("Tổng token", ledgerCompact(ledger.totalTokens))
                LedgerMetricRow("Đầu vào", ledgerCompact(ledger.inputTokens))
                LedgerMetricRow("Suy luận", ledgerCompact(ledger.reasoningTokens))
                LedgerMetricRow("Đọc cache", ledgerCompact(ledger.cacheReadTokens))
                LedgerMetricRow("Trung bình/lần", ledgerCompact(ledger.averageTokensPerCall()))
            }
            Column(modifier = Modifier.weight(1f)) {
                LedgerMetricRow("Lượt gọi LLM", "${ledger.calls}")
                LedgerMetricRow("Đầu ra", ledgerCompact(ledger.outputTokens))
                LedgerMetricRow("Tỉ lệ trúng cache", ledgerPercent(ledger.cacheHitRate))
                LedgerMetricRow("Ghi cache", ledgerCompact(ledger.cacheWriteTokens))
                LedgerMetricRow("Chi phí", "Chưa tính")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LedgerCounter("Cuộc trò chuyện", "$conversations", Modifier.weight(1f))
            LedgerCounter("Tin nhắn", ledgerCompact(messages.toLong()), Modifier.weight(1f))
            LedgerCounter("Lượt mở", "$launches", Modifier.weight(1f))
        }

        Text(
            text = "Trên đây đều là số liệu thực tế do sổ mức dùng ghi lại; ba mục dưới là chỉ số hành vi không liên quan token. Chi phí chưa được tính — hiện chưa có bảng giá.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun LedgerMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun LedgerCounter(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            maxLines = 1,
        )
    }
}

/**
 * 「用量趋势」分区。
 *
 * 复用既有 [StatsUsageChart]：喂账本口径的数据即可，于是「点立柱看当日各模型」
 * 与「左右滑动看趋势」两个既有手感原样继承，不需要重写手势。
 */
@Composable
fun LedgerTrendPanel(
    days: List<StatsTrendDay>,
    modifier: Modifier = Modifier,
) {
    StatsSectionCard(title = "Xu hướng mức dùng", modifier = modifier) {
        if (days.isEmpty()) {
            Text(
                text = "Không có bản ghi sổ trong phạm vi đã chọn",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(vertical = 12.dp),
            )
        } else {
            StatsUsageChart(days = days, modifier = Modifier.fillMaxWidth())
            Text(
                text = "Chạm vào cột để xem mức dùng từng mô hình trong ngày, vuốt ngang để xem khoảng thời gian sớm hơn.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                ),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * 「分解」分区：同一批数字的三种拆法。
 *
 * 比例条**同色**，只表达"占多少"；类型构成（输入/输出/推理/缓存写）留给「构成」分区，
 * 两块刻意不重叠。
 */
@Composable
fun LedgerBreakdownPanel(
    rows: List<StatsLedgerGroupRow>,
    groupDim: StatsLedgerGroupDim,
    onGroupChange: (StatsLedgerGroupDim) -> Unit,
    modifier: Modifier = Modifier,
) {
    StatsSectionCard(title = "Phân rã", modifier = modifier) {
        LedgerGroupControls(groupDim = groupDim, onGroupChange = onGroupChange)
        Spacer(modifier = Modifier.height(12.dp))

        if (rows.isEmpty()) {
            Text(
                text = "Không có dữ liệu để phân rã trong phạm vi này",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                ),
            )
            return@StatsSectionCard
        }

        val maxTokens = rows.maxOf { it.totalTokens }.coerceAtLeast(1L)
        val sumTokens = rows.sumOf { it.totalTokens }.coerceAtLeast(1L)

        for (row in rows) {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "${ledgerCompact(row.totalTokens)} · ${ledgerPercent(row.totalTokens.toDouble() / sumTokens)} · ${row.calls} lần",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((row.totalTokens.toFloat() / maxTokens.toFloat()).coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
                Text(
                    text = "Vào ${ledgerCompact(row.inputTokens)} · Ra ${ledgerCompact(row.outputTokens)} · Suy luận ${ledgerCompact(row.reasoningTokens)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

/**
 * 「构成」分区：token 类型构成。
 * 缓存命中读取**不计入**合计——它是输入前缀的复用部分，计入会与输入双重计算。
 */
@Composable
fun LedgerCompositionPanel(
    ledger: StatsLedger,
    modifier: Modifier = Modifier,
) {
    val segments = listOf(
        Triple("Đầu vào", ledger.inputTokens, MaterialTheme.colorScheme.primary),
        Triple("Đầu ra", ledger.outputTokens, MaterialTheme.colorScheme.secondary),
        Triple("Suy luận", ledger.reasoningTokens, MaterialTheme.colorScheme.tertiary),
        Triple("Ghi cache", ledger.cacheWriteTokens, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ).filter { (_, value, _) -> value > 0L }

    StatsSectionCard(title = "Cơ cấu", modifier = modifier) {
        if (segments.isEmpty()) {
            Text(
                text = "Không có bản ghi sổ trong phạm vi đã chọn",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                ),
            )
            return@StatsSectionCard
        }

        val total = segments.sumOf { it.second }.coerceAtLeast(1L)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            for ((_, value, color) in segments) {
                Box(
                    modifier = Modifier
                        .weight(value.toFloat() / total.toFloat())
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            for ((label, value, color) in segments) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Text(
                        text = "$label  ${ledgerCompact(value)} · ${ledgerPercent(value.toDouble() / total)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        Text(
            text = "Đọc trúng cache ${ledgerCompact(ledger.cacheReadTokens)} không tính vào tổng: đó là phần tái sử dụng của tiền tố đầu vào, nếu tính sẽ trùng với đầu vào.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            ),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** 计费口径里"一次调用平均多少 token"。 */
internal fun StatsLedger.averageTokensPerCall(): Long =
    if (calls <= 0) 0L else totalTokens / calls

internal fun ledgerPercent(ratio: Double): String {
    if (ratio <= 0.0) return "0%"
    return String.format(Locale.US, "%.1f%%", ratio * 100.0)
}

internal fun ledgerCompact(value: Long): String {
    if (value < 1000L) return value.toString()
    if (value < 1_000_000L) {
        val k = value / 1000.0
        return String.format(Locale.US, if (k < 10) "%.1fk" else "%.0fk", k)
    }
    val m = value / 1_000_000.0
    return String.format(Locale.US, if (m < 10) "%.1fM" else "%.0fM", m)
}
