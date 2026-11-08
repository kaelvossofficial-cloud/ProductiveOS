package com.productiveos.launcher.presentation.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    val isLight: Boolean,
    val accentPrimary: Color = accentDot,
    val accentSecondary: Color = glowColor,
    val glassSurface: Color = surface.copy(alpha = 0.78f),
    val glassSurfaceElevated: Color = surfaceElevated.copy(alpha = 0.86f),
    val glassBorderTop: Color = primaryText.copy(alpha = if (isLight) 0.38f else 0.24f),
    val glassBorderBottom: Color = borderSubtle.copy(alpha = 0.45f)
)

fun paletteForTheme(themeOption: LauncherThemeOption): ProductiveOSPalette = when (themeOption) {
    LauncherThemeOption.LIQUID_FROST -> ProductiveOSPalette(
        background = Color(0xFF060B14),
        surface = Color(0xFF0E1726),
        surfaceElevated = Color(0xFF162238),
        primaryText = Color(0xFFF2F7FF),
        secondaryText = Color(0xFF9AB0D0),
        mutedText = Color(0xFF566B8A),
        borderSubtle = Color(0xFF223352),
        accentDot = Color(0xFF38BDF8),
        glowColor = Color(0xFF818CF8),
        isLight = false,
        accentPrimary = Color(0xFF38BDF8),
        accentSecondary = Color(0xFF818CF8),
        glassSurface = Color(0xCC0F192A),
        glassSurfaceElevated = Color(0xD918263E),
        glassBorderTop = Color(0x557DD3FC),
        glassBorderBottom = Color(0x2838BDF8)
    )
    LauncherThemeOption.AURORA_EMERALD -> ProductiveOSPalette(
        background = Color(0xFF05100D),
        surface = Color(0xFF0C1E18),
        surfaceElevated = Color(0xFF132D24),
        primaryText = Color(0xFFECFDF5),
        secondaryText = Color(0xFF86C5B0),
        mutedText = Color(0xFF497A6A),
        borderSubtle = Color(0xFF1E4438),
        accentDot = Color(0xFF34D399),
        glowColor = Color(0xFF2DD4BF),
        isLight = false,
        accentPrimary = Color(0xFF34D399),
        accentSecondary = Color(0xFF2DD4BF),
        glassSurface = Color(0xCC0D211B),
        glassSurfaceElevated = Color(0xD9153229),
        glassBorderTop = Color(0x556EE7B7),
        glassBorderBottom = Color(0x2834D399)
    )
    LauncherThemeOption.COSMIC_VIOLET -> ProductiveOSPalette(
        background = Color(0xFF090614),
        surface = Color(0xFF140F26),
        surfaceElevated = Color(0xFF1F183B),
        primaryText = Color(0xFFF5F3FF),
        secondaryText = Color(0xFFB4A5E8),
        mutedText = Color(0xFF695A9E),
        borderSubtle = Color(0xFF2E2356),
        accentDot = Color(0xFFC084FC),
        glowColor = Color(0xFFF472B6),
        isLight = false,
        accentPrimary = Color(0xFFC084FC),
        accentSecondary = Color(0xFFF472B6),
        glassSurface = Color(0xCC16102B),
        glassSurfaceElevated = Color(0xD9231A42),
        glassBorderTop = Color(0x55D8B4FE),
        glassBorderBottom = Color(0x28C084FC)
    )
    LauncherThemeOption.SUNSET_EMBER -> ProductiveOSPalette(
        background = Color(0xFF120807),
        surface = Color(0xFF21100E),
        surfaceElevated = Color(0xFF301815),
        primaryText = Color(0xFFFFF5F2),
        secondaryText = Color(0xFFD9A396),
        mutedText = Color(0xFF8A5A4F),
        borderSubtle = Color(0xFF472520),
        accentDot = Color(0xFFFB7185),
        glowColor = Color(0xFFFBBF24),
        isLight = false,
        accentPrimary = Color(0xFFFB7185),
        accentSecondary = Color(0xFFFBBF24),
        glassSurface = Color(0xCC24120F),
        glassSurfaceElevated = Color(0xD9361B17),
        glassBorderTop = Color(0x55FDA4AF),
        glassBorderBottom = Color(0x28FB7185)
    )
    LauncherThemeOption.OCEAN_ABYSS -> ProductiveOSPalette(
        background = Color(0xFF040E14),
        surface = Color(0xFF0A1D26),
        surfaceElevated = Color(0xFF112C3A),
        primaryText = Color(0xFFECFEFF),
        secondaryText = Color(0xFF88C4D4),
        mutedText = Color(0xFF487887),
        borderSubtle = Color(0xFF1B4254),
        accentDot = Color(0xFF22D3EE),
        glowColor = Color(0xFF38BDF8),
        isLight = false,
        accentPrimary = Color(0xFF22D3EE),
        accentSecondary = Color(0xFF38BDF8),
        glassSurface = Color(0xCC0B202B),
        glassSurfaceElevated = Color(0xD9133140),
        glassBorderTop = Color(0x5567E8F9),
        glassBorderBottom = Color(0x2822D3EE)
    )
    LauncherThemeOption.OLED_BLACK -> ProductiveOSPalette(
        background = Color(0xFF000000),
        surface = Color(0xFF0D0E12),
        surfaceElevated = Color(0xFF171920),
        primaryText = Color(0xFFF7F8FA),
        secondaryText = Color(0xFFA0A6B2),
        mutedText = Color(0xFF5A606D),
        borderSubtle = Color(0xFF232630),
        accentDot = Color(0xFF60A5FA),
        glowColor = Color(0xFFA78BFA),
        isLight = false,
        accentPrimary = Color(0xFF60A5FA),
        accentSecondary = Color(0xFFA78BFA),
        glassSurface = Color(0xCC101218),
        glassSurfaceElevated = Color(0xD91A1D26),
        glassBorderTop = Color(0x45FFFFFF),
        glassBorderBottom = Color(0x22FFFFFF)
    )
    LauncherThemeOption.MIDNIGHT_NAVY -> ProductiveOSPalette(
        background = Color(0xFF090E18),
        surface = Color(0xFF121A2B),
        surfaceElevated = Color(0xFF1B2740),
        primaryText = Color(0xFFEAF0FA),
        secondaryText = Color(0xFF94A3B8),
        mutedText = Color(0xFF52627A),
        borderSubtle = Color(0xFF273654),
        accentDot = Color(0xFF60A5FA),
        glowColor = Color(0xFF38BDF8),
        isLight = false,
        accentPrimary = Color(0xFF60A5FA),
        accentSecondary = Color(0xFF38BDF8),
        glassSurface = Color(0xCC131C2E),
        glassSurfaceElevated = Color(0xD91D2A44),
        glassBorderTop = Color(0x4D93C5FD),
        glassBorderBottom = Color(0x2460A5FA)
    )
    LauncherThemeOption.SLATE_GRAY -> ProductiveOSPalette(
        background = Color(0xFF101114),
        surface = Color(0xFF191B20),
        surfaceElevated = Color(0xFF23262E),
        primaryText = Color(0xFFF1F3F5),
        secondaryText = Color(0xFFA5ABB6),
        mutedText = Color(0xFF636975),
        borderSubtle = Color(0xFF2F333D),
        accentDot = Color(0xFF94A3B8),
        glowColor = Color(0xFFCBD5E1),
        isLight = false,
        accentPrimary = Color(0xFF94A3B8),
        accentSecondary = Color(0xFF38BDF8),
        glassSurface = Color(0xCC1B1D23),
        glassSurfaceElevated = Color(0xD9252830),
        glassBorderTop = Color(0x45E2E8F0),
        glassBorderBottom = Color(0x2294A3B8)
    )
    LauncherThemeOption.ROSE_QUARTZ -> ProductiveOSPalette(
        background = Color(0xFFF9F1F4),
        surface = Color(0xFFF2E4E9),
        surfaceElevated = Color(0xFFFFFFFF),
        primaryText = Color(0xFF2B141D),
        secondaryText = Color(0xFF734859),
        mutedText = Color(0xFFA67C8D),
        borderSubtle = Color(0xFFE2C8D2),
        accentDot = Color(0xFFE11D48),
        glowColor = Color(0xFFF43F5E),
        isLight = true,
        accentPrimary = Color(0xFFE11D48),
        accentSecondary = Color(0xFFEC4899),
        glassSurface = Color(0xCCF5E8ED),
        glassSurfaceElevated = Color(0xE6FFFFFF),
        glassBorderTop = Color(0x99FFFFFF),
        glassBorderBottom = Color(0x44E11D48)
    )
    LauncherThemeOption.WARM_STONE -> ProductiveOSPalette(
        background = Color(0xFFF5F3EE),
        surface = Color(0xFFEAE6DF),
        surfaceElevated = Color(0xFFFFFFFF),
        primaryText = Color(0xFF1C1A17),
        secondaryText = Color(0xFF5E5A52),
        mutedText = Color(0xFF948E83),
        borderSubtle = Color(0xFFD6D0C4),
        accentDot = Color(0xFFD97706),
        glowColor = Color(0xFFF59E0B),
        isLight = true,
        accentPrimary = Color(0xFFD97706),
        accentSecondary = Color(0xFFEA580C),
        glassSurface = Color(0xCCEDEAE2),
        glassSurfaceElevated = Color(0xE6FFFFFF),
        glassBorderTop = Color(0x99FFFFFF),
        glassBorderBottom = Color(0x44D97706)
    )
    LauncherThemeOption.PALE_SAGE -> ProductiveOSPalette(
        background = Color(0xFFECF2EE),
        surface = Color(0xFFDFE8E2),
        surfaceElevated = Color(0xFFF9FCFA),
        primaryText = Color(0xFF14241B),
        secondaryText = Color(0xFF476353),
        mutedText = Color(0xFF7B9687),
        borderSubtle = Color(0xFFC5D6CB),
        accentDot = Color(0xFF059669),
        glowColor = Color(0xFF10B981),
        isLight = true,
        accentPrimary = Color(0xFF059669),
        accentSecondary = Color(0xFF0D9488),
        glassSurface = Color(0xCCE2ECE5),
        glassSurfaceElevated = Color(0xE6FFFFFF),
        glassBorderTop = Color(0x99FFFFFF),
        glassBorderBottom = Color(0x44059669)
    )
    LauncherThemeOption.TOKYO_MIST -> ProductiveOSPalette(
        background = Color(0xFFEBF0F7),
        surface = Color(0xFFDDE5F0),
        surfaceElevated = Color(0xFFFDFEFF),
        primaryText = Color(0xFF121C2B),
        secondaryText = Color(0xFF4A5C75),
        mutedText = Color(0xFF7E92AD),
        borderSubtle = Color(0xFFC4D1E3),
        accentDot = Color(0xFF4F46E5),
        glowColor = Color(0xFF6366F1),
        isLight = true,
        accentPrimary = Color(0xFF4F46E5),
        accentSecondary = Color(0xFF0284C7),
        glassSurface = Color(0xCCE1E9F4),
        glassSurfaceElevated = Color(0xE6FFFFFF),
        glassBorderTop = Color(0x99FFFFFF),
        glassBorderBottom = Color(0x444F46E5)
    )
    LauncherThemeOption.OCHRE_SAND -> ProductiveOSPalette(
        background = Color(0xFFF6EFE4),
        surface = Color(0xFFEBE1D1),
        surfaceElevated = Color(0xFFFFFCF7),
        primaryText = Color(0xFF281F15),
        secondaryText = Color(0xFF6B5947),
        mutedText = Color(0xFF9E8A75),
        borderSubtle = Color(0xFFD9C9B4),
        accentDot = Color(0xFFC2410C),
        glowColor = Color(0xFFD97706),
        isLight = true,
        accentPrimary = Color(0xFFC2410C),
        accentSecondary = Color(0xFFD97706),
        glassSurface = Color(0xCCEDE3D5),
        glassSurfaceElevated = Color(0xE6FFFFFF),
        glassBorderTop = Color(0x99FFFFFF),
        glassBorderBottom = Color(0x44C2410C)
    )
}

