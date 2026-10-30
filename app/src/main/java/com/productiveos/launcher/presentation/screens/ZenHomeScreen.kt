package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.repository.DailyTelemetry
import com.productiveos.launcher.data.repository.WhisperItem
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.tactilePress
import com.productiveos.launcher.util.FocusSoundMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onLongPressEmptySpace: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            val delayMs = 60_000L - (nowMillis % 60_000L)
            delay(delayMs.coerceIn(1_000L, 60_000L))
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

    var totalCanvasDragX by remember { mutableFloatStateOf(0f) }
    var totalCanvasDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
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
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceElevated)
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                            .clickable(onClick = onSetDefaultLauncherClick)
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "Set ProductiveOS as Default Home →",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.primaryText,
                    textAlign = textAlign,
                    modifier = Modifier.clickable(onClick = onClockClick)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateFormatted.lowercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.secondaryText,
                    textAlign = textAlign,
                    modifier = Modifier.clickable(onClick = onDateClick)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Screen-Time & Focus Telemetry Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                        .clickable(onClick = onTelemetryPillClick)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
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

                Spacer(modifier = Modifier.height(8.dp))

                // Editorial Contextual Whisper Header
                AnimatedContent(
                    targetState = whisperItem,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "WhisperTicker"
                ) { item ->
                    Row(
                        modifier = Modifier
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

            // Center / Lower Third Pinned Essentials (4 to 6 Text Items)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                if (pinnedApps.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
                            .clickable(onClick = onSwipeUpSearch)
                            .padding(20.dp),
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
                } else {
                    pinnedApps.take(6).forEach { app ->
                        IntentionalHoldAppRow(
                            app = app,
                            settings = settings,
                            textAlign = textAlign,
                            onAppClick = onAppClick,
                            onAppLongClick = onAppLongClick
                        )
                    }
                }
            }

            // Bottom Navigation / Micro-OS Dock Trigger
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "↑ search",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier.clickable(onClick = onSwipeUpSearch)
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
                        modifier = Modifier.clickable(onClick = onOpenBurnerNote)
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
                        modifier = Modifier.clickable(onClick = onOpenQuantumClip)
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
                        modifier = Modifier.clickable(onClick = onLongPressEmptySpace)
                    )
                }
            }
        }
    }
}

@Composable
private fun IntentionalHoldAppRow(
    app: AppEntity,
    settings: LauncherSettings,
    textAlign: TextAlign,
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHolding) palette.surfaceElevated.copy(alpha = 0.6f) else Color.Transparent)
            .border(
                width = if (isHolding) 1.dp else 0.dp,
                color = if (isHolding) palette.borderSubtle else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
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
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                            val animJob = coroutineScope.launch {
                                val startTime = System.currentTimeMillis()
                                var lastTick = startTime
                                while (System.currentTimeMillis() - startTime < holdDurationMs) {
                                    val elapsed = System.currentTimeMillis() - startTime
                                    progress.snapTo((elapsed.toFloat() / holdDurationMs).coerceIn(0f, 1f))
                                    if (System.currentTimeMillis() - lastTick >= 700L) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        lastTick = System.currentTimeMillis()
                                    }
                                    delay(16L)
                                }
                                progress.snapTo(1f)
                            }

                            val released = tryAwaitRelease()
                            animJob.cancel()
                            if (progress.value >= 0.98f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppClick(app)
                            }
                            isHolding = false
                            progress.snapTo(0f)
                        },
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAppLongClick(app)
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

            if (isHolding && settings.enableHoldToOpen) {
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
                                sweepAngle = progress.value * 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "hold ${remainingSeconds}s",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = palette.secondaryText
                    )
                }
            }
        }
    }
}
