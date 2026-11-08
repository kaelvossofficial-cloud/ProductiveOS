package com.productiveos.launcher.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.preferences.VolumeBarStyleOption
import com.productiveos.launcher.presentation.theme.LiquidGlassCard
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.jellyPress
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Floating OS Volume / Voice Bar Overlay triggered by hardware volume keys or quick action.
 * Renders the user's chosen [VolumeBarStyleOption] matched to the active Liquid Glass theme palette.
 */
@Composable
fun VolumeBarOverlay(
    visible: Boolean,
    volumeFraction: Float,
    isMuted: Boolean,
    style: VolumeBarStyleOption,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onCycleStyle: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = fadeOut(tween(200)) + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            ThemedVolumeVoiceBar(
                volumeFraction = if (isMuted) 0f else volumeFraction,
                isMuted = isMuted,
                style = style,
                onVolumeChange = onVolumeChange,
                onToggleMute = onToggleMute,
                onCycleStyle = onCycleStyle
            )
        }
    }
}

/**
 * Interactive themed Volume / Voice Bar supporting 4 distinct skins:
 * 1. LIQUID_GLASS_PILL
 * 2. EQUALIZER_WAVE
 * 3. NEON_ORBIT_ARC
 * 4. MINIMAL_EDGE_BLADE
 */
@Composable
fun ThemedVolumeVoiceBar(
    volumeFraction: Float,
    isMuted: Boolean,
    style: VolumeBarStyleOption,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onCycleStyle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val coercedVolume = volumeFraction.coerceIn(0f, 1f)

    val animatedVol by animateFloatAsState(
        targetValue = coercedVolume,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "VolumeSmooth"
    )

    val infinite = rememberInfiniteTransition(label = "VoiceWavePulse")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    var trackWidthPx by remember { mutableFloatStateOf(1f) }

    val pctLabel = "${(coercedVolume * 100).roundToInt()}%"
    val dbLabel = if (coercedVolume <= 0.01f || isMuted) {
        "-∞ dB"
    } else {
        val db = ((coercedVolume - 1f) * 42f).roundToInt()
        "${db} dB"
    }

    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = when (style) {
            VolumeBarStyleOption.LIQUID_GLASS_PILL -> 28.dp
            VolumeBarStyleOption.EQUALIZER_WAVE -> 24.dp
            VolumeBarStyleOption.NEON_ORBIT_ARC -> 22.dp
            VolumeBarStyleOption.MINIMAL_EDGE_BLADE -> 18.dp
        },
        elevated = true,
        accentHighlight = true,
        paddingHorizontal = 16.dp,
        paddingVertical = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Header: Acoustic Icon + Style Name + Live dB / % Readout + Skin Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mute / Speaker Orb
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .jellyPress(0.92f)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (isMuted || coercedVolume == 0f) {
                                        listOf(palette.surface, palette.surfaceElevated)
                                    } else {
                                        listOf(
                                            palette.accentPrimary.copy(alpha = 0.28f),
                                            palette.accentSecondary.copy(alpha = 0.20f)
                                        )
                                    }
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = if (isMuted || coercedVolume == 0f) palette.borderSubtle else palette.accentPrimary,
                                shape = CircleShape
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleMute()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val c = if (isMuted || coercedVolume == 0f) palette.mutedText else palette.primaryText
                            // Speaker cone
                            drawRoundRect(
                                color = c,
                                topLeft = Offset(size.width * 0.12f, size.height * 0.32f),
                                size = Size(size.width * 0.28f, size.height * 0.36f),
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                            if (isMuted || coercedVolume == 0f) {
                                drawLine(
                                    color = palette.accentPrimary,
                                    start = Offset(size.width * 0.55f, size.height * 0.28f),
                                    end = Offset(size.width * 0.88f, size.height * 0.72f),
                                    strokeWidth = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            } else {
                                drawArc(
                                    color = palette.accentPrimary,
                                    startAngle = -45f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    topLeft = Offset(size.width * 0.32f, size.height * 0.20f),
                                    size = Size(size.width * 0.45f, size.height * 0.60f),
                                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                if (coercedVolume > 0.45f) {
                                    drawArc(
                                        color = palette.accentSecondary,
                                        startAngle = -50f,
                                        sweepAngle = 100f,
                                        useCenter = false,
                                        topLeft = Offset(size.width * 0.44f, size.height * 0.08f),
                                        size = Size(size.width * 0.52f, size.height * 0.84f),
                                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = style.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = palette.primaryText
                        )
                        Text(
                            text = if (isMuted) "Muted · Tap orb to unmute" else "Voice & Media Acoustic Stream · $dbLabel",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                            color = palette.secondaryText
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onCycleStyle != null) {
                        Box(
                            modifier = Modifier
                                .jellyPress(0.94f)
                                .clip(RoundedCornerShape(50))
                                .background(palette.surface.copy(alpha = 0.75f))
                                .border(1.dp, palette.glassBorderTop, RoundedCornerShape(50))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCycleStyle()
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "skin ↻",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                                color = palette.accentPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.accentPrimary.copy(alpha = 0.18f))
                            .border(1.dp, palette.accentPrimary.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = pctLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.primaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Touch & Drag Track rendering the 4 distinct styles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        when (style) {
                            VolumeBarStyleOption.LIQUID_GLASS_PILL -> 34.dp
                            VolumeBarStyleOption.EQUALIZER_WAVE -> 42.dp
                            VolumeBarStyleOption.NEON_ORBIT_ARC -> 36.dp
                            VolumeBarStyleOption.MINIMAL_EDGE_BLADE -> 28.dp
                        }
                    )
                    .onSizeChanged { trackWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val newVol = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onVolumeChange(newVol)
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val newVol = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                            onVolumeChange(newVol)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                when (style) {
                    VolumeBarStyleOption.LIQUID_GLASS_PILL -> {
                        LiquidGlassPillTrack(
                            animatedVol = animatedVol,
                            wavePhase = wavePhase
                        )
                    }
                    VolumeBarStyleOption.EQUALIZER_WAVE -> {
                        EqualizerWaveTrack(
                            animatedVol = animatedVol,
                            wavePhase = wavePhase
                        )
                    }
                    VolumeBarStyleOption.NEON_ORBIT_ARC -> {
                        NeonTactileMatrixTrack(
                            animatedVol = animatedVol,
                            wavePhase = wavePhase
                        )
                    }
                    VolumeBarStyleOption.MINIMAL_EDGE_BLADE -> {
                        MinimalEdgeBladeTrack(
                            animatedVol = animatedVol
                        )
                    }
                }
            }
        }
    }
}

/**
 * STYLE 1: Liquid Glass Capsule
 * Translucent frosted capsule with glowing liquid gradient fill, specular top highlight, and wave meniscus.
 */
@Composable
private fun LiquidGlassPillTrack(
    animatedVol: Float,
    wavePhase: Float
) {
    val palette = LocalProductiveOSPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(50))
            .background(palette.background.copy(alpha = 0.55f))
            .border(1.dp, palette.glassBorderTop, RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fillWidth = (size.width * animatedVol).coerceAtLeast(size.height)
            if (animatedVol > 0.01f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            palette.accentPrimary.copy(alpha = 0.85f),
                            palette.accentSecondary.copy(alpha = 0.95f)
                        ),
                        startX = 0f,
                        endX = fillWidth
                    ),
                    size = Size(fillWidth.coerceAtMost(size.width), size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )

                // Specular top glass reflection strip inside the liquid fill
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.28f),
                    topLeft = Offset(8.dp.toPx(), 3.dp.toPx()),
                    size = Size((fillWidth - 16.dp.toPx()).coerceAtLeast(4f), 4.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )

                // Glowing meniscus orb at the leading edge
                val orbX = (fillWidth - size.height / 2f).coerceIn(size.height / 2f, size.width - size.height / 2f)
                val pulse = 1f + 0.08f * sin(wavePhase)
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = (size.height * 0.26f) * pulse,
                    center = Offset(orbX, size.height / 2f)
                )
            }
        }
    }
}

