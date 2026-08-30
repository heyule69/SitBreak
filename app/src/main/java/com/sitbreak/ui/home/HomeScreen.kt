package com.sitbreak.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.R
import com.sitbreak.SitBreakApp
import com.sitbreak.ui.theme.Coral
import com.sitbreak.ui.theme.CoralSoft
import com.sitbreak.ui.theme.InkBlack
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
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        HeaderRow(onOpenSettings)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            StatusPill(state.tracking)
        }
        PlantScene(
            droop = state.cycleProgress,
            tracking = state.tracking,
            standCount = state.today.standCount,
        )
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val sec = state.nextRemindInSeconds
            Text(
                String.format("%02d:%02d", sec / 60, sec % 60),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkBlack,
            )
            Text(
                stringResource(R.string.home_next_remind),
                fontSize = 12.sp,
                color = InkGray,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        WaterBar(state.cycleProgress)
        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = onToggleTracking,
                modifier = Modifier
                    .size(46.dp)
                    .shadow(2.dp, CircleShape),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = InkGray,
                ),
            ) {
                Icon(
                    painterResource(if (state.tracking) R.drawable.ic_pause else R.drawable.ic_play),
                    stringResource(if (state.tracking) R.string.action_pause else R.string.action_start),
                    modifier = Modifier.size(18.dp),
                )
            }
            Button(
                onClick = onOpenStandConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .shadow(6.dp, RoundedCornerShape(percent = 50)),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
            ) {
                Icon(painterResource(R.drawable.ic_drop), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_stand), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.home_stat_sedentary),
                value = DurationText.compact(LocalContext.current, state.today.sedentaryMinutes),
                container = SunnySoft,
                tint = SunnyInk,
            )
            StatChip(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.home_stat_stands),
                value = pluralStringResource(
                    R.plurals.value_times,
                    state.today.standCount,
                    state.today.standCount,
                ),
                container = MintSoft,
                tint = Mint,
            )
            StatChip(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.home_stat_streak),
                value = pluralStringResource(R.plurals.value_days, state.streak, state.streak),
                container = CoralSoft,
                tint = Coral,
            )
        }
        Spacer(Modifier.height(12.dp))
        HintCard()
        Spacer(Modifier.height(20.dp))
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
            Text(stringResource(greeting), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
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
private fun StatusPill(tracking: Boolean) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(CoralSoft)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_bell_ring), null, tint = Coral, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(if (tracking) R.string.home_pill_tracking else R.string.home_pill_paused),
            color = Coral,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 带叶尖的浇水进度条：填满一轮 = 该起来给它浇水了 */
@Composable
private fun WaterBar(fraction: Float) {
    val anim by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "water",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val fillColor = Mint
    Canvas(Modifier.fillMaxWidth().height(26.dp)) {
        val u = 1.dp.toPx()
        val barH = 10.dp.toPx()
        val cy = size.height / 2f
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, cy - barH / 2f),
            size = Size(size.width, barH),
            cornerRadius = CornerRadius(barH / 2f, barH / 2f),
        )
        val fillW = size.width * anim
        if (fillW > barH) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(0f, cy - barH / 2f),
                size = Size(fillW, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f),
            )
        }
        // 叶尖在填充末端
        val tx = size.width * anim
        rotate(-28f, pivot = Offset(tx, cy)) {
            val leaf = Path().apply {
                moveTo(tx - 3 * u, cy - 11 * u)
                quadraticTo(tx - 8 * u, cy - 4 * u, tx - 1 * u, cy - 2 * u)
                quadraticTo(tx + 6 * u, cy, tx + 5 * u, cy - 6 * u)
                quadraticTo(tx + 4 * u, cy - 10 * u, tx - 3 * u, cy - 11 * u)
            }
            drawPath(leaf, fillColor)
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    label: String,
    value: String,
    container: Color,
    tint: Color,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = tint)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = tint)
    }
}

@Composable
private fun HintCard() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SunnySoft)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_bulb), null, tint = Sunny, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.home_hint),
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = SunnyInk,
        )
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
                Icon(painterResource(R.drawable.ic_drop), null, tint = Mint, modifier = Modifier.size(28.dp))
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
