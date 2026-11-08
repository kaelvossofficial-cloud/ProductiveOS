package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.text.font.FontWeight
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
import com.productiveos.launcher.presentation.theme.LiquidGlassCard
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
    onHomeEdgeLetterSelected: (String) -> Unit = { onSwipeUpSearch() },
    onOpenVolumeBar: () -> Unit = {}
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

    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }
    val holdProgress = remember { mutableFloatStateOf(0f) }
    var holdingPackage by remember { mutableStateOf<String?>(null) }
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
            bloom.animateTo(1f, tween(420, easing = SnappyEaseOut))
        }
        scope.launch {
            delay(80L)
            onAppClick(app)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .onGloballyPositioned { containerOrigin = it.positionInWindow() }
            // Observe touches without consuming them (feeds the kinetic dot grid)
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
            // Clean Double-Tap & Long-Press on empty space (no 16ms busy-loop, zero jank)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapEmptySpace() },
                    onLongPress = {
                        if (holdingPackage == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressEmptySpace()
                        }
                    }
                )
            }
            // Clean Vertical Swipe detector (never shrinks or distorts home layout)
            .pointerInput(settings.swipeDownForNotifications) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    val slop = viewConfiguration.touchSlop
                    var dragging = false
                    var totalDy = 0f
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val dy = change.position.y - down.position.y
                        val dx = change.position.x - down.position.x
                        if (!dragging) {
                            if (abs(dx) > slop * 1.4f && abs(dx) > abs(dy)) break
                            if (abs(dy) > slop * 1.3f && abs(dy) > abs(dx) * 1.25f && holdingPackage == null) {
                                dragging = true
                            }
                        }
                        if (dragging) {
                            totalDy = dy
                            change.consume()
                        }
                    }
                    if (dragging) {
                        val velocityY = tracker.calculateVelocity().y
                        if (totalDy < -85f || velocityY < -950f) {
                            onSwipeUpSearch()
                        } else if ((totalDy > 85f || velocityY > 950f) && settings.swipeDownForNotifications) {
                            onSwipeDownNotifications()
                        }
                    }
                }
            }
    ) {
        // Layer 0: Ambient top radial liquid glow + kinetic grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accentPrimary.copy(alpha = if (palette.isLight) 0.10f else 0.14f),
                        palette.accentSecondary.copy(alpha = if (palette.isLight) 0.04f else 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.14f),
                    radius = size.minDimension * 0.72f
                ),
                radius = size.minDimension * 0.72f,
                center = Offset(size.width * 0.5f, size.height * 0.14f)
            )
        }

        if (settings.showInteractiveGrid) {
            InteractiveGridCanvas(
                touchProvider = { activeTouchPoint },
                holdProvider = { holdProgress.floatValue },
                tilt = tilt
            )
        }

        // Layer 1: Stable full-screen content (never shrinks or breaks)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 26.dp,
                    end = if (settings.showHomeEdgeSidebar) 34.dp else 26.dp,
                    top = 20.dp,
                    bottom = 20.dp
                ),
            horizontalAlignment = horizontalAlignment
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                if (!isDefaultLauncher) {
                    LiquidGlassCard(
                        modifier = Modifier.padding(bottom = 16.dp),
                        cornerRadius = 20.dp,
                        elevated = true,
                        accentHighlight = true,
                        paddingHorizontal = 16.dp,
                        paddingVertical = 10.dp,
                        onClick = onSetDefaultLauncherClick
                    ) {
                        Text(
                            text = "Set ProductiveOS as Default Home →",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
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
                    Spacer(modifier = Modifier.height(2.dp))
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
                                        color = palette.accentPrimary
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
                    Spacer(modifier = Modifier.height(10.dp))
                    DoubleBezelSquircle(
                        outerRadius = 20.dp,
                        innerPaddingHorizontal = 14.dp,
                        innerPaddingVertical = 8.dp,
                        onClick = onTelemetryPillClick
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentPrimary)
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
                                .background(palette.glassSurfaceElevated)
                                .clickable(onClick = onToggleFocusSound)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ılılı  focus anchor · ${activeFocusSound.label} · tap to stop",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentPrimary
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
                                color = palette.accentPrimary
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

            // Middle: Responsive Floating Icons or Liquid Glass Text Rows
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (pinnedApps.isEmpty()) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                        cornerRadius = 24.dp,
                        elevated = true,
                        accentHighlight = true,
                        paddingHorizontal = 20.dp,
                        paddingVertical = 20.dp,
                        onClick = onSwipeUpSearch
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "productiveos · liquid glass",
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                                    drawRect(palette.accentPrimary.copy(alpha = 0.18f))
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
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accentPrimary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Liquid Glass Dock (with quick Voice Bar summon)
            if (settings.showIconDock) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DoubleBezelSquircle(
                        outerRadius = 24.dp,
                        innerPaddingHorizontal = 16.dp,
                        innerPaddingVertical = 10.dp,
                        elevated = true
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val dockItems = listOf(
                                "↑ apps" to onSwipeUpSearch,
                                "ılı voice" to onOpenVolumeBar,
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
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (label == "ılı voice") FontWeight.Medium else FontWeight.Normal
                                    ),
                                    color = if (label == "ılı voice") palette.accentPrimary else palette.secondaryText,
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

        // Launch bloom overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val b = bloom.value
            if (b > 0.005f && b < 0.999f) {
                val radius = (size.maxDimension * 1.2f * b).coerceAtLeast(1f)
                val fade = 1f - b
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.accentPrimary.copy(alpha = 0.50f * fade),
                            palette.accentSecondary.copy(alpha = 0.18f * fade),
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