/**
 * STYLE 2: Voice Equalizer Wave
 * 22 acoustic frequency bars that animate with live voice-like harmonic waves and theme gradient coloring.
 */
@Composable
private fun EqualizerWaveTrack(
    animatedVol: Float,
    wavePhase: Float
) {
    val palette = LocalProductiveOSPalette.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val barCount = 24
        val gap = 4.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(3f)
        val activeBars = (animatedVol * barCount).roundToInt()

        for (i in 0 until barCount) {
            val isLit = i < activeBars && animatedVol > 0.01f
            val normIdx = i.toFloat() / barCount.toFloat()
            // Voice harmonic envelope (taller in mid-frequencies, dancing when active)
            val envelope = 0.35f + 0.65f * sin(normIdx * PI).toFloat()
            val dynamicWave = if (isLit) {
                0.72f + 0.28f * sin(wavePhase * 2f + i * 0.65f)
            } else {
                0.28f
            }
            val heightFrac = (envelope * dynamicWave).coerceIn(0.18f, 1f)
            val barH = size.height * heightFrac
            val x = i * (barWidth + gap)
            val y = (size.height - barH) / 2f

            val barBrush = if (isLit) {
                Brush.verticalGradient(
                    colors = listOf(
                        palette.accentPrimary,
                        palette.accentSecondary
                    ),
                    startY = y,
                    endY = y + barH
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        palette.borderSubtle.copy(alpha = 0.55f),
                        palette.borderSubtle.copy(alpha = 0.30f)
                    )
                )
            }

            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * STYLE 3: Neon Tactile Matrix
 * 14 segmented squircle power cells with glowing inner cores and specular borders.
 */
@Composable
private fun NeonTactileMatrixTrack(
    animatedVol: Float,
    wavePhase: Float
) {
    val palette = LocalProductiveOSPalette.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellCount = 14
        val gap = 5.dp.toPx()
        val cellWidth = ((size.width - gap * (cellCount - 1)) / cellCount).coerceAtLeast(6f)
        val cellHeight = size.height * 0.82f
        val topY = (size.height - cellHeight) / 2f
        val exactLit = animatedVol * cellCount

        for (i in 0 until cellCount) {
            val x = i * (cellWidth + gap)
            val cellFill = (exactLit - i).coerceIn(0f, 1f)
            val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

            // Base dark glass cell
            drawRoundRect(
                color = palette.background.copy(alpha = 0.55f),
                topLeft = Offset(x, topY),
                size = Size(cellWidth, cellHeight),
                cornerRadius = corner
            )

            if (cellFill > 0.02f) {
                val color = if (i > cellCount * 0.75f) palette.accentSecondary else palette.accentPrimary
                val pulseAlpha = (0.75f + 0.20f * sin(wavePhase + i * 0.3f)).coerceIn(0.6f, 1f)
                drawRoundRect(
                    color = color.copy(alpha = cellFill * pulseAlpha),
                    topLeft = Offset(x + 1.5.dp.toPx(), topY + 1.5.dp.toPx()),
                    size = Size(cellWidth - 3.dp.toPx(), cellHeight - 3.dp.toPx()),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }

            // Cell rim border
            drawRoundRect(
                color = if (cellFill > 0.5f) palette.accentPrimary else palette.borderSubtle.copy(alpha = 0.6f),
                topLeft = Offset(x, topY),
                size = Size(cellWidth, cellHeight),
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

/**
 * STYLE 4: Minimal Edge Blade
 * Ultra-clean precision architectural line with harmonic tick marks and a floating luminescence orb.
 */
@Composable
private fun MinimalEdgeBladeTrack(
    animatedVol: Float
) {
    val palette = LocalProductiveOSPalette.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cy = size.height / 2f
        val trackStart = 10.dp.toPx()
        val trackEnd = size.width - 10.dp.toPx()
        val trackLen = (trackEnd - trackStart).coerceAtLeast(1f)
        val thumbX = trackStart + trackLen * animatedVol

        // Subtle tick marks along the blade
        val ticks = 20
        for (i in 0..ticks) {
            val tx = trackStart + (trackLen * i / ticks)
            val isMajor = i % 5 == 0
            val tickH = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
            val passed = tx <= thumbX
            drawLine(
                color = if (passed) palette.accentPrimary.copy(alpha = 0.65f) else palette.borderSubtle,
                start = Offset(tx, cy - tickH / 2f),
                end = Offset(tx, cy + tickH / 2f),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Background rail
        drawLine(
            color = palette.borderSubtle.copy(alpha = 0.6f),
            start = Offset(trackStart, cy),
            end = Offset(trackEnd, cy),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Active glowing blade line
        if (animatedVol > 0.005f) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(palette.accentPrimary, palette.accentSecondary),
                    startX = trackStart,
                    endX = thumbX.coerceAtLeast(trackStart + 1f)
                ),
                start = Offset(trackStart, cy),
                end = Offset(thumbX, cy),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Outer halo + glowing thumb orb
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.accentPrimary.copy(alpha = 0.50f),
                    Color.Transparent
                ),
                center = Offset(thumbX, cy),
                radius = 14.dp.toPx()
            ),
            radius = 14.dp.toPx(),
            center = Offset(thumbX, cy)
        )
        drawCircle(
            color = palette.primaryText,
            radius = 6.5.dp.toPx(),
            center = Offset(thumbX, cy)
        )
        drawCircle(
            color = palette.accentPrimary,
            radius = 3.5.dp.toPx(),
            center = Offset(thumbX, cy)
        )
    }
}

/**
 * Compact visual preview card for selecting one of the 4 Volume/Voice Bar skins in Settings.
 */
@Composable
fun VolumeSkinSelectorCard(
    option: VolumeBarStyleOption,
    selected: Boolean,
    previewVolume: Float,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalProductiveOSPalette.current
    val infinite = rememberInfiniteTransition(label = "SkinCardWave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SkinPhase"
    )

    LiquidGlassCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        elevated = selected,
        accentHighlight = selected,
        paddingHorizontal = 14.dp,
        paddingVertical = 14.dp,
        onClick = onSelect
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = palette.primaryText
                )
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) palette.accentPrimary else Color.Transparent
                        )
                        .border(
                            width = 1.2.dp,
                            color = if (selected) palette.accentPrimary else palette.borderSubtle,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(palette.background)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = option.subtitle,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                color = palette.secondaryText,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Miniature Live Skin Graphic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                contentAlignment = Alignment.Center
            ) {
                when (option) {
                    VolumeBarStyleOption.LIQUID_GLASS_PILL -> LiquidGlassPillTrack(previewVolume, phase)
                    VolumeBarStyleOption.EQUALIZER_WAVE -> EqualizerWaveTrack(previewVolume, phase)
                    VolumeBarStyleOption.NEON_ORBIT_ARC -> NeonTactileMatrixTrack(previewVolume, phase)
                    VolumeBarStyleOption.MINIMAL_EDGE_BLADE -> MinimalEdgeBladeTrack(previewVolume)
                }
            }
        }
    }
}
