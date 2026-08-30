package com.sitbreak.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sitbreak.MainActivity
import com.sitbreak.R
import com.sitbreak.SitBreakApp
import com.sitbreak.service.ReminderActions
import com.sitbreak.util.AppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 桌面小组件：不打开应用也能看到倒计时并一键打卡。
 *
 * 走 RemoteViews 而非 Glance，避免为一个静态卡片引入整套 Compose Glance 依赖。
 */
class SitBreakWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // confirmStand 内部会回头调 refresh，这里不需要再手动渲染一次
            ACTION_STAND -> asyncWork { ReminderActions.confirmStand(context) }
            // 拦截系统与自身发起的更新广播，统一在协程里读数据后渲染
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> asyncWork { renderAll(context) }
            else -> super.onReceive(context, intent)
        }
    }

    /**
     * BroadcastReceiver 生命周期很短，用 goAsync 保住进程直到协程结束。
     *
     * 必须吞掉异常：部分国产 ROM 会拦截后台的前台服务启动，一旦这里抛出去，
     * 未捕获的协程异常会直接杀进程 —— 用户看到的就是"点了没反应"。
     */
    private fun asyncWork(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun renderAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SitBreakWidget::class.java))
        if (ids.isEmpty()) return

        val app = context.applicationContext as SitBreakApp
        val s = app.prefs.current()
        val now = System.currentTimeMillis()
        val views = RemoteViews(context.packageName, R.layout.widget_sitbreak)
        // 小组件的 Context 不跟随应用内语言选择，取文案得用换过语言的
        val c = AppLocale.wrap(context)

        if (s.trackingEnabled && s.sessionBeginMillis > 0) {
            val interval = s.intervalMinutes.coerceAtLeast(1)
            val satMinutes = (now - s.sessionBeginMillis) / 60_000L
            val remain = interval - (satMinutes % interval)
            views.setTextViewText(R.id.widget_state, c.getString(R.string.state_tracking))
            views.setTextViewText(R.id.widget_countdown, remain.toString())
            views.setTextViewText(
                R.id.widget_sub,
                c.getString(R.string.widget_sub_remain, satMinutes),
            )
            views.setTextViewText(R.id.widget_stand, c.getString(R.string.action_stand))
            views.setOnClickPendingIntent(R.id.widget_stand, standIntent(context))
        } else {
            views.setTextViewText(R.id.widget_state, c.getString(R.string.state_paused))
            views.setTextViewText(R.id.widget_countdown, "—")
            views.setTextViewText(R.id.widget_sub, c.getString(R.string.widget_sub_paused))
            views.setTextViewText(R.id.widget_stand, c.getString(R.string.widget_action_open))
            views.setOnClickPendingIntent(R.id.widget_stand, openAppIntent(context))
        }
        // 根布局兜底：子控件没绑点击的区域（图标、副标题、留白）点上去也要有反应，
        // 否则 3x2 格子里大半面积是死区，用户的感觉就是"小组件点不动"
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_countdown, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_state, openAppIntent(context))

        manager.updateAppWidget(ids, views)
    }

    private fun standIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_STAND,
            Intent(context, SitBreakWidget::class.java).setAction(ACTION_STAND),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            // 从桌面点击没有 Activity 栈可依附，必须显式开新任务（singleTask 保证不重复实例）
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val ACTION_STAND = "com.sitbreak.widget.ACTION_STAND"
        private const val REQUEST_STAND = 3001
        private const val REQUEST_OPEN = 3002

        /** 状态变化后主动刷新所有小组件实例 */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SitBreakWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, SitBreakWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }
}
