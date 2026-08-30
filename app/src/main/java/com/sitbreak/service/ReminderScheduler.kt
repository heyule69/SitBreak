package com.sitbreak.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** 精确闹钟调度：久坐提醒的"下一次"与强提醒的 30 秒自动静音 */
object ReminderScheduler {

    const val ACTION_REMIND = "com.sitbreak.ACTION_REMIND"
    const val ACTION_STRONG_TIMEOUT = "com.sitbreak.ACTION_STRONG_TIMEOUT"

    private const val REQUEST_REMIND = 2001
    private const val REQUEST_STRONG_TIMEOUT = 2002

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || alarmManager(context).canScheduleExactAlarms()

    private fun pending(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, AlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** 调度一次提醒 */
    fun scheduleReminder(context: Context, delayMillis: Long) {
        cancelReminder(context)
        val at = System.currentTimeMillis() + delayMillis
        val pi = pending(context, ACTION_REMIND, REQUEST_REMIND)
        val am = alarmManager(context)
        if (canExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancelReminder(context: Context) {
        alarmManager(context).cancel(pending(context, ACTION_REMIND, REQUEST_REMIND))
    }

    /** 强提醒 30 秒后自动停止响铃/震动 */
    fun scheduleStrongTimeout(context: Context, delayMillis: Long = 30_000L) {
        val at = System.currentTimeMillis() + delayMillis
        val pi = pending(context, ACTION_STRONG_TIMEOUT, REQUEST_STRONG_TIMEOUT)
        val am = alarmManager(context)
        if (canExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}
