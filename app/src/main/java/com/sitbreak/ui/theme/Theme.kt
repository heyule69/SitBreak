package com.sitbreak.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 品牌色：珊瑚橙 主色 / 薄荷绿 达成 / 暖黄 提示 / 靛蓝 免打扰
private val CoralLight = Color(0xFFFF6B4A)
private val CoralSoftLight = Color(0xFFFFEDE6)
private val CoralDeepLight = Color(0xFFE8532F)
private val CoralGlowLight = Color(0xFFFF9A62)
private val MintLight = Color(0xFF2BC0A4)
private val MintSoftLight = Color(0xFFDDF6F0)
private val MintGlowLight = Color(0xFF7FE3CE)
private val SunnyLight = Color(0xFFFFB020)
private val SunnySoftLight = Color(0xFFFFF3D6)
private val SunnyInkLight = Color(0xFF6B5300)
private val IndigoLight = Color(0xFF5B6ABF)
private val IndigoSoftLight = Color(0xFFE8EAFA)
private val InkBlackLight = Color(0xFF23201E)
private val InkGrayLight = Color(0xFF8A8580)
private val WarmBgLight = Color(0xFFFFF9F5)
private val CardWhiteLight = Color(0xFFFFFFFF)
private val OnSoftContainerLight = Color(0xFF665148)

// 深色版：主色提亮保证对比度，Soft 容器色翻成同色系低明度底，
// 叠在容器上的深色文字则要反过来提亮，否则深底深字直接看不见。
private val CoralDark = Color(0xFFFF8A66)
private val CoralSoftDark = Color(0xFF4B2015)
private val CoralDeepDark = Color(0xFFFFB59F)
private val CoralGlowDark = Color(0xFFD9713C)
private val MintDark = Color(0xFF4DD8BE)
private val MintSoftDark = Color(0xFF14544A)
private val MintGlowDark = Color(0xFF2A9C85)
private val SunnyDark = Color(0xFFFFC64D)
private val SunnySoftDark = Color(0xFF463514)
private val SunnyInkDark = Color(0xFFFFE0A3)
private val IndigoDark = Color(0xFF9BA7F0)
private val IndigoSoftDark = Color(0xFF2A2F4D)
private val InkBlackDark = Color(0xFFECE7E2)
private val InkGrayDark = Color(0xFFA6A09A)
private val WarmBgDark = Color(0xFF14161A)
private val CardWhiteDark = Color(0xFF1C1F24)
private val OnSoftContainerDark = Color(0xFFE6D8D1)

/**
 * 品牌色不属于 Material 的 ColorScheme，但同样要跟随深浅色切换。
 * 用一个 CompositionLocal 承载当前档位，界面里继续写 `Coral`、`InkGray` 即可，
 * 无需把上百处调用改成 `MaterialTheme.xxx`。
 */
private val LocalDarkPalette = staticCompositionLocalOf { false }

private inline fun pick(dark: Boolean, light: Color, darkColor: Color) = if (dark) darkColor else light

val Coral: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, CoralLight, CoralDark)
val CoralSoft: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, CoralSoftLight, CoralSoftDark)
val CoralDeep: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, CoralDeepLight, CoralDeepDark)

/** 主卡渐变的第二个色标，必须跟着 Coral 一起变，否则深色下两端撕裂 */
val CoralGlow: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, CoralGlowLight, CoralGlowDark)
val Mint: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, MintLight, MintDark)
val MintSoft: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, MintSoftLight, MintSoftDark)
val MintGlow: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, MintGlowLight, MintGlowDark)
val Sunny: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, SunnyLight, SunnyDark)
val SunnySoft: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, SunnySoftLight, SunnySoftDark)

/** 写在 SunnySoft 底上的文字色 */
val SunnyInk: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, SunnyInkLight, SunnyInkDark)
val Indigo: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, IndigoLight, IndigoDark)
val IndigoSoft: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, IndigoSoftLight, IndigoSoftDark)
val InkBlack: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, InkBlackLight, InkBlackDark)
val InkGray: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, InkGrayLight, InkGrayDark)
val WarmBg: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, WarmBgLight, WarmBgDark)
val CardWhite: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, CardWhiteLight, CardWhiteDark)

/** 写在任意 Soft 容器底上的次级文字色 */
val OnSoftContainer: Color
    @Composable @ReadOnlyComposable get() = pick(LocalDarkPalette.current, OnSoftContainerLight, OnSoftContainerDark)

private val LightColors = lightColorScheme(
    primary = CoralLight,
    onPrimary = Color.White,
    primaryContainer = CoralSoftLight,
    onPrimaryContainer = CoralDeepLight,
    secondary = MintLight,
    onSecondary = Color.White,
    secondaryContainer = MintSoftLight,
    onSecondaryContainer = Color(0xFF0B6B59),
    tertiary = SunnyLight,
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = SunnySoftLight,
    onTertiaryContainer = Color(0xFF5C4700),
    background = WarmBgLight,
    onBackground = InkBlackLight,
    surface = CardWhiteLight,
    onSurface = InkBlackLight,
    surfaceVariant = Color(0xFFF4EFEA),
    onSurfaceVariant = InkGrayLight,
    outline = Color(0xFFE5DED8),
    error = Color(0xFFE04B3A),
)

private val DarkColors = darkColorScheme(
    primary = CoralDark,
    onPrimary = Color(0xFF3D0E00),
    primaryContainer = CoralSoftDark,
    onPrimaryContainer = Color(0xFFFFDBD1),
    secondary = MintDark,
    onSecondary = Color(0xFF00382E),
    secondaryContainer = MintSoftDark,
    onSecondaryContainer = Color(0xFFB9F1E5),
    tertiary = SunnyDark,
    background = WarmBgDark,
    onBackground = InkBlackDark,
    surface = CardWhiteDark,
    onSurface = InkBlackDark,
    surfaceVariant = Color(0xFF26292F),
    onSurfaceVariant = InkGrayDark,
    outline = Color(0xFF3A3D43),
    error = Color(0xFFFF7A6B),
)

@Composable
fun SitBreakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDarkPalette provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
