package com.sitbreak.ui.settings

import android.app.Activity
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sitbreak.R
import com.sitbreak.SitBreakApp
import com.sitbreak.data.ReminderStyle
import com.sitbreak.domain.QuietHours
import com.sitbreak.domain.RecommendEngine
import com.sitbreak.service.ReminderActions
import com.sitbreak.service.SmartPauseDetector
import com.sitbreak.ui.components.StepperSliderRow
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

/** 设置子页。null = 根列表 */
private enum class SettingsPage { Rhythm, Style, Quiet, Tracking, Permission, Language, Widget, About }

@Composable
fun SettingsScreen(state: SettingsUiState, vm: SettingsViewModel) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    BackHandler(enabled = page != null) { page = null }

    AnimatedContent(
        targetState = page,
        label = "settings_nav",
        transitionSpec = {
            // 进入子页从右侧滑入，返回根列表滑回右侧，与主流 App 的层级动效一致
            if (targetState != null) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
    ) { current ->
        when (current) {
            null -> SettingsHome(state, vm, onOpen = { page = it })
            SettingsPage.Rhythm -> SubPage(stringResource(R.string.interval_title), { page = null }) {
                IntervalSection(
                    interval = state.settings.intervalMinutes,
                    isRecommended = state.settings.intervalIsRecommended,
                    recommended = vm.recommendedMinutes(),
                    onChange = vm::setInterval,
                    onRestore = vm::restoreRecommend,
                )
            }
            SettingsPage.Style -> SubPage(stringResource(R.string.settings_section_style), { page = null }) {
                StyleSection(state.settings.style, vm::setStyle)
            }
            SettingsPage.Quiet -> SubPage(stringResource(R.string.quiet_title), { page = null }) {
                QuietHoursCard(
                    enabled = state.settings.quietEnabled,
                    startMinute = state.settings.quietStartMinute,
                    endMinute = state.settings.quietEndMinute,
                    onToggle = vm::setQuietEnabled,
                    onRangeChange = vm::setQuietRange,
                )
            }
            SettingsPage.Tracking -> SubPage(stringResource(R.string.settings_section_tracking), { page = null }) {
                val context = LocalContext.current
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                TrackingCard(
                    enabled = state.settings.trackingEnabled,
                    onToggle = { enabled ->
                        scope.launch {
                            if (enabled) ReminderActions.startTracking(context)
                            else ReminderActions.pauseTracking(context)
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                SmartPauseCard(enabled = state.settings.smartPauseEnabled, onToggle = vm::setSmartPause)
            }
            SettingsPage.Permission -> SubPage(stringResource(R.string.settings_section_permission), { page = null }) {
                PermissionSection(allowed = state.exactAlarmAllowed)
            }
            SettingsPage.Language -> SubPage(stringResource(R.string.settings_section_language), { page = null }) {
                LanguageCard()
            }
            SettingsPage.Widget -> SubPage(stringResource(R.string.settings_entry_widget), { page = null }) {
                WidgetCard()
            }
            SettingsPage.About -> SubPage(stringResource(R.string.settings_section_about), { page = null }) {
                AboutCard()
            }
        }
    }
}

@Composable
private fun SettingsHome(state: SettingsUiState, vm: SettingsViewModel, onOpen: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    val s = state.settings
    // 小组件是否已添加：从子页返回时根列表重新进组合，此处会重查一次
    val widgetAdded = remember {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, SitBreakWidget::class.java)).isNotEmpty()
    }

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
        SettingsGroupCard(
            Entry(
                R.string.interval_title,
                stringResource(
                    R.string.settings_summary_interval,
                    s.intervalMinutes,
                    stringResource(if (s.intervalIsRecommended) R.string.interval_is_recommended else R.string.interval_custom),
                ),
                R.drawable.ic_timer, Coral, CoralSoft, SettingsPage.Rhythm,
            ),
            Entry(
                R.string.settings_section_style,
                stringResource(styleTitleRes(s.style)),
                R.drawable.ic_alarm, Sunny, SunnySoft, SettingsPage.Style,
            ),
            Entry(
                R.string.quiet_title,
                if (s.quietEnabled) stringResource(
                    R.string.quiet_desc_on,
                    QuietHours.format(s.quietStartMinute),
                    QuietHours.format(s.quietEndMinute),
                ) else stringResource(R.string.quiet_desc_off),
                R.drawable.ic_moon, Indigo, IndigoSoft, SettingsPage.Quiet,
            ),
            onOpen = onOpen,
        )
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_tracking))
        SettingsGroupCard(
            Entry(
                R.string.tracking_title,
                stringResource(if (s.trackingEnabled) R.string.tracking_on else R.string.tracking_off),
                R.drawable.ic_power, Mint, MintSoft, SettingsPage.Tracking,
            ),
            Entry(
                R.string.smart_pause_title,
                if (SmartPauseDetector.isSupported(context)) stringResource(R.string.smart_pause_desc)
                else stringResource(R.string.smart_pause_unsupported),
                R.drawable.ic_run_fast, Sunny, SunnySoft, SettingsPage.Tracking,
            ),
            onOpen = onOpen,
        )
        Spacer(Modifier.height(20.dp))

        SectionTitle(stringResource(R.string.settings_section_general))
        SettingsGroupCard(
            Entry(
                R.string.perm_exact_alarm_title,
                stringResource(if (state.exactAlarmAllowed) R.string.perm_exact_alarm_ok else R.string.perm_exact_alarm_missing),
                R.drawable.ic_shield, if (state.exactAlarmAllowed) Mint else Sunny,
                if (state.exactAlarmAllowed) MintSoft else SunnySoft, SettingsPage.Permission,
            ),
            Entry(
                R.string.language_title,
                languageLabel(context),
                R.drawable.ic_translate, Indigo, IndigoSoft, SettingsPage.Language,
            ),
            Entry(
                R.string.settings_entry_widget,
                stringResource(
                    if (widgetAdded) R.string.widget_status_added else R.string.widget_status_not_added,
                ),
                R.drawable.ic_widget, Coral, CoralSoft, SettingsPage.Widget,
            ),
            Entry(
                R.string.settings_section_about,
                stringResource(R.string.about_version),
                R.drawable.ic_info, Sunny, SunnySoft, SettingsPage.About,
            ),
            onOpen = onOpen,
        )
        Spacer(Modifier.height(28.dp))
    }
}

