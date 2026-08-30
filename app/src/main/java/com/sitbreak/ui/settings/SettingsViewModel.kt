package com.sitbreak.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sitbreak.SitBreakApp
import com.sitbreak.data.AppSettings
import com.sitbreak.data.ReminderStyle
import com.sitbreak.domain.RecommendEngine
import com.sitbreak.service.ReminderActions
import com.sitbreak.service.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val exactAlarmAllowed: Boolean = true,
)

class SettingsViewModel(private val app: SitBreakApp) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        viewModelScope.launch {
            app.prefs.settings.collect { s ->
                _state.update { it.copy(settings = s, exactAlarmAllowed = ReminderScheduler.canExact(app)) }
            }
        }
    }

    fun setInterval(minutes: Int) {
        viewModelScope.launch {
            app.prefs.setInterval(minutes, isRecommended = false)
            ReminderActions.applySettingChanged(app)
        }
    }

    fun restoreRecommend() {
        viewModelScope.launch {
            val profile = app.prefs.current().profile ?: RecommendEngine.DEFAULT_PROFILE
            app.prefs.setInterval(RecommendEngine.recommend(profile), isRecommended = true)
            ReminderActions.applySettingChanged(app)
        }
    }

    fun setStyle(style: ReminderStyle) {
        viewModelScope.launch {
            app.prefs.setStyle(style)
            ReminderActions.applySettingChanged(app)
        }
    }

    fun setQuietEnabled(enabled: Boolean) {
        val s = _state.value.settings
        viewModelScope.launch {
            app.prefs.setQuietHours(enabled, s.quietStartMinute, s.quietEndMinute)
            // 开关一变，下一次提醒时间可能要顺延，重新排一次
            ReminderActions.applySettingChanged(app)
        }
    }

    fun setQuietRange(startMinute: Int, endMinute: Int) {
        viewModelScope.launch {
            app.prefs.setQuietHours(true, startMinute, endMinute)
            ReminderActions.applySettingChanged(app)
        }
    }

    fun setSmartPause(enabled: Boolean) {
        viewModelScope.launch {
            app.prefs.setSmartPause(enabled)
            ReminderActions.applySettingChanged(app)
        }
    }

    /** 通知权限变化后刷新（由界面回调触发） */
    fun refreshPermissions() {
        _state.update { it.copy(exactAlarmAllowed = ReminderScheduler.canExact(app)) }
    }

    fun recommendedMinutes(): Int =
        RecommendEngine.recommend(_state.value.settings.profile ?: RecommendEngine.DEFAULT_PROFILE)

    companion object {
        fun factory(app: SitBreakApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(app) as T
        }
    }
}
