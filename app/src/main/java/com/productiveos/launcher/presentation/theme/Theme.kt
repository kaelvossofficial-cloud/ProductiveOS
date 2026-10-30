package com.productiveos.launcher.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.preferences.LauncherThemeOption
import com.productiveos.launcher.data.preferences.TypographyStyleOption

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
    isLight = true
)

val LocalProductiveOSPalette = staticCompositionLocalOf { OledBlackPalette }
val LocalProductiveOSFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }
val LocalProductiveOSFontScale = staticCompositionLocalOf { 1.0f }

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

