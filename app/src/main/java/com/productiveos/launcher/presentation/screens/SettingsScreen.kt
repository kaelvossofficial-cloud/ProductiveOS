package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.IconColorMode
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.LauncherThemeOption
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.preferences.TypographyStyleOption
import com.productiveos.launcher.data.preferences.VolumeBarStyleOption
import com.productiveos.launcher.data.repository.WeeklyAwarenessSummary
import com.productiveos.launcher.presentation.components.ActionPillButton
import com.productiveos.launcher.presentation.components.ThemedVolumeVoiceBar
import com.productiveos.launcher.presentation.components.VolumeSkinSelectorCard
import com.productiveos.launcher.presentation.theme.LiquidGlassCard
import com.productiveos.launcher.presentation.theme.LiquidGlassPill
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.jellyPress
import com.productiveos.launcher.presentation.theme.paletteForTheme
import com.productiveos.launcher.util.FocusSoundMode
import kotlin.math.roundToInt

private enum class SettingsTab(val title: String, val badge: String) {
    THEMES("Themes", "◐"),
    LAYOUT("Home & UI", "⊞"),
    VOICE_BAR("Voice Bar", "ılı"),
    FOCUS("Mindful", "◎"),
    VAULT("Vault & OS", "◈")
}

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    isDefaultLauncher: Boolean,
    pinnedApps: List<AppEntity>,
    hiddenApps: List<AppEntity>,
    weeklyAwareness: WeeklyAwarenessSummary,
    activeFocusSound: FocusSoundMode?,
    volumeFraction: Float = 0.68f,
    isVolumeMuted: Boolean = false,
    onBackToHome: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onSwitchSystemLauncher: () -> Unit,
    onTextAlignmentSelected: (TextAlignmentOption) -> Unit,
    onTypographySelected: (TypographyStyleOption) -> Unit,
    onThemeSelected: (LauncherThemeOption) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onIconScaleChanged: (Float) -> Unit,
    onIconColorModeChanged: (IconColorMode) -> Unit,
    onAutoLaunchChanged: (Boolean) -> Unit,
    onT9SearchChanged: (Boolean) -> Unit,
    onFrictionSecondsChanged: (Int) -> Unit,
    onMindfulFrictionEnabledChanged: (Boolean) -> Unit,
    onHoldToOpenChanged: (Boolean) -> Unit,
    onHoldSecondsChanged: (Int) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onStatusBarStealthChanged: (Boolean) -> Unit,
    onBottomDockedSearchChanged: (Boolean) -> Unit,
    onSwipeDownNotificationsChanged: (Boolean) -> Unit,
    onOmniSearchToolsChanged: (Boolean) -> Unit,
    onAutoDayNightThemeChanged: (Boolean) -> Unit,
    onSyncWallpaperClicked: () -> Unit,
    onOpenQuantumClip: () -> Unit,
    onOpenMicroLog: () -> Unit,
    onOpenBurnerNote: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
    onStartFocusSound: (FocusSoundMode?) -> Unit,
    onMovePinnedApp: (AppEntity, Boolean) -> Unit,
    onUnpinApp: (AppEntity) -> Unit,
    onUnhideApp: (AppEntity) -> Unit,
    onLaunchHiddenApp: (AppEntity) -> Unit,
    onUpdateVaultPasscode: (String) -> Unit,
    onHomeToggleChanged: (String, Boolean) -> Unit = { _, _ -> },
    onAddFavoriteContact: (String, String) -> Unit = { _, _ -> },
    onRemoveFavoriteContact: (Int) -> Unit = {},
    onVolumeBarStyleSelected: (VolumeBarStyleOption) -> Unit = {},
    onCustomVolumeBarEnabledChanged: (Boolean) -> Unit = {},
    onSystemVolumeChanged: (Float) -> Unit = {},
    onToggleSystemMute: () -> Unit = {},
    onTestVolumeOverlay: () -> Unit = {}
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableStateOf(SettingsTab.THEMES) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Liquid Glass Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PRODUCTIVE OS · LIQUID GLASS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp
                    ),
                    color = palette.accentPrimary
                )
                Text(
                    text = "Control Center",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
            }

            LiquidGlassPill(
                selected = false,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackToHome()
                }
            ) {
                Text(
                    text = "← Home",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented 5-Tab Liquid Glass Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                LiquidGlassPill(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = tab
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tab.badge,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) palette.accentPrimary else palette.secondaryText
                        )
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) palette.primaryText else palette.secondaryText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content Area
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(tween(200, easing = FastOutSlowInEasing)) togetherWith
                    fadeOut(tween(150, easing = FastOutSlowInEasing))
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            label = "SettingsTabContent"
        ) { tab ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                when (tab) {
                    SettingsTab.THEMES -> ThemesAndGlassTab(
                        settings = settings,
                        onThemeSelected = onThemeSelected,
                        onAutoDayNightThemeChanged = onAutoDayNightThemeChanged,
                        onSyncWallpaperClicked = onSyncWallpaperClicked,
                        onTypographySelected = onTypographySelected,
                        onFontScaleChanged = onFontScaleChanged,
                        onIconScaleChanged = onIconScaleChanged,
                        onIconColorModeChanged = onIconColorModeChanged
                    )
                    SettingsTab.LAYOUT -> LayoutAndWidgetsTab(
                        settings = settings,
                        pinnedApps = pinnedApps,
                        onHomeToggleChanged = onHomeToggleChanged,
                        onStatusBarStealthChanged = onStatusBarStealthChanged,
                        onBottomDockedSearchChanged = onBottomDockedSearchChanged,
                        onTextAlignmentSelected = onTextAlignmentSelected,
                        onMovePinnedApp = onMovePinnedApp,
                        onUnpinApp = onUnpinApp,
                        onAddFavoriteContact = onAddFavoriteContact,
                        onRemoveFavoriteContact = onRemoveFavoriteContact,
                        onRequestNotificationAccess = onRequestNotificationAccess
                    )
                    SettingsTab.VOICE_BAR -> VoiceBarAndSoundTab(
                        settings = settings,
                        volumeFraction = volumeFraction,
                        isVolumeMuted = isVolumeMuted,
                        activeFocusSound = activeFocusSound,
                        onVolumeBarStyleSelected = onVolumeBarStyleSelected,
                        onCustomVolumeBarEnabledChanged = onCustomVolumeBarEnabledChanged,
                        onSystemVolumeChanged = onSystemVolumeChanged,
                        onToggleSystemMute = onToggleSystemMute,
                        onTestVolumeOverlay = onTestVolumeOverlay,
                        onStartFocusSound = onStartFocusSound
                    )
                    SettingsTab.FOCUS -> MindfulAndFocusTab(
                        settings = settings,
                        weeklyAwareness = weeklyAwareness,
                        onMindfulFrictionEnabledChanged = onMindfulFrictionEnabledChanged,
                        onFrictionSecondsChanged = onFrictionSecondsChanged,
                        onHoldToOpenChanged = onHoldToOpenChanged,
                        onHoldSecondsChanged = onHoldSecondsChanged,
                        onAutoLaunchChanged = onAutoLaunchChanged,
                        onT9SearchChanged = onT9SearchChanged,
                        onOmniSearchToolsChanged = onOmniSearchToolsChanged,
                        onSwipeDownNotificationsChanged = onSwipeDownNotificationsChanged,
                        onHomeToggleChanged = onHomeToggleChanged
                    )
                    SettingsTab.VAULT -> VaultAndSystemTab(
                        settings = settings,
                        isDefaultLauncher = isDefaultLauncher,
                        hiddenApps = hiddenApps,
                        onSetDefaultLauncher = onSetDefaultLauncher,
                        onSwitchSystemLauncher = onSwitchSystemLauncher,
                        onOpenQuantumClip = onOpenQuantumClip,
                        onOpenMicroLog = onOpenMicroLog,
                        onOpenBurnerNote = onOpenBurnerNote,
                        onUnhideApp = onUnhideApp,
                        onLaunchHiddenApp = onLaunchHiddenApp,
                        onUpdateVaultPasscode = onUpdateVaultPasscode,
                        onReplayOnboarding = onReplayOnboarding
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

// ============================================================================
// TAB 1: THEMES & LIQUID GLASS
// ============================================================================
@Composable
private fun ThemesAndGlassTab(
    settings: LauncherSettings,
    onThemeSelected: (LauncherThemeOption) -> Unit,
    onAutoDayNightThemeChanged: (Boolean) -> Unit,
    onSyncWallpaperClicked: () -> Unit,
    onTypographySelected: (TypographyStyleOption) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onIconScaleChanged: (Float) -> Unit,
    onIconColorModeChanged: (IconColorMode) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    // Live Liquid Glass Preview Card
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevated = true,
        accentHighlight = true,
        paddingHorizontal = 20.dp,
        paddingVertical = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary)
                    )
                    Text(
                        text = settings.themeOption.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = settings.themeOption.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassPill(
                        selected = settings.autoDayNightTheme,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAutoDayNightThemeChanged(!settings.autoDayNightTheme)
                        }
                    ) {
                        Text(
                            text = if (settings.autoDayNightTheme) "☀/☾ Auto Day-Night On" else "☀/☾ Auto Day-Night",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = palette.primaryText
                        )
                    }
                    LiquidGlassPill(
                        selected = false,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSyncWallpaperClicked()
                        }
                    ) {
                        Text(
                            text = "Sync Wallpaper",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = palette.accentPrimary
                        )
                    }
                }
            }

            // Mini Clock & Orbs Preview
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "09:41",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        palette.accentPrimary,
                        palette.accentSecondary,
                        palette.primaryText,
                        palette.surfaceElevated
                    ).forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(0.8.dp, palette.glassBorderTop, CircleShape)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    SectionTitle("Curated Color Palettes", "Multi-tone Liquid Glass themes")
    Spacer(modifier = Modifier.height(10.dp))

    // 2-Column Visual Theme Swatch Grid (Zero Hex Codes!)
    val themes = LauncherThemeOption.entries
    val rows = themes.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { themeOption ->
                    ThemePaletteSwatchCard(
                        themeOption = themeOption,
                        selected = settings.themeOption == themeOption,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onThemeSelected(themeOption)
                        }
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    SectionTitle("Typography & Font Architecture", "Editorial typefaces & scaling")
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TypographyStyleOption.entries.forEach { option ->
            val isSelected = settings.typographyStyle == option
            val previewFont = when (option) {
                TypographyStyleOption.CLEAN_SANS -> FontFamily.SansSerif
                TypographyStyleOption.MONOSPACE_TERMINAL -> FontFamily.Monospace
                TypographyStyleOption.EDITORIAL_SERIF -> FontFamily.Serif
            }
            LiquidGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 18.dp,
                elevated = isSelected,
                accentHighlight = isSelected,
                paddingHorizontal = 12.dp,
                paddingVertical = 14.dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTypographySelected(option)
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aa",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = previewFont,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isSelected) palette.accentPrimary else palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = palette.primaryText,
                        maxLines = 1
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Font Scale & Icon Scale Sliders inside a single Liquid Glass Card
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Typography Scale",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
                Text(
                    text = "${(settings.fontScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentPrimary
                )
            }
            Slider(
                value = settings.fontScale,
                onValueChange = onFontScaleChanged,
                valueRange = 0.85f..1.30f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accentPrimary,
                    activeTrackColor = palette.accentPrimary,
                    inactiveTrackColor = palette.borderSubtle
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Home Icon Size",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
                Text(
                    text = "${(settings.iconScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentPrimary
                )
            }
            Slider(
                value = settings.iconScale,
                onValueChange = onIconScaleChanged,
                valueRange = 0.75f..1.35f,
                steps = 11,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accentSecondary,
                    activeTrackColor = palette.accentSecondary,
                    inactiveTrackColor = palette.borderSubtle
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconColorMode.entries.forEach { mode ->
                    val selected = settings.iconColorMode == mode
                    LiquidGlassPill(
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIconColorModeChanged(mode)
                        }
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) palette.primaryText else palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePaletteSwatchCard(
    themeOption: LauncherThemeOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val swatchPalette = remember(themeOption) { paletteForTheme(themeOption) }
    val activePalette = LocalProductiveOSPalette.current
    val shape = remember { RoundedCornerShape(18.dp) }

    Box(
        modifier = modifier
            .jellyPress(0.96f)
            .clip(shape)
            .background(swatchPalette.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                brush = if (selected) {
                    Brush.linearGradient(
                        listOf(swatchPalette.accentPrimary, swatchPalette.accentSecondary)
                    )
                } else {
                    Brush.linearGradient(
                        listOf(swatchPalette.glassBorderTop, swatchPalette.borderSubtle)
                    )
                },
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Visual Mini Glass Window & Color Orbs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(swatchPalette.accentPrimary)
                            .border(1.dp, swatchPalette.background, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(swatchPalette.accentSecondary)
                            .border(1.dp, swatchPalette.background, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(swatchPalette.surfaceElevated)
                            .border(1.dp, swatchPalette.glassBorderTop, CircleShape)
                    )
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(swatchPalette.accentPrimary.copy(alpha = 0.22f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = swatchPalette.accentPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = themeOption.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = swatchPalette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = themeOption.subtitle,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                color = swatchPalette.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================================
// TAB 2: HOME & LAYOUT (BENTO VISUAL CARDS INSTEAD OF TEXT SWITCHES)
// ============================================================================
private data class BentoToggleSpec(
    val key: String,
    val title: String,
    val subtitle: String,
    val glyph: String,
    val enabled: Boolean,
    val onToggle: (Boolean) -> Unit
)

@Composable
private fun LayoutAndWidgetsTab(
    settings: LauncherSettings,
    pinnedApps: List<AppEntity>,
    onHomeToggleChanged: (String, Boolean) -> Unit,
    onStatusBarStealthChanged: (Boolean) -> Unit,
    onBottomDockedSearchChanged: (Boolean) -> Unit,
    onTextAlignmentSelected: (TextAlignmentOption) -> Unit,
    onMovePinnedApp: (AppEntity, Boolean) -> Unit,
    onUnpinApp: (AppEntity) -> Unit,
    onAddFavoriteContact: (String, String) -> Unit,
    onRemoveFavoriteContact: (Int) -> Unit,
    onRequestNotificationAccess: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    SectionTitle("Home Canvas Modules", "Tap any glass tile to toggle its presence")
    Spacer(modifier = Modifier.height(10.dp))

    val tiles = listOf(
        BentoToggleSpec(
            "showClock",
            "Hero Clock",
            "Minimalist timepiece",
            "◷",
            settings.showClock
        ) { onHomeToggleChanged("showClock", it) },
        BentoToggleSpec(
            "showDate",
            "Date Subheader",
            "Calendar link",
            "📅",
            settings.showDate
        ) { onHomeToggleChanged("showDate", it) },
        BentoToggleSpec(
            "showMusicCapsule",
            "Music Capsule",
            "Live media pill",
            "♫",
            settings.showMusicCapsule
        ) { onHomeToggleChanged("showMusicCapsule", it) },
        BentoToggleSpec(
            "showTelemetry",
            "Mindful Pill",
            "Screen time & unlocks",
            "◉",
            settings.showTelemetry
        ) { onHomeToggleChanged("showTelemetry", it) },
        BentoToggleSpec(
            "floatingHomeIcons",
            "Floating Icons",
            "Organic squircle grid",
            "❖",
            settings.floatingHomeIcons
        ) { onHomeToggleChanged("floatingHomeIcons", it) },
        BentoToggleSpec(
            "showIconDock",
            "Glass Dock",
            "Quick bottom bar",
            "▭",
            settings.showIconDock
        ) { onHomeToggleChanged("showIconDock", it) },
        BentoToggleSpec(
            "showInteractiveGrid",
            "Kinetic Grid",
            "Touch-reactive dots",
            "∷",
            settings.showInteractiveGrid
        ) { onHomeToggleChanged("showInteractiveGrid", it) },
        BentoToggleSpec(
            "showHomeEdgeSidebar",
            "Edge A-Z Wave",
            "Gaussian scrubber",
            "⌇",
            settings.showHomeEdgeSidebar
        ) { onHomeToggleChanged("showHomeEdgeSidebar", it) },
        BentoToggleSpec(
            "drawerGridMode",
            "Drawer Icon Grid",
            "4-column app grid",
            "⊞",
            settings.drawerGridMode
        ) { onHomeToggleChanged("drawerGridMode", it) },
        BentoToggleSpec(
            "bottomDockedSearch",
            "Thumb Search Bar",
            "Dock search at bottom",
            "⌕",
            settings.bottomDockedSearch
        ) { onBottomDockedSearchChanged(it) },
        BentoToggleSpec(
            "statusBarStealth",
            "Stealth Status Bar",
            "Full-bleed immersion",
            "⌃",
            settings.statusBarStealth
        ) { onStatusBarStealthChanged(it) },
        BentoToggleSpec(
            "showWhisper",
            "Whisper Ticker",
            "Battery & next event",
            "✦",
            settings.showWhisper
        ) { onHomeToggleChanged("showWhisper", it) }
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { spec ->
                    BentoFeatureTile(
                        spec = spec,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            spec.onToggle(!spec.enabled)
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    SectionTitle("Canvas Alignment", "Visual layout posture")
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextAlignmentOption.entries.forEach { option ->
            val selected = settings.textAlignment == option
            LiquidGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 18.dp,
                elevated = selected,
                accentHighlight = selected,
                paddingHorizontal = 12.dp,
                paddingVertical = 14.dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTextAlignmentSelected(option)
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = when (option) {
                        TextAlignmentOption.LEFT -> Alignment.Start
                        TextAlignmentOption.CENTER -> Alignment.CenterHorizontally
                        TextAlignmentOption.RIGHT -> Alignment.End
                    }
                ) {
                    // Visual alignment lines graphic
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) palette.accentPrimary else palette.secondaryText)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) palette.accentSecondary else palette.mutedText)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = palette.primaryText
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Pinned Essentials Manager
    SectionTitle("Pinned Home Essentials (${pinnedApps.size}/6)", "Reorder or unpin home apps")
    Spacer(modifier = Modifier.height(10.dp))

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        if (pinnedApps.isEmpty()) {
            Text(
                text = "No apps pinned yet. Long-press any app in the drawer to pin up to 6 essentials.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pinnedApps.forEachIndexed { index, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentPrimary.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.accentPrimary
                                )
                            }
                            Text(
                                text = app.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.primaryText
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (index > 0) {
                                LiquidGlassPill(
                                    selected = false,
                                    onClick = { onMovePinnedApp(app, true) }
                                ) {
                                    Text("↑", color = palette.primaryText, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            if (index < pinnedApps.lastIndex) {
                                LiquidGlassPill(
                                    selected = false,
                                    onClick = { onMovePinnedApp(app, false) }
                                ) {
                                    Text("↓", color = palette.primaryText, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            LiquidGlassPill(
                                selected = false,
                                onClick = { onUnpinApp(app) }
                            ) {
                                Text("Unpin", color = palette.secondaryText, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    if (index < pinnedApps.lastIndex) {
                        HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Favorite Contacts & Inline Reply
    SectionTitle("Quick-Action Contacts", "Hold Phone / WhatsApp / SMS icon for 1-tap reach")
    Spacer(modifier = Modifier.height(10.dp))

    var contactName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            settings.favoriteContacts.forEachIndexed { idx, fav ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${fav.name} · ${fav.number}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.primaryText
                    )
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.accentPrimary,
                        modifier = Modifier.clickable { onRemoveFavoriteContact(idx) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = contactName,
                    onValueChange = { contactName = it.take(18) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.primaryText),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.background.copy(alpha = 0.55f))
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (contactName.isEmpty()) {
                            Text("Name", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
                        }
                        inner()
                    }
                )
                BasicTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it.take(18) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.primaryText),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.background.copy(alpha = 0.55f))
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (contactNumber.isEmpty()) {
                            Text("+Phone", style = MaterialTheme.typography.bodyMedium, color = palette.mutedText)
                        }
                        inner()
                    }
                )
                LiquidGlassPill(
                    selected = true,
                    onClick = {
                        if (contactNumber.isNotBlank()) {
                            onAddFavoriteContact(contactName, contactNumber)
                            contactName = ""
                            contactNumber = ""
                        }
                    }
                ) {
                    Text("+ Add", style = MaterialTheme.typography.labelMedium, color = palette.primaryText)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LiquidGlassPill(
                selected = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequestNotificationAccess
            ) {
                Text(
                    text = "Grant Notification Access for Inline Popup Replies →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentPrimary
                )
            }
        }
    }
}

@Composable
private fun BentoFeatureTile(
    spec: BentoToggleSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        elevated = spec.enabled,
        accentHighlight = spec.enabled,
        paddingHorizontal = 14.dp,
        paddingVertical = 14.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (spec.enabled) palette.accentPrimary.copy(alpha = 0.22f)
                            else palette.background.copy(alpha = 0.45f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spec.glyph,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (spec.enabled) palette.accentPrimary else palette.mutedText
                    )
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (spec.enabled) palette.accentPrimary else palette.borderSubtle
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = spec.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (spec.enabled) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = palette.primaryText,
                maxLines = 1
            )
            Text(
                text = spec.subtitle,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                color = palette.secondaryText,
                maxLines = 1
            )
        }
    }
}

// ============================================================================
// TAB 3: CUSTOMIZED VOICE / VOLUME BAR (4 THEME-MATCHED SKINS) & SOUND
// ============================================================================
@Composable
private fun VoiceBarAndSoundTab(
    settings: LauncherSettings,
    volumeFraction: Float,
    isVolumeMuted: Boolean,
    activeFocusSound: FocusSoundMode?,
    onVolumeBarStyleSelected: (VolumeBarStyleOption) -> Unit,
    onCustomVolumeBarEnabledChanged: (Boolean) -> Unit,
    onSystemVolumeChanged: (Float) -> Unit,
    onToggleSystemMute: () -> Unit,
    onTestVolumeOverlay: () -> Unit,
    onStartFocusSound: (FocusSoundMode?) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    SectionTitle(
        "Live Voice & Volume Bar Studio",
        "Drag slider or press physical volume keys to experience your active skin"
    )
    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Live Volume / Voice Bar right inside Settings
    ThemedVolumeVoiceBar(
        volumeFraction = volumeFraction,
        isMuted = isVolumeMuted,
        style = settings.volumeBarStyle,
        onVolumeChange = onSystemVolumeChanged,
        onToggleMute = onToggleSystemMute,
        onCycleStyle = {
            val entries = VolumeBarStyleOption.entries
            val next = entries[(entries.indexOf(settings.volumeBarStyle) + 1) % entries.size]
            onVolumeBarStyleSelected(next)
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Hardware Volume Key Interceptor & Pop-Up Trigger Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LiquidGlassPill(
            selected = settings.customVolumeBarEnabled,
            modifier = Modifier.weight(1.2f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCustomVolumeBarEnabledChanged(!settings.customVolumeBarEnabled)
            }
        ) {
            Text(
                text = if (settings.customVolumeBarEnabled) "✓ Hardware Volume HUD Active" else "Enable Hardware Volume HUD",
                style = MaterialTheme.typography.labelMedium,
                color = palette.primaryText
            )
        }

        LiquidGlassPill(
            selected = false,
            modifier = Modifier.weight(0.8f),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onTestVolumeOverlay()
            }
        ) {
            Text(
                text = "Pop Overlay ↑",
                style = MaterialTheme.typography.labelMedium,
                color = palette.accentPrimary
            )
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    SectionTitle(
        "4 Theme-Matched Voice Bar Skins",
        "Automatically adapts to your active Liquid Glass color palette"
    )
    Spacer(modifier = Modifier.height(10.dp))

    // 2x2 Grid of the 4 Volume Bar Skins with Live Miniature Previews
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        VolumeBarStyleOption.entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { styleOption ->
                    VolumeSkinSelectorCard(
                        option = styleOption,
                        selected = settings.volumeBarStyle == styleOption,
                        previewVolume = volumeFraction.coerceAtLeast(0.45f),
                        modifier = Modifier.weight(1f),
                        onSelect = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onVolumeBarStyleSelected(styleOption)
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    SectionTitle(
        "Focus Anchor Acoustic Synthesizer",
        "Zero-latency procedural soundscapes for deep work"
    )
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FocusSoundMode.entries.forEach { mode ->
            val isPlaying = activeFocusSound == mode
            LiquidGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 18.dp,
                elevated = isPlaying,
                accentHighlight = isPlaying,
                paddingHorizontal = 12.dp,
                paddingVertical = 14.dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isPlaying) onStartFocusSound(null) else onStartFocusSound(mode)
                }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isPlaying) "ılılı" else "♪",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isPlaying) palette.accentPrimary else palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = palette.primaryText,
                        maxLines = 1
                    )
                    Text(
                        text = if (isPlaying) "Playing" else "Tap to play",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = if (isPlaying) palette.accentPrimary else palette.mutedText
                    )
                }
            }
        }
    }
}

// ============================================================================
// TAB 4: MINDFUL FOCUS & ANALYTICS
// ============================================================================
@Composable
private fun MindfulAndFocusTab(
    settings: LauncherSettings,
    weeklyAwareness: WeeklyAwarenessSummary,
    onMindfulFrictionEnabledChanged: (Boolean) -> Unit,
    onFrictionSecondsChanged: (Int) -> Unit,
    onHoldToOpenChanged: (Boolean) -> Unit,
    onHoldSecondsChanged: (Int) -> Unit,
    onAutoLaunchChanged: (Boolean) -> Unit,
    onT9SearchChanged: (Boolean) -> Unit,
    onOmniSearchToolsChanged: (Boolean) -> Unit,
    onSwipeDownNotificationsChanged: (Boolean) -> Unit,
    onHomeToggleChanged: (String, Boolean) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    SectionTitle("PathCheck Weekly Awareness", "Intentional vs habitual app launches")
    Spacer(modifier = Modifier.height(10.dp))

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = true,
        accentHighlight = true
    ) {
        val intentionalRatio = (weeklyAwareness.needInfoPct.toFloat() / 100f).coerceIn(0f, 1f)
        val intentionalChecks = ((weeklyAwareness.needInfoPct * weeklyAwareness.totalChecks) / 100)
        val habitChecks = ((weeklyAwareness.habitPct * weeklyAwareness.totalChecks) / 100)
        val boredomChecks = (weeklyAwareness.totalChecks - intentionalChecks - habitChecks).coerceAtLeast(0)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${weeklyAwareness.needInfoPct}% Intentional",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${intentionalChecks} mindful \u00B7 ${habitChecks} habit \u00B7 ${boredomChecks} boredom (${weeklyAwareness.totalChecks} checks)",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Gradient Split Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.borderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = intentionalRatio.coerceIn(0.08f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(palette.accentPrimary, palette.accentSecondary)
                                )
                            )
                    )
                }
            }

            // Mini Radial Ring
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 6.dp.toPx()
                    drawArc(
                        color = palette.borderSubtle,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(palette.accentPrimary, palette.accentSecondary, palette.accentPrimary)
                        ),
                        startAngle = -90f,
                        sweepAngle = intentionalRatio * 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${weeklyAwareness.totalChecks}\u2713",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.accentPrimary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    SectionTitle("Mindful Friction Gate", "Breathing pause before opening distracting apps")
    Spacer(modifier = Modifier.height(10.dp))

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Breathing Pause Duration",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = palette.primaryText
                    )
                    Text(
                        text = if (settings.mindfulFrictionEnabled) "Active (${settings.frictionSeconds}s countdown)" else "Paused globally",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }
                LiquidGlassPill(
                    selected = settings.mindfulFrictionEnabled,
                    onClick = { onMindfulFrictionEnabledChanged(!settings.mindfulFrictionEnabled) }
                ) {
                    Text(
                        text = if (settings.mindfulFrictionEnabled) "Enabled" else "Off",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 5, 10, 15).forEach { seconds ->
                    val selected = settings.frictionSeconds == seconds && settings.mindfulFrictionEnabled
                    LiquidGlassPill(
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onMindfulFrictionEnabledChanged(true)
                            onFrictionSecondsChanged(seconds)
                        }
                    ) {
                        Text(
                            text = "${seconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    SectionTitle("Hold-to-Open Gesture", "Press and hold home apps with radial glow")
    Spacer(modifier = Modifier.height(10.dp))

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hold Gesture Duration",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = palette.primaryText
                    )
                    Text(
                        text = if (settings.enableHoldToOpen) "${settings.holdToOpenSeconds}s hold to launch" else "Instant tap launch",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }
                LiquidGlassPill(
                    selected = settings.enableHoldToOpen,
                    onClick = { onHoldToOpenChanged(!settings.enableHoldToOpen) }
                ) {
                    Text(
                        text = if (settings.enableHoldToOpen) "Hold On" else "Instant Tap",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..5).forEach { sec ->
                    val selected = settings.holdToOpenSeconds == sec && settings.enableHoldToOpen
                    LiquidGlassPill(
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onHoldToOpenChanged(true)
                            onHoldSecondsChanged(sec)
                        }
                    ) {
                        Text(
                            text = "${sec}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    SectionTitle("Omni-Search & Smart Gestures", "Instant search & gesture shortcuts")
    Spacer(modifier = Modifier.height(10.dp))

    val focusTiles = listOf(
        BentoToggleSpec(
            "autoLaunch",
            "Instant Auto-Launch",
            "Open single search match",
            "⚡",
            settings.autoLaunchSingleResult,
            onAutoLaunchChanged
        ),
        BentoToggleSpec(
            "t9Search",
            "T9 Fuzzy Match",
            "Smart initial & keypad filter",
            "⌗",
            settings.enableT9Search,
            onT9SearchChanged
        ),
        BentoToggleSpec(
            "omniTools",
            "Omni-Search Tools",
            "Math, timers, clips & logs",
            "∑",
            settings.omniSearchToolsEnabled,
            onOmniSearchToolsChanged
        ),
        BentoToggleSpec(
            "swipeDown",
            "Swipe Down Shade",
            "Pull notification drawer",
            "↓",
            settings.swipeDownForNotifications,
            onSwipeDownNotificationsChanged
        ),
        BentoToggleSpec(
            "doubleTapLock",
            "Double-Tap Lock",
            "Off = Theme flip · On = Lock",
            "🔒",
            settings.doubleTapToLock
        ) { onHomeToggleChanged("doubleTapToLock", it) }
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        focusTiles.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { spec ->
                    BentoFeatureTile(
                        spec = spec,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            spec.onToggle(!spec.enabled)
                        }
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ============================================================================
// TAB 5: VAULT & MICRO-OS UTILITIES
// ============================================================================
@Composable
private fun VaultAndSystemTab(
    settings: LauncherSettings,
    isDefaultLauncher: Boolean,
    hiddenApps: List<AppEntity>,
    onSetDefaultLauncher: () -> Unit,
    onSwitchSystemLauncher: () -> Unit,
    onOpenQuantumClip: () -> Unit,
    onOpenMicroLog: () -> Unit,
    onOpenBurnerNote: () -> Unit,
    onUnhideApp: (AppEntity) -> Unit,
    onLaunchHiddenApp: (AppEntity) -> Unit,
    onUpdateVaultPasscode: (String) -> Unit,
    onReplayOnboarding: () -> Unit = {}
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    var isVaultUnlocked by remember(settings.vaultPasscode) {
        mutableStateOf(settings.vaultPasscode.isEmpty())
    }
    var passcodeInput by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf(false) }
    var isEditingVaultPasscode by remember { mutableStateOf(false) }
    var newPasscodeDraft by remember { mutableStateOf("") }

    SectionTitle("Onboarding Ritual & Permissions", "Replay tutorial guide & check all system access at once")
    Spacer(modifier = Modifier.height(10.dp))
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        onClick = onReplayOnboarding
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "onboarding tutorial & permissions hub",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
                Text(
                    text = "review gestures, mindful philosophy, and grant all access together",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
            }
            Text(
                text = "launch \u2192",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.accentPrimary
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    SectionTitle("System Home Role", "Default launcher configuration")
    Spacer(modifier = Modifier.height(10.dp))

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevated = true,
        accentHighlight = !isDefaultLauncher
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDefaultLauncher) "ProductiveOS is Default Home" else "Not Set as Default Home",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.primaryText
                )
                Text(
                    text = if (isDefaultLauncher) "Home gesture & back protection active" else "Tap to make ProductiveOS your primary OS home",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
            }
            LiquidGlassPill(
                selected = !isDefaultLauncher,
                onClick = if (isDefaultLauncher) onSwitchSystemLauncher else onSetDefaultLauncher
            ) {
                Text(
                    text = if (isDefaultLauncher) "Switch Home" else "Set Default →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    SectionTitle("Micro-OS Utility Suite", "Zero-context-switch built-in tools")
    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val utilities = listOf(
            Triple("Quantum Clip", "Clipboard vault", onOpenQuantumClip),
            Triple("Micro-Log", "Inbox.md journal", onOpenMicroLog),
            Triple("Burner Note", "24h scratchpad", onOpenBurnerNote)
        )
        utilities.forEach { (title, sub, action) ->
            LiquidGlassCard(
                modifier = Modifier.weight(1f),
                cornerRadius = 18.dp,
                paddingHorizontal = 12.dp,
                paddingVertical = 14.dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    action()
                }
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Open →",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                        color = palette.accentPrimary
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Hidden Apps Vault
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle("Hidden Apps Vault (${hiddenApps.size})", "Private PIN-protected app locker")
        LiquidGlassPill(
            selected = isEditingVaultPasscode,
            onClick = {
                isEditingVaultPasscode = !isEditingVaultPasscode
                newPasscodeDraft = ""
            }
        ) {
            Text(
                text = if (settings.vaultPasscode.isEmpty()) "Set PIN" else "Change PIN",
                style = MaterialTheme.typography.labelMedium,
                color = palette.accentPrimary
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (isEditingVaultPasscode) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
            Column {
                Text(
                    text = "Enter new 4-6 digit Vault PIN (leave blank to remove lock):",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = newPasscodeDraft,
                    onValueChange = { newPasscodeDraft = it.filter { ch -> ch.isDigit() }.take(6) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.primaryText),
                    cursorBrush = SolidColor(palette.accentPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.background.copy(alpha = 0.6f))
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionPillButton(
                    text = "Save Vault PIN",
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onUpdateVaultPasscode(newPasscodeDraft)
                        isEditingVaultPasscode = false
                        isVaultUnlocked = true
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        if (!isVaultUnlocked) {
            Column {
                Text(
                    text = if (passcodeError) "Incorrect PIN · Try again" else "Vault Locked · Enter PIN to reveal hidden apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (passcodeError) palette.accentPrimary else palette.secondaryText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = passcodeInput,
                        onValueChange = {
                            passcodeError = false
                            passcodeInput = it.filter { ch -> ch.isDigit() }.take(6)
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.primaryText),
                        cursorBrush = SolidColor(palette.accentPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.background.copy(alpha = 0.6f))
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    LiquidGlassPill(
                        selected = true,
                        onClick = {
                            if (passcodeInput == settings.vaultPasscode) {
                                isVaultUnlocked = true
                                passcodeError = false
                            } else {
                                passcodeError = true
                            }
                        }
                    ) {
                        Text("Unlock", style = MaterialTheme.typography.labelMedium, color = palette.primaryText)
                    }
                }
            }
        } else if (hiddenApps.isEmpty()) {
            Text(
                text = "Vault is empty. Long-press any app in the search drawer to hide it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hiddenApps.forEachIndexed { idx, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = app.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.primaryText,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onLaunchHiddenApp(app) }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidGlassPill(
                                selected = false,
                                onClick = { onLaunchHiddenApp(app) }
                            ) {
                                Text("Open", style = MaterialTheme.typography.labelMedium, color = palette.primaryText)
                            }
                            LiquidGlassPill(
                                selected = true,
                                onClick = { onUnhideApp(app) }
                            ) {
                                Text("Unhide", style = MaterialTheme.typography.labelMedium, color = palette.primaryText)
                            }
                        }
                    }
                    if (idx < hiddenApps.lastIndex) {
                        HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    val palette = LocalProductiveOSPalette.current
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.primaryText
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
            color = palette.secondaryText
        )
    }
}
