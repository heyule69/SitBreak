package com.sitbreak.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sitbreak.SitBreakApp
import com.sitbreak.data.UserPrefs
import com.sitbreak.data.UserProfile
import com.sitbreak.domain.RecommendEngine
import com.sitbreak.service.ReminderActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val age: Int = 30,
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val intervalMinutes: Int = 45,
    val intervalTouched: Boolean = false,
    val done: Boolean = false,
) {
    val profile: UserProfile get() = UserProfile(age, heightCm, weightKg)
    val recommended: Int get() = RecommendEngine.recommend(profile)
}

class OnboardingViewModel(private val app: SitBreakApp) : ViewModel() {

    private val prefs: UserPrefs get() = app.prefs

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state

    fun nextStep() = _state.update {
        when (it.step) {
            0 -> it.copy(step = 1)
            1 -> it.copy(step = 2, intervalMinutes = it.recommended, intervalTouched = false)
            else -> it
        }
    }

    fun prevStep() = _state.update { if (it.step > 0) it.copy(step = it.step - 1) else it }

    fun setAge(v: Int) = _state.update { it.copy(age = v.coerceIn(12, 100)) }
    fun setHeight(v: Int) = _state.update { it.copy(heightCm = v.coerceIn(120, 220)) }
    fun setWeight(v: Int) = _state.update { it.copy(weightKg = v.coerceIn(30, 200)) }
    fun setInterval(v: Int) = _state.update {
        it.copy(intervalMinutes = v.coerceIn(RecommendEngine.MIN_INTERVAL, RecommendEngine.MAX_INTERVAL), intervalTouched = true)
    }

    /** 完成引导：保存资料与间隔，并立即开始追踪 */
    fun complete() {
        val s = _state.value
        viewModelScope.launch {
            prefs.completeOnboarding(
                profile = s.profile,
                intervalMinutes = s.intervalMinutes,
                isRecommended = !s.intervalTouched,
            )
            ReminderActions.startTracking(app)
            _state.update { it.copy(done = true) }
        }
    }

    /** 跳过引导兜底：使用默认资料与智能推荐值，服务照常提醒 */
    fun skip() {
        viewModelScope.launch {
            val defaults = RecommendEngine.DEFAULT_PROFILE
            prefs.completeOnboardingWithDefaults(defaults, RecommendEngine.recommend(defaults))
            ReminderActions.startTracking(app)
            _state.update { it.copy(done = true) }
        }
    }

    companion object {
        fun factory(app: SitBreakApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                OnboardingViewModel(app) as T
        }
    }
}
