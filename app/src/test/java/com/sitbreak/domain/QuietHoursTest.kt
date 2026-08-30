package com.sitbreak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class QuietHoursTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    /** 22:00 ~ 次日 07:00，典型的睡眠时段 */
    private val night = QuietWindow(enabled = true, startMinute = 22 * 60, endMinute = 7 * 60)

    /** 12:00 ~ 14:00，不跨午夜的午休时段 */
    private val nap = QuietWindow(enabled = true, startMinute = 12 * 60, endMinute = 14 * 60)

    private fun at(day: Int, hour: Int, minute: Int, second: Int = 0): Long =
        LocalDateTime.of(2026, 3, day, hour, minute, second).atZone(zone).toInstant().toEpochMilli()

    private fun min(hour: Int, minute: Int = 0) = hour * 60 + minute

    @Test
    fun `disabled window never matches`() {
        val off = night.copy(enabled = false)
        assertFalse(QuietHours.isWithin(min(23), off))
        assertFalse(QuietHours.isWithin(min(3), off))
    }

    @Test
    fun `window crossing midnight covers both sides`() {
        assertTrue(QuietHours.isWithin(min(22), night))
        assertTrue(QuietHours.isWithin(min(23, 59), night))
        assertTrue(QuietHours.isWithin(min(0), night))
        assertTrue(QuietHours.isWithin(min(6, 59), night))
    }

    @Test
    fun `window end is exclusive so reminders resume right at wake up time`() {
        assertFalse(QuietHours.isWithin(min(7), night))
        assertFalse(QuietHours.isWithin(min(21, 59), night))
        assertFalse(QuietHours.isWithin(min(12), night))
    }

    @Test
    fun `same day window does not wrap around`() {
        assertFalse(QuietHours.isWithin(min(11, 59), nap))
        assertTrue(QuietHours.isWithin(min(12), nap))
        assertTrue(QuietHours.isWithin(min(13, 59), nap))
        assertFalse(QuietHours.isWithin(min(14), nap))
        assertFalse(QuietHours.isWithin(min(3), nap))
    }

    @Test
    fun `identical start and end is treated as unset instead of muting all day`() {
        val degenerate = QuietWindow(enabled = true, startMinute = min(9), endMinute = min(9))
        assertFalse(QuietHours.isWithin(min(9), degenerate))
        assertFalse(QuietHours.isWithin(min(15), degenerate))
    }

    @Test
    fun `reminder outside the window keeps the plain interval`() {
        val delay = QuietHours.nextReminderDelay(at(10, 9, 0), 45, night, zone)
        assertEquals(45 * 60_000L, delay)
    }

    @Test
    fun `reminder landing inside the window is pushed to the window end`() {
        // 21:30 起算 45 分钟会落在 22:15，处于勿扰内，应顺延到次日 07:00
        val now = at(10, 21, 30)
        val delay = QuietHours.nextReminderDelay(now, 45, night, zone)
        assertEquals(at(11, 7, 0), now + delay)
    }

    @Test
    fun `quiet window is ignored when disabled`() {
        val now = at(10, 21, 30)
        val delay = QuietHours.nextReminderDelay(now, 45, night.copy(enabled = false), zone)
        assertEquals(45 * 60_000L, delay)
    }

    @Test
    fun `millisUntilQuietEnd counts to the next occurrence of the end time`() {
        assertEquals(9 * 60 * 60_000L, QuietHours.millisUntilQuietEnd(at(10, 22, 0), night, zone))
        assertEquals(60_000L, QuietHours.millisUntilQuietEnd(at(11, 6, 59), night, zone))
    }

    @Test
    fun `millisUntilQuietEnd trims the partial minute so the alarm lands on the hour`() {
        val at2230With40s = at(10, 22, 30, 40)
        val expected = 8 * 60 * 60_000L + 30 * 60_000L - 40_000L
        assertEquals(expected, QuietHours.millisUntilQuietEnd(at2230With40s, night, zone))
    }

    @Test
    fun `interval is clamped to at least one minute`() {
        assertEquals(60_000L, QuietHours.nextReminderDelay(at(10, 9, 0), 0, night.copy(enabled = false), zone))
    }

    @Test
    fun `format pads to two digits and wraps out of range values`() {
        assertEquals("07:00", QuietHours.format(min(7)))
        assertEquals("22:05", QuietHours.format(min(22, 5)))
        assertEquals("00:00", QuietHours.format(QuietHours.MINUTES_PER_DAY))
        assertEquals("23:00", QuietHours.format(-60))
    }
}
