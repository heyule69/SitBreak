package com.sitbreak.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.R
import com.sitbreak.SitBreakApp
import com.sitbreak.data.DailyStat
import com.sitbreak.ui.theme.Coral
import com.sitbreak.ui.theme.InkGray
import com.sitbreak.ui.theme.Mint
import com.sitbreak.ui.theme.MintGlow
import com.sitbreak.ui.theme.Sunny
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Composable
fun StatsRoute(app: SitBreakApp) {
    val vm: StatsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = StatsViewModel.factory(app),
    )
    val state by vm.state.collectAsState()
    StatsScreen(state)
}

@Composable
fun StatsScreen(state: StatsUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.stats_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(14.dp))

        // 连续打卡 hero：照片 + 火焰数据
        StreakHero(state)
        Spacer(Modifier.height(14.dp))

        Text(stringResource(R.string.stats_last7), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        WeeklyChart(state.last7Days)
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.stats_badges), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(
                    R.string.stats_badge_ratio,
                    state.achievements.count { it.unlocked },
                    state.achievements.size,
                ),
                fontSize = 13.sp,
                color = InkGray,
            )
        }
        Spacer(Modifier.height(10.dp))
        AchievementRow(state.achievements)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StreakHero(state: StatsUiState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(RoundedCornerShape(28.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.img_stretch_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xE623201E),
                        0.6f to Color(0x8C23201E),
                        1f to Color.Transparent,
                    )
                ),
        )
        Column(Modifier
            .align(Alignment.CenterStart)
            .padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_fire), null, tint = Sunny, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.stats_streak_days, state.streak),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row {
                MiniStat(
                    stringResource(R.string.stats_best_streak),
                    pluralStringResource(R.plurals.value_days, state.bestStreak, state.bestStreak),
                )
                Spacer(Modifier.width(24.dp))
                MiniStat(
                    stringResource(R.string.stats_total_stands),
                    pluralStringResource(R.plurals.value_times, state.totalStands, state.totalStands),
                )
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(value, color = Sunny, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xCCFFFFFF), fontSize = 11.sp)
    }
}

/** 自绘近 7 天柱状图：站立次数（薄荷）+ 久坐时长（浅橙） */
@Composable
private fun WeeklyChart(days: List<DailyStat>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (days.isEmpty()) {
                Text(
                    stringResource(R.string.stats_empty),
                    fontSize = 13.sp,
                    color = InkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                )
            } else {
                val maxStand = (days.maxOfOrNull { it.standCount } ?: 1).coerceAtLeast(1)
                // 久坐时长与站立次数量纲不同，各按自己的最大值归一
                val maxSedentary = (days.maxOfOrNull { it.sedentaryMinutes } ?: 1).coerceAtLeast(1)
                val sedentaryBarColor = Coral.copy(alpha = 0.45f)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(118.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    days.forEach { day ->
                        val standFrac = (day.standCount.toFloat() / maxStand).coerceAtLeast(0.04f)
                        val sedFrac = (day.sedentaryMinutes.toFloat() / maxSedentary).coerceAtLeast(0.04f)
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) {
                                Row(
                                    Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    // 久坐时长（浅橙）
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.BottomCenter,
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(sedFrac)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(sedentaryBarColor),
                                        )
                                    }
                                    // 站立次数（薄荷）
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.BottomCenter,
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(standFrac)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Mint, MintGlow)
                                                    )
                                                ),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                dayLabel(day.date),
                                fontSize = 10.sp,
                                color = InkGray,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(Mint)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.stats_legend_stands), fontSize = 11.sp, color = InkGray)
                    Spacer(Modifier.width(16.dp))
                    LegendDot(sedentaryBarColor)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.stats_legend_sedentary), fontSize = 11.sp, color = InkGray)
                }
            }
        }
    }
}

/** 柱状图横轴只需要“几号”，存的是 ISO 日期串，取 dayOfMonth 就好，不必过日期格式化 */
private fun dayLabel(date: String): String =
    try {
        LocalDate.parse(date).dayOfMonth.toString()
    } catch (e: DateTimeParseException) {
        ""
    }

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

/**
 * 成就徽章改为单行横向滑动：整体页面一屏装得下，徽章多了也不撑高版面
 */
@Composable
private fun AchievementRow(states: List<com.sitbreak.domain.AchievementState>) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        states.forEach { a ->
            AchievementCard(Modifier.width(216.dp), a)
        }
    }
}

@Composable
private fun AchievementCard(modifier: Modifier, a: com.sitbreak.domain.AchievementState) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (a.unlocked) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (a.unlocked) Coral else MaterialTheme.colorScheme.outline),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(if (a.unlocked) R.drawable.ic_medal else R.drawable.ic_trophy),
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).alpha(if (a.unlocked) 1f else 0.5f),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        stringResource(a.achievement.titleRes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (a.unlocked) MaterialTheme.colorScheme.onSurface else InkGray,
                    )
                    Text(
                        if (a.unlocked) stringResource(R.string.stats_unlocked)
                        else stringResource(R.string.stats_percent, (a.progress * 100).toInt()),
                        fontSize = 11.sp,
                        color = if (a.unlocked) Coral else InkGray,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(a.achievement.descRes),
                fontSize = 11.sp,
                color = InkGray,
                lineHeight = 15.sp,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(a.progress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (a.unlocked) Coral else Sunny),
                )
            }
        }
    }
}
