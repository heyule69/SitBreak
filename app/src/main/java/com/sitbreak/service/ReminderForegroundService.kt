package com.sitbreak.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.sitbreak.SitBreakApp
import com.sitbreak.domain.QuietHours
import com.sitbreak.widget.SitBreakWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 前台服务：常驻计时通知 + 维护提醒闹钟。
 * 唤醒/暂停/重置会话等指令通过 startService 的 action 传入。
 */
class ReminderForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var app: SitBreakApp
    private var tickJob: kotlinx.coroutines.Job? = null
    private var detector: SmartPauseDetector? = null

    /** 亮屏/解锁瞬间补一次刷新：灭屏期间 tick 循环随 CPU 休眠停摆，恢复可见时要立刻追上 */
    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scope.launch { refreshTick() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        app = application as SitBreakApp
        NotificationHelper.ensureChannels(this)
        detector = SmartPauseDetector(this) {
            // 传感器回调在主线程，结算走协程
            scope.launch { ReminderActions.confirmStand(this@ReminderForegroundService, auto = true) }
        }
        registerReceiver(
            screenOnReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_USER_PRESENT) },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 先以前台状态启动（占位通知，随后由 tick 循环每分钟刷新）
        startForeground(
            NotificationHelper.NOTIF_ONGOING_ID,
            NotificationHelper.buildOngoing(this, 0, 0, tracking = true)
        )
        scope.launch { handle(intent?.action) }
        return START_STICKY
    }

    private suspend fun handle(action: String?) {
        if (action == ACTION_STOP_TRACKING) {
            ReminderScheduler.cancelReminder(this)
            detector?.stop()
            stopSelf()
            return
        }
        // START / RESET / SETTING_CHANGED：按当前设置继续计时并重新调度
        val s = app.prefs.current()
        if (!s.trackingEnabled || !s.onboardingCompleted) {
            detector?.stop()
            stopSelf()
            return
        }
        val begin = if (s.sessionBeginMillis <= 0L) {
            System.currentTimeMillis().also { app.prefs.resetSession(it) }
        } else {
            s.sessionBeginMillis
        }
        val interval = s.intervalMinutes.coerceAtLeast(1)
        val elapsed = System.currentTimeMillis() - begin
        val plainNext = (interval * 60_000L - elapsed % (interval * 60_000L)).coerceAtLeast(1_000L)
        // 落点在勿扰窗口内就顺延到窗口结束
        val target = System.currentTimeMillis() + plainNext
        val nextIn = if (QuietHours.isWithin(target, s.quietWindow)) {
            plainNext + QuietHours.millisUntilQuietEnd(target, s.quietWindow)
        } else {
            plainNext
        }
        ReminderScheduler.scheduleReminder(this, nextIn)
        // 智能暂停：开关变更后同步传感器监听状态
        if (s.smartPauseEnabled) {
            if (action == ACTION_RESET_SESSION) detector?.resetBaseline()
            detector?.start()
        } else {
            detector?.stop()
        }
        // 取消旧 tick 再启动，避免多次 start 产生并发循环
        tickJob?.cancel()
        tickJob = scope.launch { tickLoop() }
    }

    /**
     * 按当前设置现算一遍常驻通知与小组件。
     * @return 会话起点毫秒；未在追踪时返回 -1，调用方按普通 60s 兜底轮询
     */
    private suspend fun refreshTick(): Long {
        val s = app.prefs.current()
        val begin = s.sessionBeginMillis
        if (!s.trackingEnabled || begin <= 0L) return -1L
        val now = System.currentTimeMillis()
        val satMin = (now - begin) / 60_000L
        val interval = s.intervalMinutes.coerceAtLeast(1)
        val nextIn = interval - (satMin % interval)
        NotificationHelper.notifyOngoing(this, satMin, nextIn.toLong(), tracking = true)
        SitBreakWidget.refresh(this)
        return begin
    }

    private suspend fun tickLoop() {
        while (true) {
            val begin = refreshTick()
            // 不能 delay(60_000) 了事：协程 delay 会随调度累积漂移，几分钟下来通知就
            // 落后应用内时钟。对齐到会话起点的下一个整分边界，误差不随时间累积
            val wait = if (begin <= 0L) 60_000L else {
                (60_000L - (System.currentTimeMillis() - begin) % 60_000L)
                    .coerceIn(1_000L, 60_000L)
            }
            delay(wait)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(screenOnReceiver)
        tickJob?.cancel()
        detector?.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.sitbreak.action.START"
        const val ACTION_RESET_SESSION = "com.sitbreak.action.RESET_SESSION"
        const val ACTION_SETTING_CHANGED = "com.sitbreak.action.SETTING_CHANGED"
        const val ACTION_STOP_TRACKING = "com.sitbreak.action.STOP_TRACKING"

        fun start(context: Context, action: String = ACTION_START) {
            val intent = Intent(context, ReminderForegroundService::class.java).setAction(action)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ReminderForegroundService::class.java).setAction(ACTION_STOP_TRACKING)
            )
        }
    }
}
