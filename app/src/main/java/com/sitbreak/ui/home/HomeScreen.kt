package com.sitbreak.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.R
import com.sitbreak.SitBreakApp
import com.sitbreak.ui.theme.Coral
import com.sitbreak.ui.theme.CoralGlow
import com.sitbreak.ui.theme.CoralSoft
import com.sitbreak.ui.theme.InkGray
import com.sitbreak.ui.theme.Mint
import com.sitbreak.ui.theme.MintSoft
import com.sitbreak.ui.theme.Sunny
import com.sitbreak.ui.theme.SunnyInk
import com.sitbreak.ui.theme.SunnySoft
import com.sitbreak.util.DurationText
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeRoute(
    app: SitBreakApp,
    onOpenSettings: () -> Unit,
) {
    val vm: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = HomeViewModel.factory(app),
    )
    val state by vm.state.collectAsState()
    HomeScreen(state, vm::toggleTracking, vm::openStandConfirm, vm::dismissStandConfirm, vm::confirmStand, onOpenSettings)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleTracking: () -> Unit,
    onOpenStandConfirm: () -> Unit,
    onDismissStandConfirm: () -> Unit,
    onConfirmStand: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        HeaderRow(onOpenSettings)
        Spacer(Modifier.height(20.dp))
        TimerHeroCard(state, onToggleTracking)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_clock,
                label = stringResource(R.string.home_stat_sedentary),
                value = DurationText.compact(LocalContext.current, state.today.sedentaryMinutes),
                tint = Sunny,
                container = SunnySoft,
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_shoe_print,
                label = stringResource(R.string.home_stat_stands),
                value = pluralStringResource(
                    R.plurals.value_times,
                    state.today.standCount,
                    state.today.standCount,
                ),
                tint = Mint,
                container = MintSoft,
            )
            StatChip(
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_fire,
                label = stringResource(R.string.home_stat_streak),
                value = pluralStringResource(R.plurals.value_days, state.streak, state.streak),
                tint = Coral,
                container = CoralSoft,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onOpenStandConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint),
        ) {
            Icon(painterResource(R.drawable.ic_walk), null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(10.dp))
            Text(stringResource(R.string.action_stand), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        HintCard()
        Spacer(Modifier.height(24.dp))
    }

    if (state.showStandConfirm) {
        StandConfirmDialog(
            satMinutes = state.satMinutes,
            onDismiss = onDismissStandConfirm,
            onConfirm = onConfirmStand,
        )
    }
}

@Composable
private fun HeaderRow(onOpenSettings: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 6 -> R.string.greeting_night
        hour < 12 -> R.string.greeting_morning
        hour < 18 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(greeting), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                // 日期格式中英文语序不同，pattern 也放在资源里随语言切换
                LocalDate.now().format(
                    DateTimeFormatter.ofPattern(
                        stringResource(R.string.home_date_pattern),
                        Locale.getDefault(),
                    ),
                ),
                fontSize = 13.sp,
                color = InkGray,
            )
        }
        FilledIconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Icon(
                painterResource(R.drawable.ic_cog),
                stringResource(R.string.tab_settings),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TimerHeroCard(state: HomeUiState, onToggle: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Coral, CoralGlow)))
            .padding(vertical = 28.dp),
    ) {
        // 右下角装饰小人
        Icon(
            painterResource(if (state.tracking) R.drawable.ic_walk else R.drawable.ic_seat),
            null,
            tint = Color(0x30FFFFFF),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(110.dp)
                .padding(end = 12.dp, bottom = 0.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_bell_ring), null, tint = Color(0xFFFFE3DA), modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    stringResource(if (state.tracking) R.string.state_tracking else R.string.state_paused),
                    color = Color(0xFFFFE3DA),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(18.dp))
            Box(contentAlignment = Alignment.Center) {
                val stroke = 14f
                Canvas(Modifier.size(190.dp)) {
                    val diameter = size.minDimension - stroke
                    drawArc(
                        color = Color(0x40FFFFFF),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    )
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * state.cycleProgress,
                        useCenter = false,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val sec = state.nextRemindInSeconds
                    Text(
                        String.format("%02d:%02d", sec / 60, sec % 60),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.home_next_remind), fontSize = 13.sp, color = Color(0xFFFFE3DA))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(
                    R.string.home_sat_this_round,
                    DurationText.full(LocalContext.current, state.satMinutes),
                ),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(14.dp))
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White,
                    contentColor = Coral,
                ),
            ) {
                Icon(
                    painterResource(if (state.tracking) R.drawable.ic_pause else R.drawable.ic_play),
                    stringResource(if (state.tracking) R.string.action_pause else R.string.action_start),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    icon: Int,
    label: String,
    value: String,
    tint: Color,
    container: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = InkGray)
        }
    }
}

@Composable
private fun HintCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SunnySoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_bulb), null, tint = Sunny, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Text(
                stringResource(R.string.home_hint),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = SunnyInk,
            )
        }
    }
}

@Composable
private fun StandConfirmDialog(satMinutes: Long, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_handsup), null, tint = Mint, modifier = Modifier.size(30.dp))
            }
        },
        title = { Text(stringResource(R.string.dialog_stand_title), fontWeight = FontWeight.ExtraBold) },
        text = {
            Text(
                stringResource(
                    R.string.dialog_stand_text,
                    DurationText.full(LocalContext.current, satMinutes),
                ),
                lineHeight = 22.sp,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
            ) { Text(stringResource(R.string.dialog_stand_confirm), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_stand_dismiss), color = InkGray)
            }
        },
    )
}
