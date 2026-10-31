package com.productiveos.launcher.presentation.screens

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.repository.DailyTelemetry
import com.productiveos.launcher.data.repository.WhisperItem
import com.productiveos.launcher.presentation.components.FloatingHomeIcons
import com.productiveos.launcher.presentation.components.HoldToOpenHost
import com.productiveos.launcher.presentation.components.InteractiveGridCanvas
import com.productiveos.launcher.presentation.components.SnappyEaseOut
import com.productiveos.launcher.presentation.components.WaveSidebarScrubber
import com.productiveos.launcher.presentation.components.rememberTilt
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
import kotlin.math.abs

/** How long an empty patch of the home screen must be held before settings open. */
private const val SETTINGS_HOLD_MS = 1100L
private const val SETTINGS_HOLD_DELAY_MS = 280L

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
    val scope = rememberCoroutineScope()
    val tilt = rememberTilt(enabled = settings.floatingHomeIcons || settings.showInteractiveGrid)

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

    // Per-frame interaction state. Only the *providers* are read by canvases, so none of this
    // recomposes the screen while a finger is moving.
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }
    val holdProgress = remember { mutableFloatStateOf(0f) }
    var holdingPackage by remember { mutableStateOf<String?>(null) }
    val pullPx = remember { mutableFloatStateOf(0f) }
    val settingsHold = remember { mutableFloatStateOf(0f) }
    var settingsHoldPoint by remember { mutableStateOf(Offset.Zero) }
    val bloom = remember { Animatable(0f) }
    var bloomOrigin by remember { mutableStateOf(Offset.Zero) }
    var containerOrigin by remember { mutableStateOf(Offset.Zero) }

    val onHoldChange: (String?, Float) -> Unit = { pkg, p ->
        if (holdingPackage != pkg) holdingPackage = pkg
        holdProgress.floatValue = if (pkg == null) 0f else p
    }

    val launchWithBloom: (AppEntity, Offset) -> Unit = { app, center ->
        bloomOrigin = center - containerOrigin
        scope.launch {
            bloom.snapTo(0f)
            bloom.animateTo(1f, tween(540, easing = SnappyEaseOut))
        }
        scope.launch {
            delay(110L)
            onAppClick(app)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .onGloballyPositioned { containerOrigin = it.positionInWindow() }
            // Observe touches without consuming them (feeds the grid + hold glows).
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val active = event.changes.firstOrNull { it.pressed }
                        activeTouchPoint = active?.position
                        if (active == null) break
                    }
                    activeTouchPoint = null
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onDoubleTapEmptySpace() })
            }
            // Hold an empty patch (longer than a normal long-press) to open settings, with a growing glow.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val slop = viewConfiguration.touchSlop * 1.5f
                    val start = SystemClock.uptimeMillis()
                    var fired = false
                    settingsHoldPoint = down.position
                    while (true) {
                        val event = withTimeoutOrNull(16L) { awaitPointerEvent() }
                        val elapsed = SystemClock.uptimeMillis() - start
                        if (event != null) {
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            if ((change.position - down.position).getDistance() > slop) break
                        }
                        if (holdingPackage != null) break
                        val p = ((elapsed - SETTINGS_HOLD_DELAY_MS).toFloat() /
                            (SETTINGS_HOLD_MS - SETTINGS_HOLD_DELAY_MS).toFloat()).coerceIn(0f, 1f)
                        settingsHold.floatValue = p
                        if (p >= 1f) {
                            fired = true
                            break
                        }
                    }
                    settingsHold.floatValue = 0f
                    if (fired) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPressEmptySpace()
                    }
                }
            }
            // Velocity-tracked pull: the home content follows the finger, then springs open or back.
            .pointerInput(settings.swipeDownForNotifications) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    val slop = viewConfiguration.touchSlop
                    var dragging = false
                    var total = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val dy = change.position.y - down.position.y
                        val dx = change.position.x - down.position.x
                        if (!dragging) {
                            if (abs(dx) > slop * 1.5f && abs(dx) > abs(dy)) break
                            if (abs(dy) > slop && abs(dy) > abs(dx) * 1.2f && holdingPackage == null) {
                                dragging = true
                            }
                        }
                        if (dragging) {
                            total = dy
                            pullPx.floatValue = if (dy < 0f) -dy * 0.92f else 0f
                            change.consume()
                        }
                    }
                    if (dragging) {
                        val velocityY = tracker.calculateVelocity().y
                        val height = size.height.toFloat()
                        if (total < 0f) {
                            val shouldOpen = -total > height * 0.15f || velocityY < -1100f
                            val startPull = pullPx.floatValue
                            scope.launch {
                                if (shouldOpen) {
                                    animate(
                                        initialValue = startPull,
                                        targetValue = height * 0.5f,
                                        animationSpec = tween(190, easing = SnappyEaseOut)
                                    ) { value, _ -> pullPx.floatValue = value }
                                    onSwipeUpSearch()
                                    delay(320L)
                                    pullPx.floatValue = 0f
                                } else {
                                    animate(
                                        initialValue = startPull,
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.55f, stiffness = 360f)
                                    ) { value, _ -> pullPx.floatValue = value }
                                }
                            }
                        } else {
                            pullPx.floatValue = 0f
                            if (total > 70f && settings.swipeDownForNotifications) {
                                onSwipeDownNotifications()
                            }
                        }
                    }
                }
            }
    ) {
        // Layer 0: kinetic grid, drifting with tilt
        if (settings.showInteractiveGrid) {
            InteractiveGridCanvas(
                touchProvider = { activeTouchPoint },
                holdProvider = { holdProgress.floatValue },
                tilt = tilt
            )
        }

        // Layer 1: content that follows the pull gesture frame-by-frame
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = pullPx.floatValue
                    val norm = (p / (size.height * 0.5f)).coerceIn(0f, 1f)
                    translationY = -p * 0.55f
                    alpha = 1f - 0.85f * norm
                    val s = 1f - 0.06f * norm
                    scaleX = s
                    scaleY = s
                }
                .padding(
                    start = 28.dp,
                    end = if (settings.showHomeEdgeSidebar) 34.dp else 28.dp,
                    top = 24.dp,
                    bottom = 24.dp
                ),
            horizontalAlignment = horizontalAlignment
        ) {
            // Header
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
                        innerPaddingHorizontal = 16.dp,
                        innerPaddingVertical = 9.dp,
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

                AnimatedVisibility(
                    visible = settings.showMusicCapsule && musicInfo != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val m = musicInfo
                    if (m != null) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            DoubleBezelSquircle(
                                outerRadius = 20.dp,
                                innerPaddingHorizontal = 14.dp,
                                innerPaddingVertical = 8.dp,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    MediaSessionHelper.playPause(context)
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
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
                }

                if (settings.showTelemetry) {
                    Spacer(modifier = Modifier.height(12.dp))
                    DoubleBezelSquircle(
                        outerRadius = 20.dp,
                        innerPaddingHorizontal = 14.dp,
                        innerPaddingVertical = 8.dp,
                        onClick = onTelemetryPillClick
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                .clickable(onClick = onToggleFocusSound)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
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
                    Spacer(modifier = Modifier.height(6.dp))
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

            // Middle: floating Google-shaped icons (or the classic text list)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (pinnedApps.isEmpty()) {
                    DoubleBezelSquircle(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                        outerRadius = 24.dp,
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
                } else if (settings.floatingHomeIcons) {
                    FloatingHomeIcons(
                        apps = pinnedApps,
                        settings = settings,
                        tilt = tilt,
                        holdingPackage = holdingPackage,
                        onHoldChange = onHoldChange,
                        onLaunch = launchWithBloom,
                        onMoreOptions = onAppLongClick
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = horizontalAlignment
                    ) {
                        pinnedApps.take(6).forEach { app ->
                            HoldToOpenHost(
                                app = app,
                                settings = settings,
                                dimmed = holdingPackage != null && holdingPackage != app.packageName,
                                modifier = Modifier.fillMaxWidth(),
                                onHoldChange = onHoldChange,
                                onLaunch = launchWithBloom,
                                onMoreOptions = onAppLongClick
                            ) { progress ->
                                val rowShape = remember { SquircleShape(16.dp) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(rowShape)
                                        .drawWithContent {
                                            drawContent()
                                            val p = progress()
                                            if (p > 0.005f) {
                                                clipRect(
                                                    left = 0f,
                                                    top = 0f,
                                                    right = size.width * p,
                                                    bottom = size.height
                                                ) {
                                                    drawRect(palette.primaryText.copy(alpha = 0.12f))
                                                }
                                            }
                                        }
                                        .graphicsLayer {
                                            val s = 1f - 0.025f * progress()
                                            scaleX = s
                                            scaleY = s
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dock
            if (settings.showIconDock) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DoubleBezelSquircle(
                        outerRadius = 24.dp,
                        innerPaddingHorizontal = 18.dp,
                        innerPaddingVertical = 10.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val dockItems = listOf(
                                "↑ search" to onSwipeUpSearch,
                                "burner" to onOpenBurnerNote,
                                "clips" to onOpenQuantumClip,
                                "settings" to onLongPressEmptySpace
                            )
                            dockItems.forEachIndexed { index, (label, action) ->
                                if (index > 0) {
                                    Text(
                                        text = "·",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.mutedText
                                    )
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryText,
                                    modifier = Modifier
                                        .jellyPress(0.95f)
                                        .clickable(onClick = action)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right-edge wave sidebar
        if (settings.showHomeEdgeSidebar) {
            WaveSidebarScrubber(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 84.dp, bottom = 84.dp, end = 2.dp),
                compactIdleMode = true,
                onLetterScrubbed = { },
                onScrubReleased = { letter ->
                    if (letter != null) onHomeEdgeLetterSelected(letter)
                }
            )
        }

        // Magic overlays: settings-hold halo + launch bloom
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = settingsHold.floatValue
            if (p > 0.01f) {
                val radius = 70.dp.toPx() + 110.dp.toPx() * p
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentDot.copy(alpha = 0.5f * p),
                            palette.primaryText.copy(alpha = 0.16f * p),
                            Color.Transparent
                        ),
                        center = settingsHoldPoint,
                        radius = radius
                    ),
                    radius = radius,
                    center = settingsHoldPoint
                )
            }
            val b = bloom.value
            if (b > 0.005f && b < 0.999f) {
                val radius = (size.maxDimension * 1.3f * b).coerceAtLeast(1f)
                val fade = 1f - b
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentDot.copy(alpha = 0.55f * fade),
                            palette.primaryText.copy(alpha = 0.14f * fade),
                            Color.Transparent
                        ),
                        center = bloomOrigin,
                        radius = radius
                    ),
                    radius = radius,
                    center = bloomOrigin
                )
            }
        }
    }
}
