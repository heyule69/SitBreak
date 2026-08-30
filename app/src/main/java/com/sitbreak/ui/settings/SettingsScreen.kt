package com.sitbreak.ui.settings

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.R
import com.sitbreak.SitBreakApp
import com.sitbreak.data.ReminderStyle
import com.sitbreak.domain.QuietHours
import com.sitbreak.domain.RecommendEngine
import com.sitbreak.service.ReminderActions
import com.sitbreak.service.SmartPauseDetector
import com.sitbreak.ui.components.SitBreakSlider
import com.sitbreak.ui.theme.Coral
import com.sitbreak.ui.theme.CoralSoft
import com.sitbreak.ui.theme.Indigo
import com.sitbreak.ui.theme.IndigoSoft
import com.sitbreak.ui.theme.InkGray
import com.sitbreak.ui.theme.Mint
import com.sitbreak.ui.theme.MintSoft
import com.sitbreak.ui.theme.OnSoftContainer
import com.sitbreak.ui.theme.Sunny
import com.sitbreak.ui.theme.SunnySoft
import com.sitbreak.util.AppLocale
import com.sitbreak.widget.SitBreakWidget
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(app: SitBreakApp) {
    val vm: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SettingsViewModel.factory(app),
    )
    val state by vm.state.collectAsState()
    SettingsScreen(state, vm)
}

@Composable
fun SettingsScreen(state: SettingsUiState, vm: SettingsViewModel) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val s = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.tab_settings), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_rhythm))
        IntervalCard(
            interval = s.intervalMinutes,
            isRecommended = s.intervalIsRecommended,
            recommended = vm.recommendedMinutes(),
            onChange = vm::setInterval,
            onRestore = vm::restoreRecommend,
        )
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_style))
        StyleCard(
            style = ReminderStyle.LIGHT,
            current = s.style,
            icon = R.drawable.ic_vibrate,
            title = stringResource(R.string.style_light_title),
            desc = stringResource(R.string.style_light_desc),
            tint = Mint,
            container = MintSoft,
            onSelect = vm::setStyle,
        )
        Spacer(Modifier.height(10.dp))
        StyleCard(
            style = ReminderStyle.STANDARD,
            current = s.style,
            icon = R.drawable.ic_volume_high,
            title = stringResource(R.string.style_standard_title),
            desc = stringResource(R.string.style_standard_desc),
            tint = Sunny,
            container = SunnySoft,
            onSelect = vm::setStyle,
        )
        Spacer(Modifier.height(10.dp))
        StyleCard(
            style = ReminderStyle.STRONG,
            current = s.style,
            icon = R.drawable.ic_alarm,
            title = stringResource(R.string.style_strong_title),
            desc = stringResource(R.string.style_strong_desc),
            tint = Coral,
            container = CoralSoft,
            onSelect = vm::setStyle,
        )
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_tracking))
        TrackingCard(
            enabled = s.trackingEnabled,
            onToggle = { enabled ->
                scope.launch {
                    if (enabled) ReminderActions.startTracking(context) else ReminderActions.pauseTracking(context)
                }
            },
        )
        Spacer(Modifier.height(10.dp))
        SmartPauseCard(
            enabled = s.smartPauseEnabled,
            onToggle = vm::setSmartPause,
        )
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_quiet))
        QuietHoursCard(
            enabled = s.quietEnabled,
            startMinute = s.quietStartMinute,
            endMinute = s.quietEndMinute,
            onToggle = vm::setQuietEnabled,
            onRangeChange = vm::setQuietRange,
        )
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_permission))
        PermissionCard(
            title = stringResource(R.string.perm_exact_alarm_title),
            desc = if (state.exactAlarmAllowed) stringResource(R.string.perm_exact_alarm_ok)
            else stringResource(R.string.perm_exact_alarm_missing),
            ok = state.exactAlarmAllowed,
            onAction = {
                if (!state.exactAlarmAllowed) {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    }
                }
            },
        )
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_language))
        LanguageCard()
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_about))
        AboutCard()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = InkGray,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun IntervalCard(
    interval: Int,
    isRecommended: Boolean,
    recommended: Int,
    onChange: (Int) -> Unit,
    onRestore: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(CoralSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_timer), null, tint = Coral, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.interval_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        stringResource(
                            if (isRecommended) R.string.interval_is_recommended else R.string.interval_custom,
                        ),
                        fontSize = 11.sp, color = InkGray,
                    )
                }
                Text("$interval", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Coral)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.unit_minutes), fontSize = 12.sp, color = InkGray)
            }
            Spacer(Modifier.height(12.dp))
            SitBreakSlider(
                value = interval.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = RecommendEngine.MIN_INTERVAL.toFloat()..RecommendEngine.MAX_INTERVAL.toFloat(),
                accent = Coral,
                trackColor = CoralSoft,
                marker = (recommended - RecommendEngine.MIN_INTERVAL).toFloat() /
                    (RecommendEngine.MAX_INTERVAL - RecommendEngine.MIN_INTERVAL).toFloat(),
                minLabel = stringResource(R.string.duration_minutes, RecommendEngine.MIN_INTERVAL),
                maxLabel = stringResource(R.string.duration_minutes, RecommendEngine.MAX_INTERVAL),
            )
            // 已是推荐值时这行信息与上方"当前为智能推荐值"重复，直接省掉
            if (!isRecommended) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.interval_recommend, recommended),
                        fontSize = 12.sp, color = Coral, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onRestore) {
                        Text(stringResource(R.string.action_restore), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleCard(
    style: ReminderStyle,
    current: ReminderStyle,
    icon: Int,
    title: String,
    desc: String,
    tint: Color,
    container: Color,
    onSelect: (ReminderStyle) -> Unit,
) {
    val selected = style == current
    Card(
        onClick = { onSelect(style) },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) container else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(if (selected) tint else container),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icon), null,
                    tint = if (selected) Color.White else tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Text(desc, fontSize = 12.sp, color = if (selected) OnSoftContainer else InkGray)
            }
            if (selected) {
                Icon(painterResource(R.drawable.ic_check_circle), null, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun TrackingCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(MintSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_power), null, tint = Mint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tracking_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    stringResource(if (enabled) R.string.tracking_on else R.string.tracking_off),
                    fontSize = 12.sp, color = InkGray,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = Mint),
            )
        }
    }
}

