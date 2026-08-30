package com.sitbreak.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sitbreak.SitBreakApp
import com.sitbreak.data.DailyStat
import com.sitbreak.domain.AchievementEngine
import com.sitbreak.domain.AchievementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StatsUiState(
    val last7Days: List<DailyStat> = emptyList(),
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val totalStands: Int = 0,
    val achievements: List<AchievementState> = emptyList(),
)

class StatsViewModel(private val app: SitBreakApp) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state

    init {
        viewModelScope.launch {
            app.stats.observeLastDays(7).collect { days ->
                _state.update { it.copy(last7Days = fillWeek(days)) }
            }
        }
        // 观察整表：打卡/提醒落库后连续天数与徽章立即刷新，
        // 否则 ViewModel 常驻期间统计页一直停在首屏算出的旧数据
        viewModelScope.launch {
            app.stats.observeAllHistory().collect { history ->
                val total = history.sumOf { it.standCount }
                _state.update {
                    it.copy(
                        streak = AchievementEngine.currentStreak(history),
                        bestStreak = AchievementEngine.longestStreak(history),
                        totalStands = total,
                        achievements = AchievementEngine.evaluate(history, total),
                    )
                }
            }
        }
    }

    /** 补全最近 7 天（无数据天填 0），保证图表恒为 7 列 */
    private fun fillWeek(days: List<DailyStat>): List<DailyStat> {
        val byDate = days.associateBy { it.date }
        // 按日历天往回数，不能按 86400000 毫秒减：夏令时切换的那天只有 23 小时，会漏一天
        val today = LocalDate.now()
        return (6 downTo 0).map { i ->
            val key = today.minusDays(i.toLong()).toString()
            byDate[key] ?: DailyStat(key)
        }
    }

    companion object {
        fun factory(app: SitBreakApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StatsViewModel(app) as T
        }
    }
}
