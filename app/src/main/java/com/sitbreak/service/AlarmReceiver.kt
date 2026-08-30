package com.sitbreak.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sitbreak.SitBreakApp
import com.sitbreak.data.ReminderStyle
import com.sitbreak.domain.QuietHours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 闹钟触发器：到点发提醒并调度下一次 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        val app = context.applicationContext as SitBreakApp
        when (intent.action) {
            ReminderScheduler.ACTION_REMIND -> {
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val settings = app.prefs.current()
                        Log.d(TAG, "settings: tracking=${settings.trackingEnabled} onboarding=${settings.onboardingCompleted} style=${settings.style} interval=${settings.intervalMinutes}")
                        if (!settings.trackingEnabled || !settings.onboardingCompleted) return@launch
                        val now = System.currentTimeMillis()
                        // 勿扰时段内静默跳过，顺延到窗口结束再恢复节奏
                        if (QuietHours.isWithin(now, settings.quietWindow)) {
                            val wait = QuietHours.millisUntilQuietEnd(now, settings.quietWindow)
                                .coerceAtLeast(60_000L)
                            ReminderScheduler.scheduleReminder(context, wait)
                            Log.d(TAG, "quiet hours, skipped; next in ${wait / 60_000L} min")
                            return@launch
                        }
                        // 记一次提醒
                        app.stats.incrementReminder()
                        // 按提醒方式发通知，文案用真实久坐时长
                        val satMinutes = if (settings.sessionBeginMillis > 0)
                            (now - settings.sessionBeginMillis) / 60_000L
                        else settings.intervalMinutes.toLong()
                        NotificationHelper.notifyReminder(context, settings.style, satMinutes)
                        Log.d(TAG, "reminder notified")
                        if (settings.style == ReminderStyle.STRONG) {
                            // 30 秒后自动停止响铃/震动
                            ReminderScheduler.scheduleStrongTimeout(context)
                        }
                        // 调度下一次提醒（依次下去）
                        ReminderScheduler.scheduleReminder(
                            context,
                            QuietHours.nextReminderDelay(now, settings.intervalMinutes, settings.quietWindow),
                        )
                    } catch (t: Throwable) {
                        Log.e(TAG, "remind failed", t)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ReminderScheduler.ACTION_STRONG_TIMEOUT -> {
                // 强提醒超时：取消持续响铃的通知（保留静默的常驻计时）
                NotificationHelper.cancelReminder(context)
            }
        }
    }

    private companion object {
        const val TAG = "SitBreakAlarm"
    }
}
