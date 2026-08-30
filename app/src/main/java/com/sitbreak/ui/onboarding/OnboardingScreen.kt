package com.sitbreak.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sitbreak.R
import com.sitbreak.SitBreakApp
import com.sitbreak.domain.RecommendEngine
import com.sitbreak.ui.components.StepperSliderRow
import com.sitbreak.ui.theme.Coral
import com.sitbreak.ui.theme.CoralGlow
import com.sitbreak.ui.theme.CoralSoft
import com.sitbreak.ui.theme.Mint
import com.sitbreak.ui.theme.MintSoft
import com.sitbreak.ui.theme.Sunny
import com.sitbreak.ui.theme.SunnySoft

@Composable
fun OnboardingRoute(app: SitBreakApp, onDone: () -> Unit) {
    val vm: OnboardingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = OnboardingViewModel.factory(app),
    )
    val state by vm.state.collectAsState()
    if (state.done) onDone()
    OnboardingScreen(state, vm::nextStep, vm::prevStep, vm::setAge, vm::setHeight, vm::setWeight, vm::setInterval, vm::complete, vm::skip)
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onAge: (Int) -> Unit,
    onHeight: (Int) -> Unit,
    onWeight: (Int) -> Unit,
    onInterval: (Int) -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        StepIndicator(state.step)
        Spacer(Modifier.height(20.dp))
        when (state.step) {
            0 -> WelcomeStep(onNext, onSkip)
            1 -> BodyInfoStep(state, onAge, onHeight, onWeight, onBack, onNext)
            else -> RecommendStep(state, onInterval, onComplete)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val active = i <= step
            Box(
                Modifier
                    .size(width = if (i == step) 28.dp else 10.dp, height = 10.dp)
                    .clip(CircleShape)
                    .background(if (active) Coral else CoralSoft),
            )
            if (i < 2) Spacer(Modifier.size(6.dp))
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, onSkip: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(32.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.img_hero_onboarding),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to Color(0xB323201E),
                    )
                ),
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FeatureChip(stringResource(R.string.ob_chip_smart), R.drawable.ic_bulb, Sunny)
            FeatureChip(stringResource(R.string.ob_chip_light), R.drawable.ic_vibrate, Mint)
            FeatureChip(stringResource(R.string.ob_chip_achieve), R.drawable.ic_trophy, Coral)
        }
    }
    Spacer(Modifier.height(28.dp))
    Text(
        "SitBreak",
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Coral,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.ob_welcome_desc),
        fontSize = 16.sp,
        lineHeight = 26.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    BigButton(stringResource(R.string.ob_cta_start), onNext)
    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ob_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FeatureChip(label: String, icon: Int, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xF0FFFFFF))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF23201E))
    }
}

@Composable
private fun BodyInfoStep(
    state: OnboardingUiState,
    onAge: (Int) -> Unit,
    onHeight: (Int) -> Unit,
    onWeight: (Int) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Text(stringResource(R.string.ob_body_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(6.dp))
    Text(
        stringResource(R.string.ob_body_desc),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    SliderCard(
        icon = R.drawable.ic_calendar,
        label = stringResource(R.string.label_age),
        value = state.age,
        unit = stringResource(R.string.unit_years),
        range = 12f..100f,
        tint = Sunny,
        container = SunnySoft,
        onChange = onAge,
    )
    Spacer(Modifier.height(16.dp))
    SliderCard(
        icon = R.drawable.ic_height,
        label = stringResource(R.string.label_height),
        value = state.heightCm,
        unit = stringResource(R.string.unit_cm),
        range = 120f..220f,
        tint = Coral,
        container = CoralSoft,
        onChange = onHeight,
    )
    Spacer(Modifier.height(16.dp))
    SliderCard(
        icon = R.drawable.ic_weight,
        label = stringResource(R.string.label_weight),
        value = state.weightKg,
        unit = stringResource(R.string.unit_kg),
        range = 30f..200f,
        tint = Mint,
        container = MintSoft,
        onChange = onWeight,
    )
    Spacer(Modifier.height(28.dp))
    BigButton(stringResource(R.string.ob_cta_next), onNext)
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_back), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SliderCard(
    icon: Int,
    label: String,
    value: Int,
    unit: String,
    range: ClosedFloatingPointRange<Float>,
    tint: Color,
    container: Color,
    onChange: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(container),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.size(12.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text(value.toString(), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = tint)
                Spacer(Modifier.size(4.dp))
                Text(unit, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            StepperSliderRow(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                accent = tint,
                trackColor = container,
                minLabel = "${range.start.toInt()}",
                maxLabel = "${range.endInclusive.toInt()}",
            )
        }
    }
}

@Composable
private fun RecommendStep(
    state: OnboardingUiState,
    onInterval: (Int) -> Unit,
    onComplete: () -> Unit,
) {
    val isCustom = state.intervalTouched
    val bmi = RecommendEngine.bmi(state.profile)
    Text(stringResource(R.string.ob_plan_title), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(6.dp))
    Text(
        stringResource(R.string.ob_plan_desc),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(Coral, CoralGlow)))
            .padding(28.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_handsup), null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(if (isCustom) R.string.ob_rhythm_custom else R.string.ob_rhythm_smart),
                    color = Color(0xFFB3FFE9),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${state.intervalMinutes}", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.ob_minutes_per_break),
                    fontSize = 16.sp,
                    color = Color(0xFFFFE3DA),
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReasonChip(
                    stringResource(
                        R.string.ob_chip_age,
                        stringResource(RecommendEngine.ageLabelRes(state.age)),
                        state.age,
                    ),
                )
                ReasonChip(
                    stringResource(
                        R.string.ob_chip_bmi,
                        bmi.toInt(),
                        stringResource(RecommendEngine.bmiLabelRes(bmi)),
                    ),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(RecommendEngine.adviceRes(state.profile)),
                fontSize = 12.sp,
                color = Color(0xFFFFE3DA),
                textAlign = TextAlign.Center,
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.ob_drag_hint),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(
                R.string.range_minutes,
                RecommendEngine.MIN_INTERVAL,
                RecommendEngine.MAX_INTERVAL,
            ),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    StepperSliderRow(
        value = state.intervalMinutes,
        onValueChange = onInterval,
        valueRange = RecommendEngine.MIN_INTERVAL.toFloat()..RecommendEngine.MAX_INTERVAL.toFloat(),
        accent = Coral,
        trackColor = CoralSoft,
        marker = (RecommendEngine.recommend(state.profile) - RecommendEngine.MIN_INTERVAL).toFloat() /
            (RecommendEngine.MAX_INTERVAL - RecommendEngine.MIN_INTERVAL).toFloat(),
    )
    Spacer(Modifier.height(16.dp))
    BigButton(stringResource(R.string.ob_cta_done), onComplete)
}

@Composable
private fun ReasonChip(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
internal fun BigButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Coral),
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
