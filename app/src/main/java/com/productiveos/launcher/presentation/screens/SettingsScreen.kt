package com.productiveos.launcher.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.LauncherThemeOption
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.preferences.TypographyStyleOption
import com.productiveos.launcher.data.repository.WeeklyAwarenessSummary
import com.productiveos.launcher.presentation.components.ActionPillButton
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.util.FocusSoundMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    isDefaultLauncher: Boolean,
    pinnedApps: List<AppEntity>,
    hiddenApps: List<AppEntity>,
    weeklyAwareness: WeeklyAwarenessSummary,
    activeFocusSound: FocusSoundMode?,
    onBackToHome: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onSwitchSystemLauncher: () -> Unit,
    onTextAlignmentSelected: (TextAlignmentOption) -> Unit,
    onTypographySelected: (TypographyStyleOption) -> Unit,
    onThemeSelected: (LauncherThemeOption) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onAutoLaunchChanged: (Boolean) -> Unit,
    onT9SearchChanged: (Boolean) -> Unit,
    onFrictionSecondsChanged: (Int) -> Unit,
    onMindfulFrictionEnabledChanged: (Boolean) -> Unit,
    onStatusBarStealthChanged: (Boolean) -> Unit,
    onBottomDockedSearchChanged: (Boolean) -> Unit,
    onSwipeDownNotificationsChanged: (Boolean) -> Unit,
    onOmniSearchToolsChanged: (Boolean) -> Unit,
    onAutoDayNightThemeChanged: (Boolean) -> Unit,
    onSyncWallpaperClicked: () -> Unit,
    onOpenQuantumClip: () -> Unit,
    onOpenMicroLog: () -> Unit,
    onOpenBurnerNote: () -> Unit,
    onStartFocusSound: (FocusSoundMode?) -> Unit,
    onMovePinnedApp: (AppEntity, Boolean) -> Unit,
    onUnpinApp: (AppEntity) -> Unit,
    onUnhideApp: (AppEntity) -> Unit,
    onLaunchHiddenApp: (AppEntity) -> Unit,
    onUpdateVaultPasscode: (String) -> Unit,
    onHoldToOpenChanged: (Boolean) -> Unit = {},
    onHoldSecondsChanged: (Int) -> Unit = {},
    onRequestNotificationAccess: () -> Unit = {},
    onHomeToggleChanged: (String, Boolean) -> Unit = { _, _ -> }
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var isVaultUnlocked by remember(settings.vaultPasscode) {
        mutableStateOf(settings.vaultPasscode.isEmpty())
    }
    var passcodeInput by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf(false) }
    var isEditingVaultPasscode by remember { mutableStateOf(false) }
    var newPasscodeDraft by remember { mutableStateOf("") }

    var lastSliderValue by remember { mutableFloatStateOf(settings.fontScale) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "productiveos settings",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.primaryText
            )
            Text(
                text = "← done",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBackToHome)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = palette.borderSubtle)
        Spacer(modifier = Modifier.height(20.dp))

        // Section 1: System Default Launcher Role
        SectionHeader(title = "system home role")
        Spacer(modifier = Modifier.height(10.dp))

        ActionPillButton(
            text = if (isDefaultLauncher) {
                "✓ ProductiveOS is Default System Launcher"
            } else {
                "Set as Default System Launcher →"
            },
            isPrimary = !isDefaultLauncher,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSetDefaultLauncher
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionPillButton(
            text = "Switch System Launcher (Android Home Picker)",
            isPrimary = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSwitchSystemLauncher
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Section 2: Text Alignment
        SectionHeader(title = "text alignment")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextAlignmentOption.entries.forEach { option ->
                val selected = settings.textAlignment == option
                OptionChip(
                    label = option.title.lowercase(),
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    onClick = { onTextAlignmentSelected(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section 3: Typography Style
        SectionHeader(title = "typography style")
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TypographyStyleOption.entries.forEach { option ->
                val selected = settings.typographyStyle == option
                OptionChip(
                    label = option.title.lowercase(),
                    selected = selected,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onTypographySelected(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section 4: Theme & Contrast
        SectionHeader(title = "surface theme (double-tap home to toggle)")
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LauncherThemeOption.entries.forEach { option ->
                val selected = settings.themeOption == option
                OptionChip(
                    label = "${option.title.lowercase()} (${option.hexLabel})",
                    selected = selected,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onThemeSelected(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section 5: Font Scaling with haptic steps
        SectionHeader(
            title = "font scale · ${(settings.fontScale * 100).roundToInt()}%"
        )
        Slider(
            value = settings.fontScale,
            onValueChange = { newVal ->
                if (kotlin.math.abs(newVal - lastSliderValue) > 0.04f) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastSliderValue = newVal
                }
                onFontScaleChanged(newVal)
            },
            valueRange = 0.85f..1.30f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = palette.primaryText,
                activeTrackColor = palette.primaryText,
                inactiveTrackColor = palette.borderSubtle
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        // BLOCK 4, ITEM 1: GESTURES & ERGONOMICS
        SectionHeader(title = "gestures & ergonomics")
        Spacer(modifier = Modifier.height(10.dp))

        ToggleSettingRow(
            title = "swipe down for notifications",
            subtitle = "expand system notification shade by swiping down anywhere on home",
            checked = settings.swipeDownForNotifications,
            onCheckedChange = onSwipeDownNotificationsChanged
        )

        ToggleSettingRow(
            title = "bottom-docked search bar",
            subtitle = "anchor search input bar directly above the keyboard for one-hand thumb use",
            checked = settings.bottomDockedSearch,
            onCheckedChange = onBottomDockedSearchChanged
        )

        ToggleSettingRow(
            title = "status bar visibility",
            subtitle = if (settings.statusBarStealth) "stealth mode (hidden status bar)" else "visible status bar",
            checked = !settings.statusBarStealth,
            onCheckedChange = { visible -> onStatusBarStealthChanged(!visible) }
        )

        ToggleSettingRow(
            title = "double tap to lock screen",
            subtitle = "double tap any empty space on home screen to turn off and lock the display",
            checked = settings.doubleTapToLock,
            onCheckedChange = { onHomeToggleChanged("doubleTapToLock", it) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // HOME SCREEN CUSTOMIZATION (WIDGETS & ELEMENTS TOGGLE)
        SectionHeader(title = "home screen customization")
        Spacer(modifier = Modifier.height(10.dp))

        ToggleSettingRow(
            title = "digital clock",
            subtitle = "show main headline time on home screen",
            checked = settings.showClock,
            onCheckedChange = { onHomeToggleChanged("showClock", it) }
        )

        ToggleSettingRow(
            title = "current date & day",
            subtitle = "show full weekday and date subtitle",
            checked = settings.showDate,
            onCheckedChange = { onHomeToggleChanged("showDate", it) }
        )

        ToggleSettingRow(
            title = "music capsule",
            subtitle = "compact media player pill below date with playback controls",
            checked = settings.showMusicCapsule,
            onCheckedChange = { onHomeToggleChanged("showMusicCapsule", it) }
        )

        ToggleSettingRow(
            title = "screen time telemetry pill",
            subtitle = "show daily hours and unlock counts pill",
            checked = settings.showTelemetry,
            onCheckedChange = { onHomeToggleChanged("showTelemetry", it) }
        )

        ToggleSettingRow(
            title = "contextual whisper header",
            subtitle = "show dynamic calendar, upcoming events, or battery status ticker",
            checked = settings.showWhisper,
            onCheckedChange = { onHomeToggleChanged("showWhisper", it) }
        )

        ToggleSettingRow(
            title = "bottom navigation dock",
            subtitle = "show bottom micro-os quick navigation pills (search, burner, clips, settings)",
            checked = settings.showIconDock,
            onCheckedChange = { onHomeToggleChanged("showIconDock", it) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // BLOCK 4, ITEM 2: MINDFUL FRICTION GATE & PATHCHECK AWARENESS
        SectionHeader(title = "mindful friction gate & pathcheck")
        Spacer(modifier = Modifier.height(10.dp))

        ToggleSettingRow(
            title = "mindful breathing countdown",
            subtitle = "require an intentional breathing pause before opening distracting apps",
            checked = settings.mindfulFrictionEnabled,
            onCheckedChange = onMindfulFrictionEnabledChanged
        )

        if (settings.mindfulFrictionEnabled) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "breathing pause length",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(3, 5, 10).forEach { seconds ->
                    OptionChip(
                        label = "${seconds}s pause",
                        selected = settings.frictionSeconds == seconds,
                        modifier = Modifier.weight(1f),
                        onClick = { onFrictionSecondsChanged(seconds) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Intentional 3-Second Hold-to-Open Gate (Impulse & Mis-touch Protection)
        SectionHeader(title = "intentional hold-to-open gate")
        Spacer(modifier = Modifier.height(10.dp))

        ToggleSettingRow(
            title = "require hold to open apps",
            subtitle = "prevents accidental pocket touches & impulsive twitch-opening of apps",
            checked = settings.enableHoldToOpen,
            onCheckedChange = onHoldToOpenChanged
        )

        if (settings.enableHoldToOpen) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "hold duration",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1, 2, 3).forEach { seconds ->
                    OptionChip(
                        label = "${seconds}s hold",
                        selected = settings.holdToOpenSeconds == seconds,
                        modifier = Modifier.weight(1f),
                        onClick = { onHoldSecondsChanged(seconds) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PathCheck 7-Day Weekly Awareness Ratio Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-day pathcheck awareness",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.primaryText
                )
                Text(
                    text = "${weeklyAwareness.needInfoPct}% intentional",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = weeklyAwareness.summaryText,
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "need info: ${weeklyAwareness.needInfoPct}% · boredom: ${weeklyAwareness.boredomPct}% · habit: ${weeklyAwareness.habitPct}% (${weeklyAwareness.totalChecks} checks)",
                style = MaterialTheme.typography.labelMedium,
                color = palette.mutedText
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // MICRO-OS UTILITIES HUB
        SectionHeader(title = "micro-os utilities (native zero-bloat tools)")
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OptionChip(
                label = "quantum clips (c/)",
                selected = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenQuantumClip
            )
            OptionChip(
                label = "micro-log (inbox.md)",
                selected = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenMicroLog
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OptionChip(
            label = "open 24h burner scratchpad →",
            selected = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenBurnerNote
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "focus anchor acoustic synthesizer",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusSoundMode.entries.forEach { mode ->
                OptionChip(
                    label = when (mode) {
                        FocusSoundMode.RAIN -> "rain"
                        FocusSoundMode.CAFE_BROWN -> "cafe"
                        FocusSoundMode.BINAURAL_ALPHA -> "alpha"
                    },
                    selected = activeFocusSound == mode,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (activeFocusSound == mode) {
                            onStartFocusSound(null)
                        } else {
                            onStartFocusSound(mode)
                        }
                    }
                )
            }
            if (activeFocusSound != null) {
                OptionChip(
                    label = "stop",
                    selected = false,
                    onClick = { onStartFocusSound(null) }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // BLOCK 4, ITEM 3: OMNI-SEARCH TOOLS
        SectionHeader(title = "omni-search tools")
        Spacer(modifier = Modifier.height(10.dp))

        ToggleSettingRow(
            title = "inline calculator & bang commands",
            subtitle = "evaluates math (+ - * / ^ %), ? web search, @ maps, and t 15 focus timers directly in search bar",
            checked = settings.omniSearchToolsEnabled,
            onCheckedChange = onOmniSearchToolsChanged
        )

        ToggleSettingRow(
            title = "auto-launch single search result",
            subtitle = "instantly open app when typing filters down to 1 match",
            checked = settings.autoLaunchSingleResult,
            onCheckedChange = onAutoLaunchChanged
        )

        ToggleSettingRow(
            title = "t9 numeric search mapping",
            subtitle = "filter apps using numeric keypad combinations (2=abc, 3=def)",
            checked = settings.enableT9Search,
            onCheckedChange = onT9SearchChanged
        )

        Spacer(modifier = Modifier.height(28.dp))

        // BLOCK 4, ITEM 4: DAYLIGHT AUTO-THEME & WALLPAPER
        SectionHeader(title = "daylight auto-theme & wallpaper")
        Spacer(modifier = Modifier.height(10.dp))

        ToggleSettingRow(
            title = "auto daylight / night theme",
            subtitle = "warm stone (light) from 6:00 am to 5:00 pm, then pure oled black at night",
            checked = settings.autoDayNightTheme,
            onCheckedChange = onAutoDayNightThemeChanged
        )

        Spacer(modifier = Modifier.height(10.dp))

        ActionPillButton(
            text = "Sync System & Lock Wallpapers to Theme",
            isPrimary = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSyncWallpaperClicked
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Section: Pinned Essentials Ordering
        SectionHeader(title = "pinned home essentials (${pinnedApps.size}/6)")
        Spacer(modifier = Modifier.height(10.dp))
        if (pinnedApps.isEmpty()) {
            Text(
                text = "no apps pinned. long-press any app in search drawer to pin.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.mutedText
            )
        } else {
            pinnedApps.forEachIndexed { index, app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${app.displayName.lowercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.primaryText,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (index > 0) {
                            Text(
                                text = "↑",
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.secondaryText,
                                modifier = Modifier.clickable { onMovePinnedApp(app, true) }
                            )
                        }
                        if (index < pinnedApps.lastIndex) {
                            Text(
                                text = "↓",
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.secondaryText,
                                modifier = Modifier.clickable { onMovePinnedApp(app, false) }
                            )
                        }
                        Text(
                            text = "unpin",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.mutedText,
                            modifier = Modifier.clickable { onUnpinApp(app) }
                        )
                    }
                }
                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.4f))
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Section: Hidden Apps Vault
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "hidden apps vault (${hiddenApps.size})")
            Text(
                text = if (settings.vaultPasscode.isEmpty()) "set pin lock" else "change pin",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText,
                modifier = Modifier.clickable {
                    isEditingVaultPasscode = !isEditingVaultPasscode
                    newPasscodeDraft = ""
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isEditingVaultPasscode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "enter new vault pin (leave blank to remove pin):",
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
                    cursorBrush = SolidColor(palette.primaryText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.background)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionPillButton(
                    text = "save vault pin",
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onUpdateVaultPasscode(newPasscodeDraft)
                        isEditingVaultPasscode = false
                        isVaultUnlocked = true
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!isVaultUnlocked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "vault locked · enter pin to view hidden apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (passcodeError) palette.primaryText else palette.secondaryText
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
                        cursorBrush = SolidColor(palette.primaryText),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.background)
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                    ActionPillButton(
                        text = "unlock",
                        isPrimary = true,
                        onClick = {
                            if (passcodeInput == settings.vaultPasscode) {
                                isVaultUnlocked = true
                                passcodeError = false
                            } else {
                                passcodeError = true
                            }
                        }
                    )
                }
            }
        } else if (hiddenApps.isEmpty()) {
            Text(
                text = "vault is empty. long-press any app in search drawer to hide it.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.mutedText
            )
        } else {
            hiddenApps.forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = app.displayName.lowercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.primaryText,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLaunchHiddenApp(app) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "open",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryText,
                            modifier = Modifier.clickable { onLaunchHiddenApp(app) }
                        )
                        Text(
                            text = "unhide",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText,
                            modifier = Modifier.clickable { onUnhideApp(app) }
                        )
                    }
                }
                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.4f))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    val palette = LocalProductiveOSPalette.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = palette.mutedText
    )
}

@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val bgColor = if (selected) palette.primaryText else palette.surface
    val textColor = if (selected) palette.background else palette.primaryText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (selected) palette.primaryText else palette.borderSubtle,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.primaryText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.primaryText,
                uncheckedThumbColor = palette.secondaryText,
                uncheckedTrackColor = palette.surface
            )
        )
    }
}
