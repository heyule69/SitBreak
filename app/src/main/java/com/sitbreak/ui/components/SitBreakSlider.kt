package com.sitbreak.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.R
import kotlin.math.roundToInt

private val ThumbSize = 24.dp
private val TrackHeight = 12.dp

/**
 * 品牌风格滑杆：胶囊渐变轨道 + 圆形旋钮，替代 M3 默认的细竖条样式。
 *
 * @param accent 品牌强调色，决定轨道渐变与旋钮描边
 * @param trackColor 未选中轨道底色
 * @param marker 可选参考刻度（0..1），用于标出推荐值所在位置
 * @param minLabel / maxLabel 轨道两端的量程说明
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitBreakSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    marker: Float? = null,
    minLabel: String? = null,
    maxLabel: String? = null,
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    // 拖动时旋钮放大，给出明确的手感反馈
    val thumbScale by animateFloatAsState(
        targetValue = if (dragged || pressed) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "thumbScale",
    )

    Column(modifier) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            interactionSource = interaction,
            thumb = {
                Box(
                    Modifier
                        .size(ThumbSize)
                        .scale(thumbScale)
                        .shadow(4.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(4.dp, accent, CircleShape)
                )
            },
            track = { Track(fraction, accent, trackColor, marker) },
        )
        if (minLabel != null || maxLabel != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                RangeLabel(minLabel)
                Spacer(Modifier.weight(1f))
                RangeLabel(maxLabel)
            }
        }
    }
}

@Composable
private fun Track(fraction: Float, accent: Color, trackColor: Color, marker: Float?) {
    val accentLight = lerp(accent, Color.White, 0.4f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(TrackHeight)
    ) {
        val h = size.height
        val corner = CornerRadius(h / 2f, h / 2f)
        // 旋钮中心只能在 [r, width - r] 内移动，轨道进度需按同一区间换算才能与旋钮对齐
        val r = ThumbSize.toPx() / 2f
        val activeEnd = r + fraction * (size.width - ThumbSize.toPx())

        drawRoundRect(color = trackColor, size = size, cornerRadius = corner)

        if (fraction > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(accentLight, accent), 0f, activeEnd),
                size = Size(activeEnd.coerceAtLeast(h), h),
                cornerRadius = corner,
            )
        }

        marker?.let { m ->
            val x = r + m.coerceIn(0f, 1f) * (size.width - ThumbSize.toPx())
            drawCircle(
                // 落在已选区间内时用白点，否则用淡色强调点，保证始终可见
                color = if (x <= activeEnd) Color.White.copy(alpha = 0.75f) else accent.copy(alpha = 0.35f),
                radius = h * 0.2f,
                center = Offset(x, h / 2f),
            )
        }
    }
}

@Composable
private fun RangeLabel(text: String?) {
    Text(
        text.orEmpty(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 滑杆 + 两侧 ±1 步进按钮：滑杆粗调（四舍五入取整），按钮精调（长按连发）。
 *
 * 单靠滑杆难以精确落在某个整数上：连续值 + toInt() 截断会让显示值系统性
 * 偏小一格，量程越宽越难命中。粗精双通道是健康类 App 的通行做法。
 */
@Composable
fun StepperSliderRow(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    marker: Float? = null,
    minLabel: String? = null,
    maxLabel: String? = null,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            iconRes = R.drawable.ic_minus,
            tint = accent,
            enabled = value > valueRange.start,
        ) { onValueChange((value - 1).coerceAtLeast(valueRange.start.toInt())) }
        SitBreakSlider(
            value = value.toFloat(),
            onValueChange = {
                onValueChange(it.roundToInt().coerceIn(valueRange.start.toInt(), valueRange.endInclusive.toInt()))
            },
            valueRange = valueRange,
            accent = accent,
            trackColor = trackColor,
            modifier = Modifier.weight(1f),
            marker = marker,
            minLabel = minLabel,
            maxLabel = maxLabel,
        )
        StepperButton(
            iconRes = R.drawable.ic_plus,
            tint = accent,
            enabled = value < valueRange.endInclusive,
        ) { onValueChange((value + 1).coerceAtMost(valueRange.endInclusive.toInt())) }
    }
}
