package com.sitbreak.service

import android.content.Context
import com.sitbreak.SitBreakApp
import com.sitbreak.domain.AchievementEngine
import com.sitbreak.domain.QuietHours
import com.sitbreak.util.AppLocale
import com.sitbreak.widget.SitBreakWidget

/** UI 层与提醒系统交互的动作封装（站立确认、开始/暂停、设置变更） */
object ReminderActions {

    /**
     * 站立确认：结算本轮久坐、重置会话、取消提醒、调度下一轮。
     *
     * @param auto 由智能暂停自动判定而非用户手动点击
     */
    suspend fun confirmStand(context: Context, auto: Boolean = false) {
        val app = context.applicationContext as SitBreakApp
        val s = app.prefs.current()
        val now = System.currentTimeMillis()
        // 会话起点缺失时仅记一次站立，不估算久坐时长
        app.stats.addStand(s.sessionBeginMillis.takeIf { it > 0 } ?: now, now, auto)
        app.prefs.resetSession(now)
        NotificationHelper.cancelReminder(context)
        if (s.trackingEnabled) {
            ReminderScheduler.scheduleReminder(
                context,
                QuietHours.nextReminderDelay(now, s.intervalMinutes, s.quietWindow),
            )
            ReminderForegroundService.start(context, ReminderForegroundService.ACTION_RESET_SESSION)
        }
        SitBreakWidget.refresh(context)
        // 成就检查（首次站立等）
        checkAchievements(app)
    }

    /** 开始追踪 */
    suspend fun startTracking(context: Context) {
        val app = context.applicationContext as SitBreakApp
        val s = app.prefs.current()
        if (s.sessionBeginMillis <= 0L) app.prefs.resetSession(System.currentTimeMillis())
        app.prefs.setTracking(true)
        ReminderForegroundService.start(context)
        SitBreakWidget.refresh(context)
    }

    /** 暂停追踪 */
    suspend fun pauseTracking(context: Context) {
        val app = context.applicationContext as SitBreakApp
        app.prefs.setTracking(false)
        ReminderForegroundService.stop(context)
        SitBreakWidget.refresh(context)
    }

    /** 设置变更（间隔/方式/勿扰）后重新调度 */
    suspend fun applySettingChanged(context: Context) {
        val app = context.applicationContext as SitBreakApp
        val s = app.prefs.current()
        if (s.trackingEnabled) {
            ReminderForegroundService.start(context, ReminderForegroundService.ACTION_SETTING_CHANGED)
        }
        SitBreakWidget.refresh(context)
    }

    private suspend fun checkAchievements(app: SitBreakApp) {
        val history = app.stats.allHistory()
        val total = app.stats.totalStands()
        val states = AchievementEngine.evaluate(history, total)
        // 去重落到 DataStore：进程重启后不再把历史成就重弹一遍
        val known = app.prefs.notifiedAchievementIds()
        val fresh = states.filter { it.unlocked && it.achievement.id !in known }.map { it.achievement }
        if (fresh.isEmpty()) return
        val c = AppLocale.wrap(app)
        fresh.forEach { ach ->
            NotificationHelper.notifyAchievement(
                app,
                c.getString(ach.titleRes),
                c.getString(ach.descRes),
                slot = AchievementEngine.ALL.indexOfFirst { it.id == ach.id },
            )
        }
        app.prefs.markAchievementsNotified(fresh.map { it.id }.toSet())
    }
}
