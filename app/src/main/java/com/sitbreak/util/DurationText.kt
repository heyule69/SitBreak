package com.sitbreak.util

import android.content.Context
import com.sitbreak.R

/**
 * 时长文案。
 *
 * 通知栏、首页小卡、弹窗都要显示"已坐 1 小时 20 分"，
 * 而中英文的单位与语序并不一致，模板统一放在资源里，代码只负责挑模板。
 */
object DurationText {

    /** 完整写法，用在句子里：1 小时 20 分 / 1 h 20 min */
    fun full(context: Context, minutes: Long): String =
        if (minutes < 60) {
            context.getString(R.string.duration_minutes, minutes.toInt())
        } else {
            context.getString(
                R.string.duration_hours_minutes,
                (minutes / 60).toInt(),
                (minutes % 60).toInt(),
            )
        }

    /** 紧凑写法，用在窄卡片里：45 分 / 3h 32m */
    fun compact(context: Context, minutes: Int): String = when {
        minutes < 60 -> context.getString(R.string.duration_minutes_short, minutes)
        minutes % 60 == 0 -> context.getString(R.string.duration_hours_short, minutes / 60)
        else -> context.getString(R.string.duration_hours_minutes_short, minutes / 60, minutes % 60)
    }
}
