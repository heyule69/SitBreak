package com.sitbreak.domain

import androidx.annotation.StringRes
import com.sitbreak.R
import com.sitbreak.data.UserProfile

/**
 * 智能推荐引擎：根据年龄 + BMI（身高/体重）推荐久坐提醒间隔。
 *
 * 依据：主流研究表明久坐 20~30 分钟后起身活动 2~3 分钟效果最佳，
 * 全天久坐应少于 9 小时；年长者与 BMI 偏高人群久坐风险更高，
 * 提醒应更频繁。BMI 采用中国成人标准（超重 24 / 肥胖 28）。
 *
 * 文案一律以资源 ID 返回：这里拿不到 Context，也不该决定用哪种语言。
 */
object RecommendEngine {

    const val MIN_INTERVAL = 15
    const val MAX_INTERVAL = 120
    const val RECOMMEND_MIN = 30
    const val RECOMMEND_MAX = 90

    /** 默认资料（用户跳过引导时兜底使用） */
    val DEFAULT_PROFILE = UserProfile(age = 30, heightCm = 170, weightKg = 65)

    fun bmi(profile: UserProfile): Double {
        val m = profile.heightCm / 100.0
        if (m <= 0) return 22.0
        return profile.weightKg / (m * m)
    }

    /** BMI 分类：0 偏瘦 / 1 正常 / 2 超重 / 3 肥胖 */
    fun bmiCategory(bmi: Double): Int = when {
        bmi < 18.5 -> 0
        bmi < 24.0 -> 1
        bmi < 28.0 -> 2
        else -> 3
    }

    @StringRes
    fun bmiLabelRes(bmi: Double): Int = when (bmiCategory(bmi)) {
        0 -> R.string.bmi_label_underweight
        1 -> R.string.bmi_label_normal
        2 -> R.string.bmi_label_overweight
        else -> R.string.bmi_label_obese
    }

    /** 年龄分段标签 */
    @StringRes
    fun ageLabelRes(age: Int): Int = when {
        age < 30 -> R.string.age_label_youth
        age <= 45 -> R.string.age_label_young_adult
        age <= 60 -> R.string.age_label_middle
        else -> R.string.age_label_senior
    }

    /**
     * 推荐提醒间隔（分钟）。
     * 基础 45min，按年龄与 BMI 调整，夹在 [RECOMMEND_MIN, RECOMMEND_MAX]。
     */
    fun recommend(profile: UserProfile): Int {
        val bmi = bmi(profile)
        var interval = 45
        interval += when {
            profile.age < 30 -> 10
            profile.age <= 45 -> 0
            profile.age <= 60 -> -5
            else -> -10
        }
        interval += when (bmiCategory(bmi)) {
            0 -> 5
            1 -> 0
            2 -> -5
            else -> -10
        }
        return interval.coerceIn(RECOMMEND_MIN, RECOMMEND_MAX)
    }

    /** 一句话建议（UI 展示用）：BMI 超重起就换成更紧的口吻 */
    @StringRes
    fun adviceRes(profile: UserProfile): Int =
        if (bmiCategory(bmi(profile)) >= 2) R.string.advice_high_risk else R.string.advice_keep_going
}