val LocalProductiveOSPalette = staticCompositionLocalOf {
    paletteForTheme(LauncherThemeOption.LIQUID_FROST)
}

@Composable
fun ProductiveOSTheme(
    themeOption: LauncherThemeOption = LauncherThemeOption.LIQUID_FROST,
    typographyStyle: TypographyStyleOption = TypographyStyleOption.CLEAN_SANS,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val targetPalette = remember(themeOption) { paletteForTheme(themeOption) }

    val colorAnimSpec = tween<Color>(durationMillis = 280)
    val animBackground by animateColorAsState(targetPalette.background, colorAnimSpec, label = "bg")
    val animSurface by animateColorAsState(targetPalette.surface, colorAnimSpec, label = "surf")
    val animSurfaceElevated by animateColorAsState(targetPalette.surfaceElevated, colorAnimSpec, label = "surfEl")
    val animPrimaryText by animateColorAsState(targetPalette.primaryText, colorAnimSpec, label = "pText")
    val animSecondaryText by animateColorAsState(targetPalette.secondaryText, colorAnimSpec, label = "sText")
    val animAccentPrimary by animateColorAsState(targetPalette.accentPrimary, colorAnimSpec, label = "acc1")
    val animAccentSecondary by animateColorAsState(targetPalette.accentSecondary, colorAnimSpec, label = "acc2")

    val activePalette = remember(
        targetPalette,
        animBackground,
        animSurface,
        animSurfaceElevated,
        animPrimaryText,
        animSecondaryText,
        animAccentPrimary,
        animAccentSecondary
    ) {
        targetPalette.copy(
            background = animBackground,
            surface = animSurface,
            surfaceElevated = animSurfaceElevated,
            primaryText = animPrimaryText,
            secondaryText = animSecondaryText,
            accentDot = animAccentPrimary,
            accentPrimary = animAccentPrimary,
            accentSecondary = animAccentSecondary,
            glowColor = animAccentSecondary
        )
    }

    val fontFamily = remember(typographyStyle) {
        when (typographyStyle) {
            TypographyStyleOption.CLEAN_SANS -> FontFamily.SansSerif
            TypographyStyleOption.MONOSPACE_TERMINAL -> FontFamily.Monospace
            TypographyStyleOption.EDITORIAL_SERIF -> FontFamily.Serif
        }
    }

    val scale = fontScale.coerceIn(0.85f, 1.30f)

    val typography = remember(fontFamily, scale) {
        Typography(
            displayLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Light,
                fontSize = (54f * scale).sp,
                lineHeight = (60f * scale).sp,
                letterSpacing = (-1.2).sp
            ),
            headlineMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (22f * scale).sp,
                lineHeight = (28f * scale).sp,
                letterSpacing = (-0.2).sp
            ),
            titleMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = (16f * scale).sp,
                lineHeight = (22f * scale).sp
            ),
            bodyLarge = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (15f * scale).sp,
                lineHeight = (21f * scale).sp
            ),
            bodyMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (13.5f * scale).sp,
                lineHeight = (19f * scale).sp
            ),
            labelMedium = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = (12f * scale).sp,
                lineHeight = (16f * scale).sp,
                letterSpacing = 0.25.sp
            )
        )
    }

    val colorScheme = remember(activePalette) {
        if (activePalette.isLight) {
            lightColorScheme(
                background = activePalette.background,
                surface = activePalette.surface,
                onBackground = activePalette.primaryText,
                onSurface = activePalette.primaryText,
                primary = activePalette.accentPrimary,
                secondary = activePalette.accentSecondary
            )
        } else {
            darkColorScheme(
                background = activePalette.background,
                surface = activePalette.surface,
                onBackground = activePalette.primaryText,
                onSurface = activePalette.primaryText,
                primary = activePalette.accentPrimary,
                secondary = activePalette.accentSecondary
            )
        }
    }

    CompositionLocalProvider(LocalProductiveOSPalette provides activePalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

/**
 * Reusable Liquid Glass OS Card with translucent multi-stop surface gradient,
 * top-left specular rim highlight, and optional interactive spring press.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevated: Boolean = false,
    accentHighlight: Boolean = false,
    paddingHorizontal: Dp = 18.dp,
    paddingVertical: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val bgBrush = remember(palette, elevated, accentHighlight) {
        if (accentHighlight) {
            Brush.linearGradient(
                colors = listOf(
                    palette.accentPrimary.copy(alpha = if (palette.isLight) 0.18f else 0.22f),
                    palette.glassSurfaceElevated,
                    palette.accentSecondary.copy(alpha = if (palette.isLight) 0.10f else 0.14f)
                )
            )
        } else if (elevated) {
            Brush.linearGradient(
                colors = listOf(
                    palette.glassSurfaceElevated,
                    palette.surface.copy(alpha = 0.90f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    palette.glassSurface,
                    palette.surface.copy(alpha = 0.72f)
                )
            )
        }
    }

    val borderBrush = remember(palette, accentHighlight) {
        if (accentHighlight) {
            Brush.linearGradient(
                colors = listOf(
                    palette.accentPrimary.copy(alpha = 0.85f),
                    palette.glassBorderTop,
                    palette.accentSecondary.copy(alpha = 0.55f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    palette.glassBorderTop,
                    palette.borderSubtle.copy(alpha = 0.45f),
                    palette.glassBorderBottom
                )
            )
        }
    }

    val clickMod = if (onClick != null) {
        Modifier
            .jellyPress(0.97f)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(clickMod)
            .clip(shape)
            .background(brush = bgBrush, shape = shape)
            .border(
                width = if (accentHighlight) 1.4.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
            .padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        content = content
    )
}

/**
 * Liquid Glass Capsule Pill for tabs, chips, and segmented actions.
 */
@Composable
fun LiquidGlassPill(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val shape = remember { RoundedCornerShape(50) }

    val bgBrush = remember(palette, selected) {
        if (selected) {
            Brush.linearGradient(
                colors = listOf(
                    palette.accentPrimary.copy(alpha = if (palette.isLight) 0.22f else 0.30f),
                    palette.accentSecondary.copy(alpha = if (palette.isLight) 0.14f else 0.20f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    palette.glassSurface,
                    palette.surface.copy(alpha = 0.65f)
                )
            )
        }
    }

    val borderBrush = remember(palette, selected) {
        if (selected) {
            Brush.linearGradient(
                colors = listOf(
                    palette.accentPrimary,
                    palette.accentSecondary.copy(alpha = 0.7f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    palette.glassBorderTop,
                    palette.borderSubtle.copy(alpha = 0.4f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .jellyPress(0.95f)
            .clip(shape)
            .background(brush = bgBrush, shape = shape)
            .border(
                width = if (selected) 1.3.dp else 0.9.dp,
                brush = borderBrush,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

fun Modifier.tactilePress(
    minTouchHeight: Boolean = true,
    enableGlow: Boolean = true,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val palette = LocalProductiveOSPalette.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val glowAlphaAnim = remember { Animatable(0f) }

    this
        .then(if (minTouchHeight) Modifier.defaultMinSize(minHeight = 48.dp) else Modifier)
        .graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
        }
        .drawBehind {
            if (enableGlow && glowAlphaAnim.value > 0.01f) {
                val radius = size.maxDimension * 0.65f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentPrimary.copy(alpha = glowAlphaAnim.value * 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
        }
        .pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scaleAnim.animateTo(
                            targetValue = 0.97f,
                            animationSpec = tween(durationMillis = 80)
                        )
                    }
                    if (enableGlow) {
                        scope.launch {
                            glowAlphaAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 110)
                            )
                        }
                    }
                    tryAwaitRelease()
                    scope.launch {
                        scaleAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    if (enableGlow) {
                        scope.launch {
                            glowAlphaAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 220)
                            )
                        }
                    }
                },
                onTap = { onClick() },
                onLongPress = { onLongClick?.invoke() }
            )
        }
}

fun Modifier.jellyPress(
    pressedScale: Float = 0.96f
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(pressedScale) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch {
                        scale.animateTo(
                            targetValue = pressedScale,
                            animationSpec = tween(
                                durationMillis = 80,
                                easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
                            )
                        )
                    }
                    waitForUpOrCancellation()
                    scope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.55f,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                }
            }
        }
}
