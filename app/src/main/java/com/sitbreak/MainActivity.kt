package com.sitbreak

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sitbreak.data.ReminderStyle
import com.sitbreak.service.NotificationHelper
import com.sitbreak.service.ReminderForegroundService
import com.sitbreak.ui.components.AppSkeleton
import com.sitbreak.ui.home.HomeRoute
import com.sitbreak.ui.onboarding.OnboardingRoute
import com.sitbreak.ui.settings.SettingsRoute
import com.sitbreak.ui.stats.StatsRoute
import com.sitbreak.ui.theme.Coral
import com.sitbreak.ui.theme.InkGray
import com.sitbreak.ui.theme.SitBreakTheme
import com.sitbreak.util.AppLocale

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** Android 13 以下靠替换 Context 来切语言 */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 启动窗口用的是带品牌图的 Launch 主题，Activity 一起来就换回正式主题
        setTheme(R.style.Theme_SitBreak)
        super.onCreate(savedInstanceState)
        // 渠道名建立后只能靠重复 createNotificationChannel 刷新；放在这里是因为切
        // 语言会重建 Activity，顺便把通知渠道的语言一起带上
        NotificationHelper.ensureChannels(this)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SitBreakTheme {
                AppRoot(app = application as SitBreakApp, requestNotifPermission = ::ensureNotifPermission)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_STAND_CONFIRM, false) == true) {
            val app = application as SitBreakApp
            app.navigateHomeRequests.value = true
            app.standConfirmRequests.value = true
        }
    }

    private fun ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_OPEN_STAND_CONFIRM = "open_stand_confirm"
    }
}

private sealed class Tab(val route: String, val iconRes: Int, val labelRes: Int) {
    data object Home : Tab("home", R.drawable.ic_clock, R.string.tab_home)
    data object Stats : Tab("stats", R.drawable.ic_chart, R.string.tab_stats)
    data object Settings : Tab("settings", R.drawable.ic_cog, R.string.tab_settings)
}

@Composable
private fun AppRoot(app: SitBreakApp, requestNotifPermission: () -> Unit) {
    val settings by app.prefs.settings.collectAsState(initial = null)
    val s = settings
    when {
        s == null -> AppSkeleton()
        !s.onboardingCompleted -> OnboardingRoute(app) {
            requestNotifPermission()
            ReminderForegroundService.start(app)
        }
        else -> {
            // 首次进入主界面时请求通知权限；进程重启/覆盖安装后恢复前台服务
            androidx.compose.runtime.LaunchedEffect(Unit) {
                requestNotifPermission()
                if (s.trackingEnabled) ReminderForegroundService.start(app)
            }
            MainScaffold(app)
        }
    }
}

@Composable
private fun MainScaffold(app: SitBreakApp) {
    val tabs = listOf(Tab.Home, Tab.Stats, Tab.Settings)
    // 用 saveable：切语言会重建 Activity，不这样做会从设置页被弹回首页
    var current by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(Tab.Home.route)
    }

    // 通知点击请求站立确认时先回到首页，再由 HomeViewModel 消费弹窗
    val goHome by app.navigateHomeRequests.collectAsState()
    androidx.compose.runtime.LaunchedEffect(goHome) {
        if (goHome) {
            current = Tab.Home.route
            app.navigateHomeRequests.value = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    val selected = current == tab.route
                    val label = stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { current = tab.route },
                        icon = {
                            Icon(
                                painterResource(tab.iconRes),
                                contentDescription = label,
                                tint = if (selected) Coral else InkGray,
                            )
                        },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Coral else InkGray,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (current) {
                Tab.Stats.route -> StatsRoute(app)
                Tab.Settings.route -> SettingsRoute(app)
                else -> HomeRoute(app, onOpenSettings = { current = Tab.Settings.route })
            }
        }
    }
}
