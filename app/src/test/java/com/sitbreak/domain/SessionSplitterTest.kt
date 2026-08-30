package com.sitbreak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SessionSplitterTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    private fun at(month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `same day session stays in one slice`() {
        val slices = SessionSplitter.split(at(3, 10, 9, 0), at(3, 10, 10, 30), zone)
        assertEquals(listOf(DaySlice("2026-03-10", 90)), slices)
    }

    @Test
    fun `session crossing midnight splits by calendar day`() {
        // 23:10 坐到次日 00:40：前一天记 50 分钟，第二天记 40 分钟
        val slices = SessionSplitter.split(at(3, 10, 23, 10), at(3, 11, 0, 40), zone)
        assertEquals(
            listOf(DaySlice("2026-03-10", 50), DaySlice("2026-03-11", 40)),
            slices,
        )
    }

    @Test
    fun `session spanning a full day produces three slices`() {
        val slices = SessionSplitter.split(at(3, 10, 22, 0), at(3, 12, 1, 0), zone)
        assertEquals(3, slices.size)
        assertEquals(DaySlice("2026-03-10", 120), slices[0])
        assertEquals(DaySlice("2026-03-11", 24 * 60), slices[1])
        assertEquals(DaySlice("2026-03-12", 60), slices[2])
        // 拆分不能凭空多算或少算时长
        assertEquals(120 + 24 * 60 + 60, slices.sumOf { it.minutes })
    }

    @Test
    fun `slices skip sub minute leftovers instead of recording zero`() {
        val slices = SessionSplitter.split(at(3, 10, 9, 0), at(3, 10, 9, 0) + 30_000L, zone)
        assertTrue(slices.isEmpty())
    }

    @Test
    fun `invalid ranges yield nothing`() {
        assertTrue(SessionSplitter.split(0L, at(3, 10, 9, 0), zone).isEmpty())
        assertTrue(SessionSplitter.split(at(3, 10, 10, 0), at(3, 10, 9, 0), zone).isEmpty())
        assertTrue(SessionSplitter.split(at(3, 10, 9, 0), at(3, 10, 9, 0), zone).isEmpty())
    }

    @Test
    fun `dateOf uses local calendar day`() {
        assertEquals("2026-03-10", SessionSplitter.dateOf(at(3, 10, 23, 59), zone))
        assertEquals("2026-03-11", SessionSplitter.dateOf(at(3, 11, 0, 1), zone))
    }

    @Test
    fun `startDateOfLastDays includes today as the last day`() {
        assertEquals("2026-03-04", SessionSplitter.startDateOfLastDays(7, at(3, 10, 12, 0), zone))
        assertEquals("2026-03-10", SessionSplitter.startDateOfLastDays(1, at(3, 10, 12, 0), zone))
    }

    @Test
    fun `month boundary is handled by the calendar not by fixed offsets`() {
        val slices = SessionSplitter.split(at(2, 28, 23, 30), at(3, 1, 0, 30), zone)
        assertEquals(
            listOf(DaySlice("2026-02-28", 30), DaySlice("2026-03-01", 30)),
            slices,
        )
    }
}
