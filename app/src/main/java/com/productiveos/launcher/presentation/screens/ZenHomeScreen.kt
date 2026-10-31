package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.repository.DailyTelemetry
import com.productiveos.launcher.data.repository.WhisperItem
import com.productiveos.launcher.presentation.components.InteractiveGridCanvas
import com.productiveos.launcher.presentation.components.WaveSidebarScrubber
import com.productiveos.launcher.presentation.theme.DoubleBezelSquircle
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.SquircleShape
import com.productiveos.launcher.presentation.theme.jellyPress
import com.productiveos.launcher.util.FocusSoundMode
import com.productiveos.launcher.util.MediaSessionHelper
import com.productiveos.launcher.util.MusicInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SnappyEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun ZenHomeScreen(
    pinnedApps: List<AppEntity>,
    telemetry: DailyTelemetry,
    whisperItem: WhisperItem,
    activeFocusSound: FocusSoundMode?,
    settings: LauncherSettings,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncherClick: () -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onTelemetryPillClick: () -> Unit,
    onWhisperClick: () -> Unit,
    onToggleFocusSound: () -> Unit,
    onOpenBurnerNote: () -> Unit,
    onOpenQuantumClip: () -> Unit,
    onAppClick: (AppEntity) -> Unit,
    onAppLongClick: (AppEntity) -> Unit,
    onAppSwipeRight: (AppEntity) -> Unit,
    onSwipeUpSearch: () -> Unit,
    onSwipeDownNotifications: () -> Unit,
    onCanvasSwipeLeft: () -> Unit,
    onCanvasSwipeRight: () -> Unit,
    onDoubleTapEmptySpace: () -> Unit,
    onLongPressEmptySpace: () -> Unit,
    onHomeEdgeLetterSelected: (String) -> Unit = { onSwipeUpSearch() }
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            val delayMs = 60_000L - (nowMillis % 60_000L)
            delay(delayMs.coerceIn(1_000L, 60_000L))
        }
    }

    var musicInfo by remember { mutableStateOf<MusicInfo?>(null) }
    LaunchedEffect(settings.showMusicCapsule) {
        if (!settings.showMusicCapsule) {
            musicInfo = null
            return@LaunchedEffect
        }
        while (true) {
            musicInfo = MediaSessionHelper.query(context)
            delay(2000L)
        }
    }

    val timeFormatted = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }
    val dateFormatted = remember(nowMillis) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
    }

    val horizontalAlignment = when (settings.textAlignment) {
        TextAlignmentOption.LEFT -> Alignment.Start
        TextAlignmentOption.CENTER -> Alignment.CenterHorizontally
        TextAlignmentOption.RIGHT -> Alignment.End
    }

    val textAlign = when (settings.textAlignment) {
        TextAlignmentOption.LEFT -> TextAlign.Start
        TextAlignmentOption.CENTER -> TextAlign.Center
        TextAlignmentOption.RIGHT -> TextAlign.End
    }

    // Track global touch coordinates for the Interactive Grid Canvas & Active Hold Pause
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var currentlyHoldingPackage by remember { mutableStateOf<String?>(null) }
    var activeHoldProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activeChange = event.changes.firstOrNull { it.pressed }
                        activeTouchPoint = activeChange?.position
                        if (event.changes.none { it.pressed }) {
                            break
                        }
                    }
                    activeTouchPoint = null
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapEmptySpace() },
                    onLongPress = { onLongPressEmptySpace() }
                )
            }
            .pointerInput(settings.swipeDownForNotifications) {
                var totalDragY = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        if (totalDragY < -70f) {
                            onSwipeUpSearch()
                        } else if (totalDragY > 70f && settings.swipeDownForNotifications) {
                            onSwipeDownNotifications()
                        }
                        totalDragY = 0f
                    },
                    onDragCancel = { totalDragY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDragY += dragAmount
                    }
                )
            }
    ) {
        // Layer 0: Interactive Kinetic Background Grid Canvas
        if (settings.showInteractiveGrid) {
            InteractiveGridCanvas(
                touchPoint = activeTouchPoint,
                holdProgress = activeHoldProgress
            )
        }

        // Layer 1: Main Home Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 28.dp,
                    end = if (settings.showHomeEdgeSidebar) 34.dp else 28.dp,
                    top = 24.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = horizontalAlignment
        ) {
            // Top Glance Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                if (!isDefaultLauncher) {
                    DoubleBezelSquircle(
                        modifier = Modifier.padding(bottom = 20.dp),
                        outerRadius = 20.dp,
                        bezelThickness = 1.5.dp,
                        innerPaddingHorizontal = 16.dp,
                        innerPaddingVertical = 8.dp,
                        elevated = true,
                        onClick = onSetDefaultLauncherClick
                    ) {
                        Text(
                            text = "Set ProductiveOS as Default Home →",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText
                        )
                    }
                }

                if (settings.showClock) {
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.displayLarge,
                        color = palette.primaryText,
                        textAlign = textAlign,
                        modifier = Modifier
                            .jellyPress(0.96f)
                            .clickable(onClick = onClockClick)
                    )
                }

                if (settings.showDate) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateFormatted.lowercase(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.secondaryText,
                        textAlign = textAlign,
                        modifier = Modifier
                            .jellyPress(0.96f)
                            .clickable(onClick = onDateClick)
                    )
                }

                // Double-Bezel Music Capsule under Time and Date
                AnimatedVisibility(
                    visible = settings.showMusicCapsule && musicInfo != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val m = musicInfo
                    if (m != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DoubleBezelSquircle(
                            outerRadius = 20.dp,
                            bezelThickness = 1.5.dp,
                            innerPaddingHorizontal = 14.dp,
                            innerPaddingVertical = 6.dp,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                MediaSessionHelper.playPause(context)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (m.isPlaying) "▶" else "❚❚",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val trackText = if (m.artist.isNotBlank()) "${m.title} · ${m.artist}".lowercase() else m.title.lowercase()
                                Text(
                                    text = trackText.take(30),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.primaryText
                                )
                                if (m.hasControls) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "⏭",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.secondaryText,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                MediaSessionHelper.next(context)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (settings.showTelemetry) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Double-Bezel Screen-Time & Focus Telemetry Pill
                    DoubleBezelSquircle(
                        outerRadius = 20.dp,
                        bezelThickness = 1.5.dp,
                        innerPaddingHorizontal = 14.dp,
                        innerPaddingVertical = 7.dp,
                        onClick = onTelemetryPillClick
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentDot)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = telemetry.formattedText,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                        }
                    }
                }

                // Focus Anchor Live Waveform Pill (when active)
                AnimatedVisibility(
                    visible = activeFocusSound != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (activeFocusSound != null) {
                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .jellyPress(0.95f)
                                .clip(RoundedCornerShape(50))
                                .background(palette.surfaceElevated)
                                .border(1.dp, palette.primaryText.copy(alpha = 0.45f), RoundedCornerShape(50))
                                .clickable(onClick = onToggleFocusSound)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ılılı  focus anchor · ${activeFocusSound.label} · tap to stop",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryText
                            )
                        }
                    }
                }

                if (settings.showWhisper) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Editorial Contextual Whisper Header
                    AnimatedContent(
                        targetState = whisperItem,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "WhisperTicker"
                    ) { item ->
                        Row(
                            modifier = Modifier
                                .jellyPress(0.96f)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onWhisperClick)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.mutedText
                            )
                            Text(
                                text = item.text.lowercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.mutedText,
                                textAlign = textAlign
                            )
                        }
                    }
                }
            }

            // Center / Lower Third Pinned Essentials (4 to 6 Text Items)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                if (pinnedApps.isEmpty()) {
                    DoubleBezelSquircle(
                        modifier = Modifier.fillMaxWidth(),
                        outerRadius = 22.dp,
                        bezelThickness = 2.dp,
                        innerPaddingHorizontal = 20.dp,
                        innerPaddingVertical = 20.dp,
                        onClick = onSwipeUpSearch
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "productiveos · zen space",
                                style = MaterialTheme.typography.headlineMedium,
                                color = palette.primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "swipe up or tap here to open apps & pin essentials",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    pinnedApps.take(6).forEach { app ->
                        val isAnotherHolding = currentlyHoldingPackage != null && currentlyHoldingPackage != app.packageName
                        IntentionalHoldAppRow(
                            app = app,
                            settings = settings,
                            textAlign = textAlign,
                            isDimmedByOtherHold = isAnotherHolding,
                            onHoldStateChange = { holding, progress ->
                                if (holding) {
                                    currentlyHoldingPackage = app.packageName
                                    activeHoldProgress = progress
                                } else if (currentlyHoldingPackage == app.packageName) {
                                    currentlyHoldingPackage = null
                                    activeHoldProgress = 0f
                                }
                            },
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick
                        )
                    }
                }
            }

            // Bottom Navigation / Micro-OS Dock Trigger
            if (settings.showIconDock) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DoubleBezelSquircle(
                        outerRadius = 24.dp,
                        bezelThickness = 1.5.dp,
                        innerPaddingHorizontal = 16.dp,
                        innerPaddingVertical = 8.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "↑ search",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText,
                                modifier = Modifier
                                    .jellyPress(0.95f)
                                    .clickable(onClick = onSwipeUpSearch)
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.mutedText
                            )
                            Text(
                                text = "burner",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText,
                                modifier = Modifier
                                    .jellyPress(0.95f)
                                    .clickable(onClick = onOpenBurnerNote)
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.mutedText
                            )
                            Text(
                                text = "clips",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText,
                                modifier = Modifier
                                    .jellyPress(0.95f)
                                    .clickable(onClick = onOpenQuantumClip)
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.mutedText
                            )
                            Text(
                                text = "settings",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText,
                                modifier = Modifier
                                    .jellyPress(0.95f)
                                    .clickable(onClick = onLongPressEmptySpace)
                            )
                        }
                    }
                }
            }
        }

        // Layer 2: Right-Edge Gaussian Sine-Wave Sidebar on Home Screen
        if (settings.showHomeEdgeSidebar) {
            WaveSidebarScrubber(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 84.dp, end = 2.dp),
                compactIdleMode = true,
                onLetterScrubbed = { /* Live wave preview */ },
                onScrubReleased = { letter ->
                    if (letter != null) {
                        onHomeEdgeLetterSelected(letter)
                    }
                }
            )
        }
    }
}

