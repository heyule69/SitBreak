package com.sitbreak.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sitbreak.SitBreakApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 开机自启：若之前在追踪则恢复提醒服务 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as SitBreakApp
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val s = app.prefs.current()
                if (s.onboardingCompleted && s.trackingEnabled) {
                    ReminderForegroundService.start(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
