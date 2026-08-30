package com.sitbreak.domain

import com.sitbreak.data.DailyStat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AchievementEngineTest {

    private fun day(date: String, stands: Int = 0, sedentary: Int = 0, reminders: Int = 0) =
        DailyStat(date = date, sedentaryMinutes = sedentary, standCount = stands, reminderCount = reminders)

    @Test
    fun `longest streak counts consecutive calendar days`() {
        val history = listOf(
            day("2026-03-01", stands = 2),
            day("2026-03-02", stands = 1),
            day("2026-03-03", stands = 4),
            day("2026-03-05", stands = 1),
        )
        assertEquals(3, AchievementEngine.longestStreak(history))
    }

    @Test
    fun `a day without stands breaks the streak`() {
        val history = listOf(
            day("2026-03-01", stands = 1),
            day("2026-03-02", stands = 0),
            day("2026-03-03", stands = 1),
            day("2026-03-04", stands = 1),
        )
        assertEquals(2, AchievementEngine.longestStreak(history))
    }

    @Test
    fun `streak spans month boundaries`() {
        val history = listOf(
            day("2026-02-27", stands = 1),
            day("2026-02-28", stands = 1),
            day("2026-03-01", stands = 1),
        )
        assertEquals(3, AchievementEngine.longestStreak(history))
    }

    @Test
    fun `unsorted history is still evaluated correctly`() {
        val history = listOf(
            day("2026-03-03", stands = 1),
            day("2026-03-01", stands = 1),
            day("2026-03-02", stands = 1),
        )
        assertEquals(3, AchievementEngine.longestStreak(history))
    }

    @Test
    fun `malformed dates are skipped instead of crashing`() {
        val history = listOf(
            day("not-a-date", stands = 1),
            day("2026-03-01", stands = 1),
            day("2026-03-02", stands = 1),
        )
        assertEquals(2, AchievementEngine.longestStreak(history))
    }

    @Test
    fun `current streak counts back from today`() {
        val today = LocalDate.of(2026, 3, 10)
        val history = listOf(
            day("2026-03-08", stands = 1),
            day("2026-03-09", stands = 1),
            day("2026-03-10", stands = 2),
        )
        assertEquals(3, AchievementEngine.currentStreak(history, today))
    }

    @Test
    fun `an unfinished today does not break the streak`() {
        val today = LocalDate.of(2026, 3, 10)
        val history = listOf(
            day("2026-03-08", stands = 1),
            day("2026-03-09", stands = 1),
        )
        assertEquals(2, AchievementEngine.currentStreak(history, today))
    }

    @Test
    fun `a missed yesterday resets the streak to zero`() {
        val today = LocalDate.of(2026, 3, 10)
        val history = listOf(
            day("2026-03-07", stands = 1),
            day("2026-03-08", stands = 1),
        )
        assertEquals(0, AchievementEngine.currentStreak(history, today))
    }

    @Test
    fun `empty history unlocks nothing`() {
        val states = AchievementEngine.evaluate(emptyList(), totalStands = 0)
        assertEquals(AchievementEngine.ALL.size, states.size)
        assertTrue(states.none { it.unlocked })
        assertTrue(states.all { it.progress in 0f..1f })
    }

    @Test
    fun `first stand unlocks on a single confirmation`() {
        val states = AchievementEngine.evaluate(listOf(day("2026-03-10", stands = 1)), totalStands = 1)
        assertTrue(states.first { it.achievement.id == "first_stand" }.unlocked)
        assertFalse(states.first { it.achievement.id == "century" }.unlocked)
    }

    @Test
    fun `light day needs a tracked day under six hours`() {
        // 只有产生过提醒的日子才算真实追踪过，否则空白日会白送成就
        val untracked = AchievementEngine.evaluate(
            listOf(day("2026-03-10", stands = 1, sedentary = 0, reminders = 0)),
            totalStands = 1,
        )
        assertFalse(untracked.first { it.achievement.id == "light_day" }.unlocked)

        val tracked = AchievementEngine.evaluate(
            listOf(day("2026-03-10", stands = 1, sedentary = 300, reminders = 4)),
            totalStands = 1,
        )
        assertTrue(tracked.first { it.achievement.id == "light_day" }.unlocked)
    }

    @Test
    fun `light day progress grows as sedentary time shrinks`() {
        fun progressAt(minutes: Int) = AchievementEngine
            .evaluate(listOf(day("2026-03-10", stands = 1, sedentary = minutes, reminders = 4)), 1)
            .first { it.achievement.id == "light_day" }
            .progress

        val far = progressAt(720)
        val near = progressAt(400)
        assertTrue("$near should be closer to done than $far", near > far)
        assertEquals(1f, progressAt(300), 0.001f)
    }

    @Test
    fun `all progress values stay normalised`() {
        val states = AchievementEngine.evaluate(
            listOf(day("2026-03-10", stands = 50, sedentary = 100, reminders = 9)),
            totalStands = 500,
        )
        assertTrue(states.all { it.progress in 0f..1f })
    }

    @Test
    fun `stand_12 unlocks only at twelve stands a day`() {
        val eleven = AchievementEngine.evaluate(listOf(day("2026-03-10", stands = 11)), totalStands = 11)
        assertFalse(eleven.first { it.achievement.id == "stand_12" }.unlocked)

        val twelve = AchievementEngine.evaluate(listOf(day("2026-03-10", stands = 12)), totalStands = 12)
        assertTrue(twelve.first { it.achievement.id == "stand_12" }.unlocked)
    }

    @Test
    fun `streak_14 sits between streak_7 and streak_30`() {
        val history = (1..13).map { day("2026-03-" + it.toString().padStart(2, '0'), stands = 1) }
        val states = AchievementEngine.evaluate(history, totalStands = 13)
        assertTrue(states.first { it.achievement.id == "streak_7" }.unlocked)
        assertFalse(states.first { it.achievement.id == "streak_14" }.unlocked)
        assertFalse(states.first { it.achievement.id == "streak_30" }.unlocked)
    }

    @Test
    fun `total_500 needs five hundred stands`() {
        val below = AchievementEngine.evaluate(emptyList(), totalStands = 499)
        assertFalse(below.first { it.achievement.id == "total_500" }.unlocked)

        val at = AchievementEngine.evaluate(emptyList(), totalStands = 500)
        assertTrue(at.first { it.achievement.id == "total_500" }.unlocked)
    }

    @Test
    fun `weekend warrior needs both days of one weekend`() {
        // 2026-03-07 是周六、03-08 周日；只打一天记半程进度
        val satOnly = AchievementEngine.evaluate(listOf(day("2026-03-07", stands = 1)), totalStands = 1)
        val satState = satOnly.first { it.achievement.id == "weekend_warrior" }
        assertFalse(satState.unlocked)
        assertEquals(0.5f, satState.progress, 0.001f)

        val both = AchievementEngine.evaluate(
            listOf(day("2026-03-07", stands = 1), day("2026-03-08", stands = 1)),
            totalStands = 2,
        )
        assertTrue(both.first { it.achievement.id == "weekend_warrior" }.unlocked)
    }

    @Test
    fun `tamer_7 needs seven tracked days under eight hours`() {
        val sixDays = (1..6).map { day("2026-03-0$it", sedentary = 300, reminders = 5) }
        assertFalse(
            AchievementEngine.evaluate(sixDays, totalStands = 6)
                .first { it.achievement.id == "tamer_7" }.unlocked,
        )

        val sevenDays = sixDays + day("2026-03-07", sedentary = 300, reminders = 5)
        assertTrue(
            AchievementEngine.evaluate(sevenDays, totalStands = 7)
                .first { it.achievement.id == "tamer_7" }.unlocked,
        )

        // 没有提醒记录的空白日不算，否则从没打开 App 的日子会白送达标天数
        val untracked = (1..7).map { day("2026-03-0$it", sedentary = 0, reminders = 0) }
        assertEquals(0, AchievementEngine.tamerDays(untracked))
    }
}
