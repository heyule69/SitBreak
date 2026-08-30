package com.sitbreak.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 一段久坐落在某一天里的时长 */
data class DaySlice(val date: String, val minutes: Int)

/**
 * 久坐会话按自然日切分。
 *
 * 23:40 坐到次日 00:20 这种跨午夜的会话，必须把 40 分钟分别记到两天，
 * 否则昨天的久坐时长会凭空消失。
 */
object SessionSplitter {

    /** 把 [beginMillis, endMillis) 按本地自然日切成若干片 */
    fun split(
        beginMillis: Long,
        endMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<DaySlice> {
        if (beginMillis <= 0L || endMillis <= beginMillis) return emptyList()
        val slices = mutableListOf<DaySlice>()
        var cursor = beginMillis
        while (cursor < endMillis) {
            val date = dateAt(cursor, zone)
            val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sliceEnd = minOf(nextMidnight, endMillis)
            val minutes = ((sliceEnd - cursor) / 60_000L).toInt()
            if (minutes > 0) slices += DaySlice(date.toString(), minutes)
            cursor = sliceEnd
        }
        return slices
    }

    /** 某个时刻所属的本地日期，yyyy-MM-dd */
    fun dateOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dateAt(millis, zone).toString()

    /** 今天往前数 days 天（含今天）的起始日期，用于统计区间查询 */
    fun startDateOfLastDays(
        days: Int,
        todayMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = dateAt(todayMillis, zone).minusDays((days - 1).toLong()).toString()

    private fun dateAt(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