private data class Entry(
    val titleRes: Int,
    val summary: String,
    val iconRes: Int,
    val iconTint: Color,
    val iconBg: Color,
    val page: SettingsPage,
)

@Composable
private fun SettingsGroupCard(vararg entries: Entry, onOpen: (SettingsPage) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            entries.forEachIndexed { index, entry ->
                EntryRow(entry) { onOpen(entry.page) }
                if (index < entries.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(start = 68.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: Entry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(entry.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(entry.iconRes), null, tint = entry.iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(entry.titleRes), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(entry.summary, fontSize = 12.sp, color = InkGray, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = InkGray.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * 子页骨架：返回箭头 + 标题 + 可滚动内容。
 *
 * 内容不足一屏时垂直居中（否则短页面下半屏全是空白，内容显得堆在顶部）。
 * 不能用 fillMaxSize + Arrangement.Center：内容超屏时居中排布会把顶部裁掉
 * 且滚不到。让滚动列只包内容、由外层 Box 居中，两种情况都正确。
 */
@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
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
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(24.dp))
            }
        }
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

private fun styleTitleRes(style: ReminderStyle): Int = when (style) {
    ReminderStyle.LIGHT -> R.string.style_light_title
    ReminderStyle.STANDARD -> R.string.style_standard_title
    ReminderStyle.STRONG -> R.string.style_strong_title
}

private fun languageLabel(context: Context): String = when (AppLocale.current(context)) {
    AppLocale.ZH -> context.getString(R.string.language_native_zh)
    AppLocale.EN -> context.getString(R.string.language_native_en)
    else -> context.getString(R.string.language_follow_system)
}

@Composable
private fun IntervalSection(
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
            StepperSliderRow(
                value = interval,
                onValueChange = onChange,
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
private fun StyleSection(current: ReminderStyle, onSelect: (ReminderStyle) -> Unit) {
    StyleCard(
        style = ReminderStyle.LIGHT,
        current = current,
        icon = R.drawable.ic_vibrate,
        title = stringResource(R.string.style_light_title),
        desc = stringResource(R.string.style_light_desc),
        tint = Mint,
        container = MintSoft,
        onSelect = onSelect,
    )
    Spacer(Modifier.height(10.dp))
    StyleCard(
        style = ReminderStyle.STANDARD,
        current = current,
        icon = R.drawable.ic_volume_high,
        title = stringResource(R.string.style_standard_title),
        desc = stringResource(R.string.style_standard_desc),
        tint = Sunny,
        container = SunnySoft,
        onSelect = onSelect,
    )
    Spacer(Modifier.height(10.dp))
    StyleCard(
        style = ReminderStyle.STRONG,
        current = current,
        icon = R.drawable.ic_alarm,
        title = stringResource(R.string.style_strong_title),
        desc = stringResource(R.string.style_strong_desc),
        tint = Coral,
        container = CoralSoft,
        onSelect = onSelect,
    )
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
private fun PermissionSection(allowed: Boolean) {
    val context = LocalContext.current
    Card(
        onClick = {
            if (!allowed) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:${context.packageName}"))
                    )
                }
            }
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(if (allowed) MintSoft else SunnySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_shield), null,
                    tint = if (allowed) Mint else Sunny, modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.perm_exact_alarm_title),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp,
                )
                Text(
                    stringResource(if (allowed) R.string.perm_exact_alarm_ok else R.string.perm_exact_alarm_missing),
                    fontSize = 12.sp, color = InkGray,
                )
            }
            if (!allowed) {
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
 * 小组件不会自己重新渲染，得主动推一下；常驻通知由前台服务的分钟级 tick 自行刷新。
 */
private fun switchLanguage(context: Context, tag: String) {
    val needsRecreate = AppLocale.apply(context, tag)
    SitBreakWidget.refresh(context)
    if (needsRecreate) (context as? Activity)?.recreate()
}

/**
 * 桌面小组件页：状态展示 + 一键添加。
 *
 * 小组件由桌面（launcher）管理，App 内做不了真正的"开关"；这里做的是把
 * 添加动作收进来 —— requestPinAppWidget 会弹系统确认，省去教用户长按桌面。
 */
@Composable
private fun WidgetCard() {
    val context = LocalContext.current
    val awm = remember { AppWidgetManager.getInstance(context) }
    val provider = remember { ComponentName(context, SitBreakWidget::class.java) }
    var added by remember { mutableStateOf(awm.getAppWidgetIds(provider).isNotEmpty()) }
    var pinSupported by remember { mutableStateOf(awm.isRequestPinAppWidgetSupported) }

    // 一键添加的确认流程会把用户带离应用，回到前台时重查状态
    val owner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                added = awm.getAppWidgetIds(provider).isNotEmpty()
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(CoralSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_widget), null, tint = Coral, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_entry_widget),
                        fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    )
                    Text(
                        stringResource(
                            if (added) R.string.widget_status_added else R.string.widget_status_not_added,
                        ),
                        fontSize = 12.sp,
                        color = if (added) Mint else InkGray,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.widget_add_hint),
                fontSize = 12.sp, color = InkGray, lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))
            if (added) {
                Text(
                    stringResource(R.string.widget_status_added),
                    fontSize = 13.sp, color = Mint, fontWeight = FontWeight.Bold,
                )
            } else if (pinSupported) {
                Button(
                    onClick = {
                        // 返回 false 说明桌面拒绝了请求（少见），退回手动指引
                        pinSupported = awm.requestPinAppWidget(provider, null, null)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral),
                ) {
                    Text(stringResource(R.string.widget_action_add), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    stringResource(R.string.widget_pin_unsupported),
                    fontSize = 12.sp, color = InkGray, lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.about_version),
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.about_slogan),
                fontSize = 13.sp, color = InkGray, lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
