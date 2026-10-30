package com.productiveos.launcher.presentation.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.preferences.LauncherThemeOption
import com.productiveos.launcher.data.preferences.TypographyStyleOption
import kotlinx.coroutines.launch

@Immutable
data class ProductiveOSPalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val borderSubtle: Color,
    val accentDot: Color,
    val glowColor: Color,
    val isLight: Boolean
)

val OledBlackPalette = ProductiveOSPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF141414),
    primaryText = Color(0xFFF5F5F7),
    secondaryText = Color(0xFF8E8E93),
    mutedText = Color(0xFF525258),
    borderSubtle = Color(0xFF1F1F24),
    accentDot = Color(0xFFE5E5EA),
    glowColor = Color(0x66FFFFFF),
    isLight = false
)

val SlateGrayPalette = ProductiveOSPalette(
    background = Color(0xFF121212),
    surface = Color(0xFF1A1A1C),
    surfaceElevated = Color(0xFF242428),
    primaryText = Color(0xFFF2F2F7),
    secondaryText = Color(0xFF98989D),
    mutedText = Color(0xFF636366),
    borderSubtle = Color(0xFF2C2C2E),
    accentDot = Color(0xFFD1D1D6),
    glowColor = Color(0x55E5E5EA),
    isLight = false
)

val WarmStonePalette = ProductiveOSPalette(
    background = Color(0xFFF5F5F0),
    surface = Color(0xFFEBEBE4),
    surfaceElevated = Color(0xFFE2E2DA),
    primaryText = Color(0xFF141413),
    secondaryText = Color(0xFF5C5C57),
    mutedText = Color(0xFF8E8E87),
    borderSubtle = Color(0xFFD6D6CE),
    accentDot = Color(0xFF1C1C1E),
    glowColor = Color(0x55141413),
    isLight = true
)

// BLOCK: Plain Sleek Thin Colors Palettes (Editorial, Low-Fatigue, High Aesthetic Contrast)
val PaleSagePalette = ProductiveOSPalette(
    background = Color(0xFFEAEFE9),
    surface = Color(0xFFDFE6DE),
    surfaceElevated = Color(0xFFD5DED4),
    primaryText = Color(0xFF1B231D),
    secondaryText = Color(0xFF566258),
    mutedText = Color(0xFF828F84),
    borderSubtle = Color(0xFFC8D2C7),
    accentDot = Color(0xFF273429),
    glowColor = Color(0x44273429),
    isLight = true
)

val TokyoMistPalette = ProductiveOSPalette(
    background = Color(0xFFE9EEF3),
    surface = Color(0xFFDFE5EB),
    surfaceElevated = Color(0xFFD4DCE3),
    primaryText = Color(0xFF141A21),
    secondaryText = Color(0xFF515C67),
    mutedText = Color(0xFF7E8A96),
    borderSubtle = Color(0xFFC7D1DB),
    accentDot = Color(0xFF1D2630),
    glowColor = Color(0x441D2630),
    isLight = true
)

val OchreSandPalette = ProductiveOSPalette(
    background = Color(0xFFF5EFE6),
    surface = Color(0xFFEBE3D7),
    surfaceElevated = Color(0xFFE0D7C9),
    primaryText = Color(0xFF231F1A),
    secondaryText = Color(0xFF655D53),
    mutedText = Color(0xFF948A7D),
    borderSubtle = Color(0xFFD5CBBC),
    accentDot = Color(0xFF322C24),
    glowColor = Color(0x44322C24),
    isLight = true
)

val MidnightNavyPalette = ProductiveOSPalette(
    background = Color(0xFF0B101B),
    surface = Color(0xFF121826),
    surfaceElevated = Color(0xFF1A2234),
    primaryText = Color(0xFFE8EEF8),
    secondaryText = Color(0xFF8B9CB2),
    mutedText = Color(0xFF5A6B81),
    borderSubtle = Color(0xFF243048),
    accentDot = Color(0xFFADC2E0),
    glowColor = Color(0x44ADC2E0),
    isLight = false
)

val LocalProductiveOSPalette = staticCompositionLocalOf { OledBlackPalette }
val LocalProductiveOSFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }
val LocalProductiveOSFontScale = staticCompositionLocalOf { 1.0f }

@Composable
fun Modifier.tactilePress(
    enabled: Boolean = true,
    minTouchHeight: Boolean = true,
    enableGlow: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    if (!enabled) return this

    val haptic = LocalHapticFeedback.current
    val palette = LocalProductiveOSPalette.current
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val glowAnim = remember { Animatable(0f) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tactilePressScale"
    )

    return this
        .then(if (minTouchHeight) Modifier.defaultMinSize(minHeight = 48.dp) else Modifier)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawBehind {
            if (enableGlow && glowAnim.value > 0.01f) {
                drawCircle(
                    color = palette.glowColor.copy(alpha = (glowAnim.value * 0.45f).coerceIn(0f, 1f)),
                    radius = size.maxDimension * (0.6f + (1f - glowAnim.value) * 0.4f)
                )
            }
        }
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    val success = tryAwaitRelease()
                    isPressed = false
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke()
                },
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (enableGlow) {
                        coroutineScope.launch {
                            glowAnim.snapTo(1f)
                            glowAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                    onClick()
                }
            )
        }
}

@Composable
fun ProductiveOSTheme(
    themeOption: LauncherThemeOption = LauncherThemeOption.OLED_BLACK,
    typographyStyle: TypographyStyleOption = TypographyStyleOption.CLEAN_SANS,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val palette = when (themeOption) {
        LauncherThemeOption.OLED_BLACK -> OledBlackPalette
        LauncherThemeOption.SLATE_GRAY -> SlateGrayPalette
        LauncherThemeOption.WARM_STONE -> WarmStonePalette
        LauncherThemeOption.PALE_SAGE -> PaleSagePalette
        LauncherThemeOption.TOKYO_MIST -> TokyoMistPalette
        LauncherThemeOption.OCHRE_SAND -> OchreSandPalette
        LauncherThemeOption.MIDNIGHT_NAVY -> MidnightNavyPalette
    }

    val fontFamily = when (typographyStyle) {
        TypographyStyleOption.CLEAN_SANS -> FontFamily.SansSerif
        TypographyStyleOption.MONOSPACE_TERMINAL -> FontFamily.Monospace
        TypographyStyleOption.EDITORIAL_SERIF -> FontFamily.Serif
    }

    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText,
            primary = palette.primaryText,
            onPrimary = palette.background,
            secondary = palette.secondaryText
        )
    } else {
        darkColorScheme(
            background = palette.background,
            surface = palette.surface,
            onBackground = palette.primaryText,
            onSurface = palette.primaryText,
            primary = palette.primaryText,
            onPrimary = palette.background,
            secondary = palette.secondaryText
        )
    }

    val scaledTypography = Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = (52 * fontScale).sp,
            lineHeight = (58 * fontScale).sp,
            letterSpacing = (-1.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (22 * fontScale).sp,
            lineHeight = (30 * fontScale).sp,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (17 * fontScale).sp,
            lineHeight = (24 * fontScale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (16 * fontScale).sp,
            lineHeight = (22 * fontScale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * fontScale).sp,
            lineHeight = (20 * fontScale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * fontScale).sp,
            lineHeight = (16 * fontScale).sp,
            letterSpacing = 0.5.sp
        )
    )

    CompositionLocalProvider(
        LocalProductiveOSPalette provides palette,
        LocalProductiveOSFontFamily provides fontFamily,
        LocalProductiveOSFontScale provides fontScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}
