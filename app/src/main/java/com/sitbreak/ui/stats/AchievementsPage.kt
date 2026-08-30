package com.sitbreak.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.R
import com.sitbreak.domain.AchievementState
import com.sitbreak.domain.BadgeTone
import com.sitbreak.ui.theme.Coral
import com.sitbreak.ui.theme.CoralSoft
import com.sitbreak.ui.theme.Indigo
import com.sitbreak.ui.theme.IndigoSoft
import com.sitbreak.ui.theme.InkGray
import com.sitbreak.ui.theme.Mint
import com.sitbreak.ui.theme.MintSoft
import com.sitbreak.ui.theme.Sunny
import com.sitbreak.ui.theme.SunnySoft

/** 徽章配色：饱和前景 + 软底色。统计页的徽章卡片与成就页共用 */
@Composable
internal fun badgeToneColors(tone: BadgeTone): Pair<Color, Color> = when (tone) {
    BadgeTone.CORAL -> Coral to CoralSoft
    BadgeTone.MINT -> Mint to MintSoft
    BadgeTone.SUNNY -> Sunny to SunnySoft
    BadgeTone.INDIGO -> Indigo to IndigoSoft
}

/**
 * 成就徽章专属页：12 枚徽章的 3 列网格总览，点任意徽章看详情。
 *
 * 页面由 StatsScreen 内部压栈展示（同设置页的两级导航模式），数据直接复用
 * StatsUiState，不再单开 ViewModel。
 */
@Composable
fun AchievementsPage(state: StatsUiState, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<AchievementState?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier.height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_arrow_left),
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(stringResource(R.string.ach_page_title), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(
                    R.string.stats_badge_ratio,
                    state.achievements.count { it.unlocked },
                    state.achievements.size,
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Coral,
                modifier = Modifier
                    .padding(end = 20.dp)
                    .clip(CircleShape)
                    .background(CoralSoft)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            val overall = if (state.achievements.isEmpty()) 0f
            else state.achievements.sumOf { it.progress.toDouble() }.toFloat() / state.achievements.size
            SummaryChipRow(
                unlocked = state.achievements.count { it.unlocked },
                overallPercent = (overall * 100).toInt(),
                totalStands = state.totalStands,
            )
            Spacer(Modifier.height(14.dp))

            state.achievements.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { a ->
                        BadgeCell(Modifier.weight(1f), a) { selected = a }
                    }
                    // 末行不满 3 个时补占位，保持等宽
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    selected?.let { a ->
        BadgeDetailDialog(a) { selected = null }
    }
}

@Composable
private fun SummaryChipRow(unlocked: Int, overallPercent: Int, totalStands: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryChip(
            Modifier.weight(1f),
            value = unlocked.toString(),
            label = stringResource(R.string.ach_summary_unlocked),
            container = MintSoft, tint = Mint,
        )
        SummaryChip(
            Modifier.weight(1f),
            value = stringResource(R.string.stats_percent, overallPercent),
            label = stringResource(R.string.ach_summary_progress),
            container = SunnySoft, tint = Sunny,
        )
        SummaryChip(
            Modifier.weight(1f),
            value = pluralStringResource(R.plurals.value_times, totalStands, totalStands),
            label = stringResource(R.string.stats_total_stands),
            container = CoralSoft, tint = Coral,
        )
    }
}

@Composable
private fun SummaryChip(modifier: Modifier, value: String, label: String, container: Color, tint: Color) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = tint)
        Text(label, fontSize = 10.sp, color = tint)
    }
}

@Composable
private fun BadgeCell(modifier: Modifier, a: AchievementState, onClick: () -> Unit) {
    val (tint, container) = badgeToneColors(a.achievement.tone)
    val lockedLook = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (a.unlocked) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(if (a.unlocked) container else lockedLook),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(a.achievement.iconRes),
                contentDescription = stringResource(a.achievement.titleRes),
                tint = if (a.unlocked) tint else InkGray.copy(alpha = 0.7f),
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(a.achievement.titleRes),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (a.unlocked) MaterialTheme.colorScheme.onSurface else InkGray,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(
            if (a.unlocked) stringResource(R.string.stats_unlocked)
            else stringResource(R.string.stats_percent, (a.progress * 100).toInt()),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (a.unlocked) tint else InkGray,
        )
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(a.progress)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (a.unlocked) tint else Sunny),
            )
        }
    }
}

@Composable
private fun BadgeDetailDialog(a: AchievementState, onDismiss: () -> Unit) {
    val (tint, container) = badgeToneColors(a.achievement.tone)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (a.unlocked) container else MaterialTheme.colorScheme.surfaceVariant)
                    .alpha(if (a.unlocked) 1f else 0.8f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(a.achievement.iconRes),
                    contentDescription = null,
                    tint = if (a.unlocked) tint else InkGray,
                    modifier = Modifier.size(32.dp),
                )
            }
        },
        title = {
            Text(
                stringResource(a.achievement.titleRes),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(a.achievement.descRes),
                    fontSize = 13.sp,
                    color = InkGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                )
                if (!a.unlocked) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(a.progress)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(tint),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.stats_percent, (a.progress * 100).toInt()),
                        fontSize = 11.sp,
                        color = tint,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(if (a.unlocked) R.string.ach_detail_take else R.string.ach_detail_keep),
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
            }
        },
    )
}
