package com.sitbreak.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 应用内语言切换。
 *
 * Android 13 起交给系统的 LocaleManager 托管，这样应用内的选择和系统设置里的
 * "应用语言"读写的是同一份数据，不会出现两边显示不一致；13 以下系统没有这个
 * 能力，自己存一份并在 attachBaseContext 时替换 Context。
 *
 * 语言存 SharedPreferences 而不是项目其他偏好用的 DataStore：attachBaseContext
 * 必须同步拿到结果，而 DataStore 只能挂起读。
 */
object AppLocale {

    /** 空串表示跟随系统语言 */
    const val FOLLOW_SYSTEM = ""

    const val ZH = "zh-CN"
    const val EN = "en"

    private const val PREF_NAME = "app_locale"
    private const val KEY_TAG = "tag"

    /** Android 13 起系统自带应用级语言能力 */
    private const val SYSTEM_MANAGED_SINCE = 33

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** 当前生效的语言 tag，空串代表跟随系统 */
    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= SYSTEM_MANAGED_SINCE) {
            val list = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (list.isEmpty) FOLLOW_SYSTEM else list[0].toLanguageTag()
        } else {
            prefs(context).getString(KEY_TAG, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        }

    /**
     * 切换语言。
     *
     * @return true 表示调用方需要自己重建界面。13+ 由系统重建并连带刷新通知、
     * 小组件，因此返回 false。
     */
    fun apply(context: Context, tag: String): Boolean {
        if (Build.VERSION.SDK_INT >= SYSTEM_MANAGED_SINCE) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag == FOLLOW_SYSTEM) LocaleList.getEmptyLocaleList()
                else LocaleList.forLanguageTags(tag)
            return false
        }
        prefs(context).edit().putString(KEY_TAG, tag).apply()
        return true
    }

    /**
     * 把 Context 换成当前语言的 Context。
     *
     * Service 和 AppWidget 拿到的 Context 不继承 Application 的覆写配置，所以这
     * 些地方取字符串之前都要过一次这里，否则通知和桌面小组件会停在系统语言。
     */
    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= SYSTEM_MANAGED_SINCE) return context // 系统已整体应用，无需覆写
        val tag = prefs(context).getString(KEY_TAG, FOLLOW_SYSTEM) ?: FOLLOW_SYSTEM
        if (tag == FOLLOW_SYSTEM) return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(config)
    }
}
