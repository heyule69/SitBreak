package com.sitbreak.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sitbreak.MainActivity
import com.sitbreak.R
import com.sitbreak.data.ReminderStyle
import com.sitbreak.util.AppLocale
import com.sitbreak.util.DurationText

/**
 * 通知与渠道的统一封装。
 *
 * Service 拿到的 Context 不跟随应用内的语言选择，所以取文案前都过一道
 * [AppLocale.wrap]，否则切了语言通知还是旧的。
 */
object NotificationHelper {

    const val CH_ONGOING = "ongoing_timer"
    const val CH_LIGHT = "reminder_light"
    const val CH_STANDARD = "reminder_standard"
    const val CH_STRONG = "reminder_strong"
    const val CH_ACHIEVEMENT = "achievement"

    const val NOTIF_ONGOING_ID = 1001
    const val NOTIF_REMINDER_ID = 1002
    const val NOTIF_ACHIEVEMENT_ID = 1003

    fun channelFor(style: ReminderStyle): String = when (style) {
        ReminderStyle.LIGHT -> CH_LIGHT
        ReminderStyle.STANDARD -> CH_STANDARD
        ReminderStyle.STRONG -> CH_STRONG
    }

    fun ensureChannels(context: Context) {
        val c = AppLocale.wrap(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fun channel(id: String, nameRes: Int, descRes: Int, importance: Int, vibration: LongArray?) {
            val ch = NotificationChannel(id, c.getString(nameRes), importance)
            ch.description = c.getString(descRes)
            if (vibration != null) ch.vibrationPattern = vibration else ch.enableVibration(false)
            ch.setSound(null, null)
            nm.createNotificationChannel(ch)
        }
        channel(CH_ONGOING, R.string.notif_channel_ongoing, R.string.notif_channel_ongoing_desc,
            NotificationManager.IMPORTANCE_LOW, null)
        // 微信风格"震动一下"：短促两下
        channel(CH_LIGHT, R.string.notif_channel_light, R.string.notif_channel_light_desc,
            NotificationManager.IMPORTANCE_DEFAULT, longArrayOf(0, 80, 60, 80))
        channel(CH_STANDARD, R.string.notif_channel_standard, R.string.notif_channel_standard_desc,
            NotificationManager.IMPORTANCE_HIGH, longArrayOf(0, 250, 250, 250))
        // 强提醒：长震动循环（配合 FLAG_INSISTENT 重复，直至用户确认或 30 秒自动停止）
        channel(CH_STRONG, R.string.notif_channel_strong, R.string.notif_channel_strong_desc,
            NotificationManager.IMPORTANCE_HIGH, longArrayOf(0, 800, 400, 800, 400, 800))
        channel(CH_ACHIEVEMENT, R.string.notif_channel_achievement, R.string.notif_channel_achievement_desc,
            NotificationManager.IMPORTANCE_DEFAULT, longArrayOf(0, 120, 80, 120))
    }

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_STAND_CONFIRM, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    /** 构建常驻计时通知 */
    fun buildOngoing(context: Context, satMinutes: Long, nextInMinutes: Long, tracking: Boolean): android.app.Notification {
        val c = AppLocale.wrap(context)
        val title = if (tracking) {
            c.getString(R.string.notif_ongoing_title, DurationText.full(c, satMinutes))
        } else {
            c.getString(R.string.notif_paused_title)
        }
        val text = if (tracking) {
            c.getString(R.string.notif_ongoing_text, DurationText.full(c, nextInMinutes))
        } else {
            c.getString(R.string.notif_paused_text)
        }
        return NotificationCompat.Builder(context, CH_ONGOING)
            .setSmallIcon(R.drawable.ic_seat)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(contentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** 常驻计时通知：显示已坐时长与下次提醒 */
    fun notifyOngoing(context: Context, satMinutes: Long, nextInMinutes: Long, tracking: Boolean) {
        safeNotify(context, NOTIF_ONGOING_ID, buildOngoing(context, satMinutes, nextInMinutes, tracking))
    }

    /** 提醒通知（渠道由提醒方式决定） */
    fun notifyReminder(context: Context, style: ReminderStyle, satMinutes: Long) {
        val c = AppLocale.wrap(context)
        val builder = NotificationCompat.Builder(context, channelFor(style))
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle(c.getString(R.string.notif_reminder_title))
            .setContentText(
                c.getString(R.string.notif_reminder_text, DurationText.full(c, satMinutes)),
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .setPriority(
                if (style == ReminderStyle.LIGHT) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
        val n = builder.build()
        if (style == ReminderStyle.STRONG) {
            // 声音与震动循环播放，直到用户点击或 30 秒后由调度器自动取消
            n.flags = n.flags or android.app.Notification.FLAG_INSISTENT
        }
        safeNotify(context, NOTIF_REMINDER_ID, n)
    }

    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_REMINDER_ID)
    }

    /** 成就祝贺通知；[slot] 区分不同成就，同一批解锁多个时不互相覆盖 */
    fun notifyAchievement(context: Context, title: String, desc: String, slot: Int = 0) {
        val n = NotificationCompat.Builder(context, CH_ACHIEVEMENT)
            .setSmallIcon(R.drawable.ic_trophy)
            .setContentTitle(AppLocale.wrap(context).getString(R.string.notif_achievement_title, title))
            .setContentText(desc)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        safeNotify(context, NOTIF_ACHIEVEMENT_ID + slot, n)
    }

    private fun safeNotify(context: Context, id: Int, n: android.app.Notification) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (_: SecurityException) {
        }
    }
}
