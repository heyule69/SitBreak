package com.sitbreak.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sitbreak.SitBreakApp
import com.sitbreak.data.AppSettings
import com.sitbreak.data.DailyStat
import com.sitbreak.domain.AchievementEngine
import com.sitbreak.service.ReminderActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
    val today: DailyStat = DailyStat(""),
    val streak: Int = 0,
    val nowMillis: Long = System.currentTimeMillis(),
    val showStandConfirm: Boolean = false,
) {
    val tracking: Boolean get() = settings.trackingEnabled

    /** 本轮已坐分钟 */
    val satMinutes: Long
        get() = if (tracking && settings.sessionBeginMillis > 0)
            (nowMillis - settings.sessionBeginMillis) / 60_000L else 0L

    /** 距下次提醒的秒数 */
    val nextRemindInSeconds: Long
        get() {
            if (!tracking) return settings.intervalMinutes * 60L
            val begin = if (settings.sessionBeginMillis > 0) settings.sessionBeginMillis else nowMillis
            val intervalSec = settings.intervalMinutes * 60L
            val elapsed = (nowMillis - begin) / 1000L
            return (intervalSec - elapsed % intervalSec).coerceAtLeast(0)
        }

    /** 倒计时环形进度 0..1 */
    val cycleProgress: Float
        get() {
            val total = settings.intervalMinutes * 60L
            if (total <= 0) return 0f
            return 1f - nextRemindInSeconds.toFloat() / total
        }
}

class HomeViewModel(private val app: SitBreakApp) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init {
        viewModelScope.launch {
            combine(app.prefs.settings, app.stats.observeToday()) { s, t -> s to t }
                .collect { (s, t) ->
                    _state.update { it.copy(settings = s, today = t ?: DailyStat("")) }
                }
        }
        // 每秒刷新时钟驱动倒计时
        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(nowMillis = System.currentTimeMillis()) }
                delay(1_000L)
            }
        }
        // 连续打卡天数
        viewModelScope.launch {
            app.stats.observeLastDays(60).collect { history ->
                _state.update { it.copy(streak = AchievementEngine.currentStreak(history)) }
            }
        }
        // 从通知点击进入时自动弹出站立确认
        viewModelScope.launch {
            app.standConfirmRequests.collect { requested ->
                if (requested) {
                    _state.update { it.copy(showStandConfirm = true) }
                    app.standConfirmRequests.value = false
                }
            }
        }
    }

    fun toggleTracking() {
        viewModelScope.launch {
            if (_state.value.tracking) {
                ReminderActions.pauseTracking(app)
            } else {
                ReminderActions.startTracking(app)
            }
        }
    }

    fun openStandConfirm() = _state.update { it.copy(showStandConfirm = true) }
    fun dismissStandConfirm() = _state.update { it.copy(showStandConfirm = false) }

    fun confirmStand() {
        viewModelScope.launch {
            ReminderActions.confirmStand(app)
            _state.update { it.copy(showStandConfirm = false) }
        }
    }

    companion object {
        fun factory(app: SitBreakApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(app) as T
        }
    }
}