@Composable
private fun SmartPauseCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val supported = remember { SmartPauseDetector.isSupported(context) }
    var granted by remember { mutableStateOf(SmartPauseDetector.hasPermission(context)) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        // 授权成功才真正打开，被拒时开关保持关闭，不给用户"已开启但不生效"的假象
        if (ok) onToggle(true)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(if (supported) SunnySoft else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_run_fast), null,
                    tint = if (supported) Sunny else InkGray, modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.smart_pause_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    when {
                        !supported -> stringResource(R.string.smart_pause_unsupported)
                        !granted -> stringResource(R.string.smart_pause_need_permission)
                        else -> stringResource(R.string.smart_pause_desc)
                    },
                    fontSize = 12.sp, color = InkGray,
                )
            }
            Switch(
                checked = enabled && supported,
                enabled = supported,
                onCheckedChange = { want ->
                    if (want && !granted) {
                        request.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        onToggle(want)
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = Sunny),
            )
        }
    }
}

@Composable
private fun QuietHoursCard(
    enabled: Boolean,
    startMinute: Int,
    endMinute: Int,
    onToggle: (Boolean) -> Unit,
    onRangeChange: (Int, Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(IndigoSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_moon), null, tint = Indigo, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.quiet_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        if (enabled) stringResource(
                            R.string.quiet_desc_on,
                            QuietHours.format(startMinute),
                            QuietHours.format(endMinute),
                        ) else stringResource(R.string.quiet_desc_off),
                        fontSize = 12.sp, color = InkGray,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Indigo),
                )
            }
            if (enabled) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeField(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.quiet_start),
                        minuteOfDay = startMinute,
                        onPick = { onRangeChange(it, endMinute) },
                    )
                    TimeField(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.quiet_end),
                        minuteOfDay = endMinute,
                        onPick = { onRangeChange(startMinute, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeField(
    modifier: Modifier,
    label: String,
    minuteOfDay: Int,
    onPick: (Int) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                TimePickerDialog(
                    context,
                    { _, hour, minute -> onPick(hour * 60 + minute) },
                    minuteOfDay / 60,
                    minuteOfDay % 60,
                    true,
                ).show()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = InkGray)
        Spacer(Modifier.height(2.dp))
        Text(QuietHours.format(minuteOfDay), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PermissionCard(title: String, desc: String, ok: Boolean, onAction: () -> Unit) {
    Card(
        onClick = onAction,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(if (ok) MintSoft else SunnySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_shield), null,
                    tint = if (ok) Mint else Sunny, modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(desc, fontSize = 12.sp, color = InkGray)
            }
            if (!ok) {
                Text(stringResource(R.string.perm_action_open), fontSize = 13.sp, color = Coral, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LanguageCard() {
    val context = LocalContext.current
    val current = AppLocale.current(context)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(IndigoSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_translate), null,
                        tint = Indigo, modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.language_title), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(6.dp))
            LanguageRow(stringResource(R.string.language_follow_system), current == AppLocale.FOLLOW_SYSTEM) {
                switchLanguage(context, AppLocale.FOLLOW_SYSTEM)
            }
            LanguageRow(stringResource(R.string.language_native_zh), current == AppLocale.ZH) {
                switchLanguage(context, AppLocale.ZH)
            }
            LanguageRow(stringResource(R.string.language_native_en), current == AppLocale.EN) {
                switchLanguage(context, AppLocale.EN)
            }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Coral else MaterialTheme.colorScheme.onSurface,
        )
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check_circle), null,
                tint = Coral, modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * 切语言。
 *
 * 小组件不会自己重新渲染，得主动推一下；常驻通知由前台服务的秒级 tick 自行刷新。
 */
private fun switchLanguage(context: Context, tag: String) {
    val needsRecreate = AppLocale.apply(context, tag)
    SitBreakWidget.refresh(context)
    if (needsRecreate) (context as? Activity)?.recreate()
}

@Composable
private fun AboutCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.about_version), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                stringResource(R.string.about_credits),
                fontSize = 12.sp, color = InkGray, lineHeight = 18.sp,
            )
        }
    }
}
