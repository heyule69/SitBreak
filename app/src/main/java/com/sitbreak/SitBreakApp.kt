package com.sitbreak

import android.app.Application
import android.content.Context
import com.sitbreak.data.StatsDatabase
import com.sitbreak.data.StatsRepository
import com.sitbreak.data.UserPrefs
import com.sitbreak.domain.AchievementEngine
import com.sitbreak.service.NotificationHelper
import com.sitbreak.util.AppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SitBreakApp : Application() {

    lateinit var prefs: UserPrefs
        private set
    lateinit var stats: StatsRepository
        private set

    /** 通知点击 → 打开站立确认弹窗的请求 */
    val standConfirmRequests = MutableStateFlow(false)

    /** 通知点击 → 需要切回首页（与站立确认分开，避免两个消费者互相抢占） */
    val navigateHomeRequests = MutableStateFlow(false)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        prefs = UserPrefs(this)
        stats = StatsRepository(StatsDatabase.build(this))
        NotificationHelper.ensureChannels(this)
        // 升级兜底：老版本没有成就去重记录，把已解锁的先静默标记为已通知，
        // 否则升级后第一次打卡会把历史成就连着重弹一遍
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (!prefs.isAchievementDedupInitialised()) {
                val history = stats.allHistory()
                val total = stats.totalStands()
                prefs.markAchievementsNotified(
                    AchievementEngine.evaluate(history, total)
                        .filter { it.unlocked }
                        .map { it.achievement.id }
                        .toSet(),
                )
            }
        }
    }
}
