package com.sitbreak.domain

import androidx.annotation.StringRes
import com.sitbreak.R
import com.sitbreak.data.DailyStat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** 徽章配色角色：UI 层据此取品牌色对（饱和前景 + 软底色），图标各自独立 */
enum class BadgeTone { CORAL, MINT, SUNNY, INDIGO }

/** 成就定义（文案以资源 ID 保存，由展示层按当前语言取字符串） */
data class Achievement(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val iconRes: Int,
    val tone: BadgeTone,
)

/** 成就解锁状态 */
data class AchievementState(
    val achievement: Achievement,
    val unlocked: Boolean,
    val progress: Float, // 0..1
)

/**
 * 成就引擎：从历史统计数据派生成就达成情况（无需单独持久化解锁表）。
 */
object AchievementEngine {

    val ALL = listOf(
        Achievement("first_stand", R.string.ach_first_stand_title, R.string.ach_first_stand_desc, R.drawable.ic_sprout, BadgeTone.MINT),
        Achievement("stand_8", R.string.ach_stand_8_title, R.string.ach_stand_8_desc, R.drawable.ic_shoe_print, BadgeTone.SUNNY),
        Achievement("stand_12", R.string.ach_stand_12_title, R.string.ach_stand_12_desc, R.drawable.ic_bolt, BadgeTone.CORAL),
        Achievement("streak_3", R.string.ach_streak_3_title, R.string.ach_streak_3_desc, R.drawable.ic_fire, BadgeTone.SUNNY),
        Achievement("streak_7", R.string.ach_streak_7_title, R.string.ach_streak_7_desc, R.drawable.ic_calendar, BadgeTone.INDIGO),
        Achievement("streak_14", R.string.ach_streak_14_title, R.string.ach_streak_14_desc, R.drawable.ic_shield_check, BadgeTone.MINT),
        Achievement("streak_30", R.string.ach_streak_30_title, R.string.ach_streak_30_desc, R.drawable.ic_crown, BadgeTone.SUNNY),
        Achievement("light_day", R.string.ach_light_day_title, R.string.ach_light_day_desc, R.drawable.ic_feather, BadgeTone.INDIGO),
        Achievement("century", R.string.ach_century_title, R.string.ach_century_desc, R.drawable.ic_trophy, BadgeTone.CORAL),
        Achievement("total_500", R.string.ach_total_500_title, R.string.ach_total_500_desc, R.drawable.ic_medal, BadgeTone.MINT),
        Achievement("weekend_warrior", R.string.ach_weekend_title, R.string.ach_weekend_desc, R.drawable.ic_heart, BadgeTone.CORAL),
        Achievement("tamer_7", R.string.ach_tamer_title, R.string.ach_tamer_desc, R.drawable.ic_heart_pulse, BadgeTone.MINT),
    )

    /** 最长连续有站立的天数 */
    fun longestStreak(history: List<DailyStat>): Int {
        var best = 0
        var cur = 0
        var prev: LocalDate? = null
        for (day in history.sortedBy { it.date }) {
            if (day.standCount <= 0) {
                cur = 0
                prev = null
                continue
            }
            val date = parseDate(day.date) ?: continue
            // 按日历日判断相邻，不能用固定 86400000 毫秒：夏令时切换那天只有 23 小时
            cur = if (prev?.plusDays(1) == date) cur + 1 else 1
            if (cur > best) best = cur
            prev = date
        }
        return best
    }

    /** 当前连续打卡（从最近一天往前数） */
    fun currentStreak(history: List<DailyStat>, today: LocalDate = LocalDate.now()): Int {
        val byDate = history.associateBy { it.date }
        fun hasStand(date: LocalDate) = (byDate[date.toString()]?.standCount ?: 0) > 0

        // 今天尚未打卡不打断连续（以昨天为起点）
        var cursor = if (hasStand(today)) today else today.minusDays(1)
        var streak = 0
        while (hasStand(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /**
     * 周末达成度：历史上最好的一个周末，只有一天打卡记 0.5，周六周日都有记 1。
     */
    fun weekendCoverage(history: List<DailyStat>): Float {
        val stands = history.asSequence()
            .filter { it.standCount > 0 }
            .mapNotNull { parseDate(it.date) }
            .toSet()
        var best = 0f
        stands.forEach { d ->
            when (d.dayOfWeek) {
                DayOfWeek.SATURDAY -> best = maxOf(best, if (d.plusDays(1) in stands) 1f else 0.5f)
                // 周日单独出现也记半程，配合周六的情况已在上面覆盖
                DayOfWeek.SUNDAY -> if (d.minusDays(1) !in stands) best = maxOf(best, 0.5f)
                else -> Unit
            }
        }
        return best
    }

    /** 「久坐驯服师」的达标天数：真实追踪过且单日久坐少于 8 小时的天数（不必连续） */
    fun tamerDays(history: List<DailyStat>): Int =
        history.count { it.reminderCount > 0 && it.sedentaryMinutes < TAMER_DAY_MINUTES }

    private fun parseDate(value: String): LocalDate? =
        try {
            LocalDate.parse(value)
        } catch (e: DateTimeParseException) {
            null
        }

    fun evaluate(history: List<DailyStat>, totalStands: Int): List<AchievementState> {
        val bestStreak = longestStreak(history)
        val bestDayStands = history.maxOfOrNull { it.standCount } ?: 0
        val bestLightDay = history.filter { it.reminderCount > 0 }.minOfOrNull { it.sedentaryMinutes }
        val weekend = weekendCoverage(history)
        val tamer = tamerDays(history)

        fun state(id: String, unlocked: Boolean, progress: Float) =
            AchievementState(ALL.first { it.id == id }, unlocked, progress.coerceIn(0f, 1f))

        val lightDayUnlocked = bestLightDay != null && bestLightDay < LIGHT_DAY_MINUTES
        return listOf(
            state("first_stand", totalStands >= 1, totalStands / 1f),
            state("stand_8", bestDayStands >= 8, bestDayStands / 8f),
            state("stand_12", bestDayStands >= 12, bestDayStands / 12f),
            state("streak_3", bestStreak >= 3, bestStreak / 3f),
            state("streak_7", bestStreak >= 7, bestStreak / 7f),
            state("streak_14", bestStreak >= 14, bestStreak / 14f),
            state("streak_30", bestStreak >= 30, bestStreak / 30f),
            // 久坐越少越接近达成，进度按"离 6 小时还差多少"折算
            state(
                "light_day",
                lightDayUnlocked,
                if (lightDayUnlocked) 1f
                else bestLightDay?.let { LIGHT_DAY_MINUTES / it.toFloat() } ?: 0f,
            ),
            state("century", totalStands >= 100, totalStands / 100f),
            state("total_500", totalStands >= 500, totalStands / 500f),
            state("weekend_warrior", weekend >= 1f, weekend),
            state("tamer_7", tamer >= 7, tamer / 7f),
        )
    }

    /** "轻盈的一天"的门槛：单日久坐少于 6 小时 */
    const val LIGHT_DAY_MINUTES = 360

    /** "久坐驯服师"的单日门槛：久坐少于 8 小时 */
    const val TAMER_DAY_MINUTES = 480
}
