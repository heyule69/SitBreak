package com.sitbreak.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sitbreak.domain.QuietWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sitbreak_prefs")

/** 提醒方式 */
enum class ReminderStyle { LIGHT, STANDARD, STRONG }

/** 用户身体资料 */
data class UserProfile(
    val age: Int,
    val heightCm: Int,
    val weightKg: Int,
)

/** 全局设置快照 */
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val profile: UserProfile? = null,
    val intervalMinutes: Int = 45,
    val intervalIsRecommended: Boolean = true,
    val style: ReminderStyle = ReminderStyle.LIGHT,
    val trackingEnabled: Boolean = false,
    val sessionBeginMillis: Long = 0L,
    val quietEnabled: Boolean = false,
    /** 自午夜起的分钟数，默认 22:00 */
    val quietStartMinute: Int = DEFAULT_QUIET_START,
    /** 默认 07:00 */
    val quietEndMinute: Int = DEFAULT_QUIET_END,
    val smartPauseEnabled: Boolean = false,
) {
    val quietWindow: QuietWindow
        get() = QuietWindow(quietEnabled, quietStartMinute, quietEndMinute)
}

const val DEFAULT_QUIET_START = 22 * 60
const val DEFAULT_QUIET_END = 7 * 60

class UserPrefs(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val AGE = intPreferencesKey("age")
        val HEIGHT = intPreferencesKey("height_cm")
        val WEIGHT = intPreferencesKey("weight_kg")
        val INTERVAL = intPreferencesKey("interval_minutes")
        val INTERVAL_IS_RECOMMENDED = booleanPreferencesKey("interval_is_recommended")
        val STYLE = stringPreferencesKey("reminder_style")
        val TRACKING = booleanPreferencesKey("tracking_enabled")
        val SESSION_BEGIN = longPreferencesKey("session_begin_millis")
        val QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
        val QUIET_START = intPreferencesKey("quiet_start_minute")
        val QUIET_END = intPreferencesKey("quiet_end_minute")
        val SMART_PAUSE = booleanPreferencesKey("smart_pause_enabled")
        val NOTIFIED_ACHIEVEMENTS = stringSetPreferencesKey("notified_achievements")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            onboardingCompleted = p[Keys.ONBOARDING_DONE] ?: false,
            profile = if (p[Keys.AGE] != null) UserProfile(
                age = p[Keys.AGE] ?: 30,
                heightCm = p[Keys.HEIGHT] ?: 170,
                weightKg = p[Keys.WEIGHT] ?: 65,
            ) else null,
            intervalMinutes = p[Keys.INTERVAL] ?: 45,
            intervalIsRecommended = p[Keys.INTERVAL_IS_RECOMMENDED] ?: true,
            style = p[Keys.STYLE]?.let { runCatching { ReminderStyle.valueOf(it) }.getOrNull() }
                ?: ReminderStyle.LIGHT,
            trackingEnabled = p[Keys.TRACKING] ?: false,
            sessionBeginMillis = p[Keys.SESSION_BEGIN] ?: 0L,
            quietEnabled = p[Keys.QUIET_ENABLED] ?: false,
            quietStartMinute = p[Keys.QUIET_START] ?: DEFAULT_QUIET_START,
            quietEndMinute = p[Keys.QUIET_END] ?: DEFAULT_QUIET_END,
            smartPauseEnabled = p[Keys.SMART_PAUSE] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    /** 完成引导：写入资料与确认后的间隔 */
    suspend fun completeOnboarding(profile: UserProfile, intervalMinutes: Int, isRecommended: Boolean) {
        context.dataStore.edit { p ->
            p[Keys.ONBOARDING_DONE] = true
            p[Keys.AGE] = profile.age
            p[Keys.HEIGHT] = profile.heightCm
            p[Keys.WEIGHT] = profile.weightKg
            p[Keys.INTERVAL] = intervalMinutes
            p[Keys.INTERVAL_IS_RECOMMENDED] = isRecommended
        }
    }

    /** 跳过引导兜底：写入默认资料与推荐值，服务照常提醒 */
    suspend fun completeOnboardingWithDefaults(defaults: UserProfile, recommendedInterval: Int) {
        completeOnboarding(defaults, recommendedInterval, isRecommended = true)
    }

    suspend fun setInterval(minutes: Int, isRecommended: Boolean) {
        context.dataStore.edit { it[Keys.INTERVAL] = minutes; it[Keys.INTERVAL_IS_RECOMMENDED] = isRecommended }
    }

    suspend fun setStyle(style: ReminderStyle) {
        context.dataStore.edit { it[Keys.STYLE] = style.name }
    }

    suspend fun setTracking(enabled: Boolean) {
        context.dataStore.edit { it[Keys.TRACKING] = enabled }
    }

    /** 开启新一轮久坐会话（站立确认后或开始追踪时） */
    suspend fun resetSession(nowMillis: Long) {
        context.dataStore.edit { it[Keys.SESSION_BEGIN] = nowMillis }
    }

    suspend fun setQuietHours(enabled: Boolean, startMinute: Int, endMinute: Int) {
        context.dataStore.edit {
            it[Keys.QUIET_ENABLED] = enabled
            it[Keys.QUIET_START] = startMinute
            it[Keys.QUIET_END] = endMinute
        }
    }

    suspend fun setSmartPause(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMART_PAUSE] = enabled }
    }

    /** 已发过祝贺通知的成就 ID，跨进程重启去重 */
    suspend fun notifiedAchievementIds(): Set<String> =
        context.dataStore.data.first()[Keys.NOTIFIED_ACHIEVEMENTS] ?: emptySet()

    suspend fun markAchievementsNotified(ids: Set<String>) {
        context.dataStore.edit { p ->
            p[Keys.NOTIFIED_ACHIEVEMENTS] = (p[Keys.NOTIFIED_ACHIEVEMENTS] ?: emptySet()) + ids
        }
    }

    /** 去重键是否已初始化；升级兜底时用来区分"从未写过"与"写过但为空" */
    suspend fun isAchievementDedupInitialised(): Boolean =
        context.dataStore.data.first().contains(Keys.NOTIFIED_ACHIEVEMENTS)
}
