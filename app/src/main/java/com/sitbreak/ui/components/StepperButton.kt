package com.sitbreak.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 数值步进按钮：点按 ±1，长按 400ms 后连发。
 *
 * 连发由按压状态驱动：短按撑不到 400ms，循环被取消，只有 onClick 生效一次；
 * 长按则循环持续触发。这样一次操作不会加两次。
 *
 * 视觉是软色圆钮（同全 App 的图标气泡语言），不再是裸 IconButton —— 裸图标
 * 挨着滑杆放没有视觉锚点，观感像没画完。
 */
@Composable
fun StepperButton(
    iconRes: Int,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onStep: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed, enabled) {
        if (pressed && enabled) {
            delay(400)
            while (true) {
                onStep()
                delay(60)
            }
        }
    }
    Surface(
        onClick = onStep,
        modifier = modifier.size(36.dp).alpha(if (enabled) 1f else 0.35f),
        enabled = enabled,
        shape = CircleShape,
        color = container,
        interactionSource = interaction,
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}