/**
 * Block 2: Asymmetric "Long-Touch Pause" Ritual Row.
 *
 * Features:
 * - Full-row liquid clip-path progress sweep (0% -> 100% width) with inverted contrast text inside the clipped wave.
 * - Asymmetric Enter/Exit timing: deliberate linear progression during hold, snappy 180ms `--ease-out` snap-back on release.
 * - Peripheral focus dimming when another row is being held.
 */
@Composable
private fun IntentionalHoldAppRow(
    app: AppEntity,
    settings: LauncherSettings,
    textAlign: TextAlign,
    isDimmedByOtherHold: Boolean,
    onHoldStateChange: (Boolean, Float) -> Unit,
    onAppClick: (AppEntity) -> Unit,
    onAppLongClick: (AppEntity) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var isHolding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val holdDurationMs = (settings.holdToOpenSeconds * 1000L).coerceAtLeast(1000L)
    val remainingSeconds = ((1f - progress.value) * settings.holdToOpenSeconds).toInt() + 1

    val rowAlpha by animateFloatAsState(
        targetValue = if (isDimmedByOtherHold) 0.32f else 1f,
        animationSpec = tween(durationMillis = 200, easing = SnappyEaseOut),
        label = "PeripheralFocusAlpha"
    )

    val holdScale by animateFloatAsState(
        targetValue = if (isHolding) 0.975f else 1f,
        animationSpec = tween(durationMillis = 160, easing = SnappyEaseOut),
        label = "HoldScale"
    )

    val rowShape = remember { SquircleShape(14.dp) }
    val fillFraction = progress.value.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = rowAlpha
                scaleX = holdScale
                scaleY = holdScale
            }
            .clip(rowShape)
            .background(
                if (isHolding) palette.surface.copy(alpha = 0.75f) else Color.Transparent
            )
            .border(
                width = if (isHolding) 1.dp else 0.dp,
                color = if (isHolding) palette.borderSubtle else Color.Transparent,
                shape = rowShape
            )
            .drawWithContent {
                // Draw base row
                drawContent()

                // Draw liquid clip-path sweep overlay during hold
                if (fillFraction > 0.005f) {
                    val clipWidth = size.width * fillFraction
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = clipWidth,
                        bottom = size.height
                    ) {
                        drawRect(
                            color = palette.primaryText.copy(alpha = 0.14f)
                        )
                    }
                    // Leading edge glowing meniscus line
                    drawLine(
                        color = palette.primaryText.copy(alpha = 0.65f),
                        start = Offset(clipWidth, 0f),
                        end = Offset(clipWidth, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .pointerInput(settings.enableHoldToOpen, settings.holdToOpenSeconds) {
                if (!settings.enableHoldToOpen) {
                    detectTapGestures(
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAppClick(app)
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAppLongClick(app)
                        }
                    )
                } else {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            progress.snapTo(0f)
                            onHoldStateChange(true, 0f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                            val animJob = coroutineScope.launch {
                                val startTime = System.currentTimeMillis()
                                var lastTick = startTime
                                while (System.currentTimeMillis() - startTime < holdDurationMs) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    val p = (elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f)
                                    progress.snapTo(p)
                                    onHoldStateChange(true, p)
                                    if (System.currentTimeMillis() - lastTick >= 600L) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastTick = System.currentTimeMillis()
                                    }
                                    delay(16L)
                                }
                                progress.snapTo(1f)
                                onHoldStateChange(true, 1f)
                            }

                            tryAwaitRelease()
                            animJob.cancel()
                            val completed = progress.value >= 0.98f
                            isHolding = false
                            if (completed) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onHoldStateChange(false, 0f)
                                progress.snapTo(0f)
                                onAppClick(app)
                            } else {
                                // Snappy 180ms ease-out release回弹
                                coroutineScope.launch {
                                    progress.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 180, easing = SnappyEaseOut)
                                    )
                                    onHoldStateChange(false, 0f)
                                }
                            }
                        },
                        onLongPress = {
                            if (!isHolding) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppLongClick(app)
                            }
                        }
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = when (settings.textAlignment) {
                TextAlignmentOption.LEFT -> Arrangement.Start
                TextAlignmentOption.CENTER -> Arrangement.Center
                TextAlignmentOption.RIGHT -> Arrangement.End
            }
        ) {
            Text(
                text = app.displayName.lowercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.primaryText,
                textAlign = textAlign
            )

            if (app.isFrictionEnabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(palette.mutedText)
                )
            }

            if ((isHolding || fillFraction > 0.05f) && settings.enableHoldToOpen) {
                Spacer(modifier = Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 2.dp.toPx()
                            drawArc(
                                color = palette.borderSubtle,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke)
                            )
                            drawArc(
                                color = palette.primaryText,
                                startAngle = -90f,
                                sweepAngle = fillFraction * 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "pause ${remainingSeconds}s",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = palette.secondaryText
                    )
                }
            }
        }
    }
}
