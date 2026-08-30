package com.sitbreak.domain

import com.sitbreak.R
import com.sitbreak.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendEngineTest {

    private fun profile(age: Int, heightCm: Int = 170, weightKg: Int = 65) =
        UserProfile(age = age, heightCm = heightCm, weightKg = weightKg)

    @Test
    fun `bmi is computed from height in metres`() {
        assertEquals(22.49, RecommendEngine.bmi(profile(30, 170, 65)), 0.01)
    }

    @Test
    fun `zero height falls back instead of dividing by zero`() {
        assertEquals(22.0, RecommendEngine.bmi(profile(30, 0, 65)), 0.001)
    }

    @Test
    fun `bmi categories follow the chinese adult thresholds`() {
        assertEquals(0, RecommendEngine.bmiCategory(18.4))
        assertEquals(1, RecommendEngine.bmiCategory(18.5))
        assertEquals(1, RecommendEngine.bmiCategory(23.9))
        assertEquals(2, RecommendEngine.bmiCategory(24.0))
        assertEquals(2, RecommendEngine.bmiCategory(27.9))
        assertEquals(3, RecommendEngine.bmiCategory(28.0))
    }

    @Test
    fun `young and slim users get the longest interval`() {
        // 45 基础 + 10（<30 岁）+ 5（偏瘦）
        assertEquals(60, RecommendEngine.recommend(profile(25, 180, 55)))
    }

    @Test
    fun `middle aged normal bmi keeps the baseline`() {
        assertEquals(45, RecommendEngine.recommend(profile(40, 170, 65)))
    }

    @Test
    fun `elderly and obese users are reminded more often but never below the floor`() {
        // 45 - 10（>60 岁）- 10（肥胖）= 25，被夹到 RECOMMEND_MIN
        assertEquals(RecommendEngine.RECOMMEND_MIN, RecommendEngine.recommend(profile(70, 165, 90)))
    }

    @Test
    fun `recommendation always stays inside the recommend range`() {
        for (age in 15..90 step 5) {
            for (weight in 40..130 step 10) {
                val value = RecommendEngine.recommend(profile(age, 170, weight))
                assertTrue(
                    "age=$age weight=$weight gave $value",
                    value in RecommendEngine.RECOMMEND_MIN..RecommendEngine.RECOMMEND_MAX,
                )
            }
        }
    }

    @Test
    fun `recommend range sits inside the slider range`() {
        assertTrue(RecommendEngine.MIN_INTERVAL <= RecommendEngine.RECOMMEND_MIN)
        assertTrue(RecommendEngine.RECOMMEND_MAX <= RecommendEngine.MAX_INTERVAL)
    }

    @Test
    fun `default profile is usable when onboarding is skipped`() {
        assertEquals(45, RecommendEngine.recommend(RecommendEngine.DEFAULT_PROFILE))
    }

    @Test
    fun `advice switches to the urgent wording once bmi is overweight`() {
        assertEquals(R.string.advice_high_risk, RecommendEngine.adviceRes(profile(50, 170, 80)))
        assertEquals(R.string.advice_keep_going, RecommendEngine.adviceRes(profile(30, 170, 65)))
    }

    @Test
    fun `bmi labels cover every category`() {
        assertEquals(R.string.bmi_label_underweight, RecommendEngine.bmiLabelRes(18.4))
        assertEquals(R.string.bmi_label_normal, RecommendEngine.bmiLabelRes(22.0))
        assertEquals(R.string.bmi_label_overweight, RecommendEngine.bmiLabelRes(24.0))
        assertEquals(R.string.bmi_label_obese, RecommendEngine.bmiLabelRes(28.0))
    }

    @Test
    fun `age labels cover every boundary`() {
        assertEquals(R.string.age_label_youth, RecommendEngine.ageLabelRes(29))
        assertEquals(R.string.age_label_young_adult, RecommendEngine.ageLabelRes(30))
        assertEquals(R.string.age_label_young_adult, RecommendEngine.ageLabelRes(45))
        assertEquals(R.string.age_label_middle, RecommendEngine.ageLabelRes(46))
        assertEquals(R.string.age_label_middle, RecommendEngine.ageLabelRes(60))
        assertEquals(R.string.age_label_senior, RecommendEngine.ageLabelRes(61))
    }
}
