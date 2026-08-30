package com.sitbreak.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.ui.theme.PlantHill
import kotlin.math.sin

// 插画固定色：不随深浅色换挡的平涂色（深底上同样成立）
private val StemGreen = Color(0xFF4CAF6D)
private val LeafGreen = Color(0xFF6BCF87)
private val LeafBright = Color(0xFF8AE0A0)
private val PotBody = Color(0xFFE5836B)
private val PotRim = Color(0xFFF09A82)
private val FaceInk = Color(0xFF53301C)
private val Blush = Color(0xFFFF8E8E)
private val WaterBlue = Color(0xFF7FC8F5)
private val ZzzGray = Color(0xFFC9B8A8)
private val SparkSunny = Color(0xFFFFB020)

/**
 * 盆栽小苗主视觉。设计稿坐标 176x160，画布放大到 196x178dp。
 *
 * 状态联动：
 * - [droop] 0..1，本轮久坐进度 —— 叶片随之从精神变蔫，久坐过久冒汗滴、嘴角变平
 * - [tracking] false 时闭眼睡觉冒 Zzz
 * - [standCount] 变大即打卡成功：盆栽从下往上弹起，水滴落下、星星炸开
 */
@Composable
fun PlantScene(
    droop: Float,
    tracking: Boolean,
    standCount: Int,
    modifier: Modifier = Modifier,
) {
    val droopAnim by animateFloatAsState(
        targetValue = if (tracking) droop.coerceIn(0f, 1f) else 0.08f,
        animationSpec = tween(900),
        label = "droop",
    )

    // 打卡弹跳：0 → 1，celebrate 是它的补集，驱动星星与水滴
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(standCount) {
        if (standCount > 0) {
            bounce.snapTo(0f)
            bounce.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f),
            )
        }
    }
    val celebrate = 1f - bounce.value

    // 呼吸摇摆：叶子随风轻摆
    val sway = rememberInfiniteTransition(label = "sway")
    val swayT by sway.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "swayT",
    )
    val swayAngle = sin(swayT * 2f * Math.PI).toFloat() * 2.2f

    val textMeasurer = rememberTextMeasurer()

    Box(modifier.fillMaxWidth().height(200.dp)) {
        // 脚下奶油地台
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .size(264.dp, 92.dp)
                .offset(y = 26.dp)
                .clip(CircleShape)
                .background(PlantHill),
        )
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .size(196.dp, 178.dp)
                .graphicsLayer {
                    // 弹跳只做“从地面压扁再弹起”
                    scaleY = 0.72f + 0.28f * bounce.value
                    scaleX = 1f + 0.06f * celebrate
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                },
        ) {
            val k = size.width / 176.dp.toPx()
            scale(k, pivot = Offset(size.width / 2f, size.height)) {
                drawPlant(droopAnim, tracking, celebrate, swayAngle, textMeasurer)
            }
        }
    }
}

