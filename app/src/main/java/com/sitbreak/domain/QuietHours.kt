package com.sitbreak.domain

import java.time.Instant
import java.time.ZoneId

/** 勿扰时段配置 */
data class QuietWindow(
    val enabled: Boolean,
    /** 自午夜起的分钟数，0..1439 */
    val startMinute: Int,
    val endMinute: Int,
)

/**
 * 勿扰时段判定与提醒顺延。
 *
 * 久坐提醒在睡眠时段照常震动是不可接受的，这里把"该不该提醒"和
 * "下一次该什么时候提醒"抽成纯函数，便于单测覆盖跨午夜的边界。
 */
object QuietHours {

    const val MINUTES_PER_DAY = 24 * 60

    /** 给定时刻是否落在勿扰窗口内 */
    fun isWithin(minuteOfDay: Int, window: QuietWindow): Boolean {
        if (!window.enabled) return false
        val start = window.startMinute
        val end = window.endMinute
        // 起止相同视为未设置，避免整天静音
        if (start == end) return false
        return if (start < end) {
            minuteOfDay in start until end
        } else {
            // 跨午夜，例如 22:00 -> 07:00
            minuteOfDay >= start || minuteOfDay < end
        }
    }

    fun isWithin(millis: Long, window: QuietWindow, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        isWithin(minuteOfDay(millis, zone), window)

    /**
     * 计算下一次提醒的延迟毫秒。
     *
     * 正常情况就是一个间隔；若落点在勿扰窗口内，则顺延到窗口结束的那一刻，
     * 保证用户醒来后第一时间恢复提醒节奏，而不是整夜被震醒。
     */
    fun nextReminderDelay(
        nowMillis: Long,
        intervalMinutes: Int,
        window: QuietWindow,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val plain = intervalMinutes.coerceAtLeast(1) * 60_000L
        val target = nowMillis + plain
        if (!isWithin(target, window, zone)) return plain
        // now -> target -> 勿扰结束，两段相加就是从现在起的总延迟
        return plain + millisUntilQuietEnd(target, window, zone)
    }

    /** 从给定时刻到勿扰窗口结束还有多少毫秒（给定时刻应在窗口内） */
    fun millisUntilQuietEnd(
        millis: Long,
        window: QuietWindow,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val current = minuteOfDay(millis, zone)
        val minutes = (window.endMinute - current + MINUTES_PER_DAY) % MINUTES_PER_DAY
        // 减去当前分钟已走过的秒数，让落点落在结束时刻的整分钟上
        val secondsInMinute = Instant.ofEpochMilli(millis).atZone(zone).second
        return minutes * 60_000L - secondsInMinute * 1_000L
    }

    fun minuteOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val t = Instant.ofEpochMilli(millis).atZone(zone)
        return t.hour * 60 + t.minute
    }

    /** 把分钟数格式化成 HH:mm */
    fun format(minuteOfDay: Int): String {
        val m = ((minuteOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return "%02d:%02d".format(m / 60, m % 60)
    }
}