/** 全部坐标基于 176x160 的设计稿（单位 dp） */
private fun DrawScope.drawPlant(
    droop: Float,
    tracking: Boolean,
    celebrate: Float,
    sway: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val u = 1.dp.toPx()
    fun x(v: Float) = v * u
    fun o(vx: Float, vy: Float) = Offset(vx * u, vy * u)

    // 地面阴影
    drawOval(
        color = Color(0xFF3C2814).copy(alpha = 0.10f),
        topLeft = o(36f, 145f),
        size = Size(x(104f), x(14f)),
    )

    // 茎
    drawLine(
        color = StemGreen,
        start = o(88f, 102f),
        end = o(88f, 60f + droop * 4f),
        strokeWidth = x(6f),
        cap = StrokeCap.Round,
    )

    // 三片叶子：久坐进度越大越下垂
    leaf(o(70f, 66f + droop * 5f), x(14f), x(8f), lerp(-34f, 26f, droop) + sway, LeafGreen)
    leaf(o(106f, 80f + droop * 5f), x(14f), x(8f), lerp(34f, -26f, droop) - sway, LeafGreen)
    leaf(
        o(88f, 46f + droop * 8f),
        x(8f),
        x(13f),
        lerp(0f, 22f, droop) + sway * 0.6f,
        LeafBright,
    )

    // 陶盆（圆角梯形）+ 盆沿
    val pot = Path().apply {
        moveTo(x(60f), x(100f))
        lineTo(x(116f), x(100f))
        lineTo(x(110f), x(138f))
        quadraticTo(x(88f), x(146f), x(66f), x(138f))
        close()
    }
    drawPath(pot, PotBody)
    drawRoundRect(
        color = PotRim,
        topLeft = o(54f, 90f),
        size = Size(x(68f), x(14f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(x(7f), x(7f)),
    )

    // 盆脸
    if (tracking) {
        drawCircle(FaceInk, x(2.6f), center = o(78f, 118f))
        drawCircle(FaceInk, x(2.6f), center = o(98f, 118f))
    } else {
        // 睡觉的闭眼（∩）
        drawArc(
            color = FaceInk,
            startAngle = 180f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = o(73f, 118f),
            size = Size(x(10f), x(7f)),
            style = Stroke(x(2.4f), cap = StrokeCap.Round),
        )
        drawArc(
            color = FaceInk,
            startAngle = 180f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = o(93f, 118f),
            size = Size(x(10f), x(7f)),
            style = Stroke(x(2.4f), cap = StrokeCap.Round),
        )
    }
    // 嘴：精神时上扬，蔫过头变平
    val sad = ((droop - 0.6f) / 0.4f).coerceIn(0f, 1f)
    val mouth = Path().apply {
        moveTo(x(82f), x(127f))
        quadraticTo(x(88f), x(lerp(133f, 125f, sad)), x(94f), x(127f))
    }
    drawPath(mouth, FaceInk, style = Stroke(x(2.4f), cap = StrokeCap.Round))
    // 腮红
    drawOval(Blush.copy(alpha = 0.5f), topLeft = o(70.5f, 122f), size = Size(x(7f), x(4.8f)))
    drawOval(Blush.copy(alpha = 0.5f), topLeft = o(98.5f, 122f), size = Size(x(7f), x(4.8f)))

    // 久坐快到点：冒汗滴
    val sweat = ((droop - 0.75f) / 0.25f).coerceIn(0f, 1f)
    if (sweat > 0f) {
        val drip = Path().apply {
            moveTo(x(124f), x(92f))
            quadraticTo(x(128f), x(98f), x(124f), x(102f))
            quadraticTo(x(120f), x(98f), x(124f), x(92f))
        }
        drawPath(drip, WaterBlue.copy(alpha = sweat))
    }

    // 睡觉冒 Zzz
    if (!tracking) {
        val zStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ZzzGray)
        drawText(textMeasurer, "z", style = zStyle, topLeft = o(126f, 62f))
        drawText(textMeasurer, "z", style = zStyle.copy(fontSize = 14.sp), topLeft = o(134f, 44f))
        drawText(textMeasurer, "z", style = zStyle.copy(fontSize = 17.sp), topLeft = o(142f, 24f))
    }

    // 打卡庆祝：水滴落下 + 星星
    if (celebrate > 0.02f) {
        val dropY = 30f + (1f - celebrate) * 52f
        val falling = Path().apply {
            moveTo(x(88f), x(dropY))
            quadraticTo(x(93f), x(dropY + 6f), x(88f), x(dropY + 10f))
            quadraticTo(x(83f), x(dropY + 6f), x(88f), x(dropY))
        }
        drawPath(falling, WaterBlue.copy(alpha = (celebrate * 1.4f).coerceAtMost(1f)))
        spark(o(40f, 66f), x(7f), SparkSunny, celebrate)
        spark(o(136f, 56f), x(6f), LeafGreen, celebrate)
        spark(o(130f, 116f), x(5f), WaterBlue, celebrate)
    }
}

private fun DrawScope.leaf(center: Offset, rx: Float, ry: Float, angleDeg: Float, color: Color) {
    rotate(angleDeg, pivot = center) {
        drawOval(color, topLeft = Offset(center.x - rx, center.y - ry), size = Size(rx * 2, ry * 2))
    }
}

private fun DrawScope.spark(center: Offset, r: Float, color: Color, alpha: Float) {
    val p = Path().apply {
        moveTo(center.x, center.y - r)
        lineTo(center.x + r, center.y)
        lineTo(center.x, center.y + r)
        lineTo(center.x - r, center.y)
        close()
    }
    drawPath(p, color.copy(alpha = alpha))
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
