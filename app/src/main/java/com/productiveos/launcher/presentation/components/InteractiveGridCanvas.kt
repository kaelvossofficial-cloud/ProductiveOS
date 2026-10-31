package com.productiveos.launcher.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Layer 0: Interactive Kinetic Dot/Crosshair Matrix Background Canvas.
 *
 * Features:
 * 1. Ambient breathing phase wave across the dot matrix.
 * 2. Magnetic Touch Well: Points near [touchPoint] warp radially and brighten smoothly.
 * 3. Hold-to-Open Resonance Ring: When holding an app ([holdProgress] > 0f), a concentric
 *    energy ring expands across the grid and pulses the surrounding lattice.
 */
@Composable
fun InteractiveGridCanvas(
    touchPoint: Offset?,
    holdProgress: Float = 0f,
    modifier: Modifier = Modifier
) {
    val palette = LocalProductiveOSPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "GridAmbientWave")

    val ambientPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AmbientPhase"
    )

    val touchEnergy by animateFloatAsState(
        targetValue = if (touchPoint != null) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessLow
        ),
        label = "TouchEnergy"
    )

    val rememberedTouchX by animateFloatAsState(
        targetValue = touchPoint?.x ?: 500f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "TouchX"
    )

    val rememberedTouchY by animateFloatAsState(
        targetValue = touchPoint?.y ?: 900f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "TouchY"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = 30.dp.toPx()
        val baseDotRadius = 1.25.dp.toPx()
        val wellRadiusPx = 125.dp.toPx()
        val maxWarpPx = 11.dp.toPx()

        val cols = (size.width / spacingPx).toInt() + 1
        val rows = (size.height / spacingPx).toInt() + 1

        val offsetX = (size.width - (cols - 1) * spacingPx) / 2f
        val offsetY = (size.height - (rows - 1) * spacingPx) / 2f

        val holdWaveRadius = holdProgress * size.maxDimension * 0.65f
        val holdBandWidth = 65.dp.toPx()

        for (r in 0 until rows) {
            val baseY = offsetY + r * spacingPx
            for (c in 0 until cols) {
                val baseX = offsetX + c * spacingPx

                // Subtle ambient diagonal breathing wave
                val wave = sin(ambientPhase + (c * 0.35f) + (r * 0.25f))
                var drawX = baseX
                var drawY = baseY
                var alpha = (0.11f + 0.04f * wave).coerceIn(0.05f, 0.25f)
                var dotRadius = baseDotRadius

                if (touchEnergy > 0.01f || holdProgress > 0.01f) {
                    val dx = baseX - rememberedTouchX
                    val dy = baseY - rememberedTouchY
                    val dist = hypot(dx, dy).coerceAtLeast(1f)

                    // 1. Magnetic Touch Well Distortion
                    if (dist < wellRadiusPx * 1.6f && touchEnergy > 0.01f) {
                        val norm = dist / wellRadiusPx
                        val gaussian = exp(-norm * norm * 1.4f) * touchEnergy
                        val push = maxWarpPx * gaussian
                        drawX += (dx / dist) * push
                        drawY += (dy / dist) * push
                        alpha += 0.28f * gaussian
                        dotRadius += baseDotRadius * 0.75f * gaussian
                    }

                    // 2. Hold-to-Open Expanding Resonance Wave
                    if (holdProgress > 0.01f) {
                        val ringDist = kotlin.math.abs(dist - holdWaveRadius)
                        if (ringDist < holdBandWidth) {
                            val ringFactor = (1f - (ringDist / holdBandWidth)) * holdProgress
                            alpha += 0.38f * ringFactor
                            dotRadius += baseDotRadius * 0.9f * ringFactor
                            val ripplePush = 6.dp.toPx() * ringFactor
                            drawX += (dx / dist) * ripplePush
                            drawY += (dy / dist) * ripplePush
                        }
                    }
                }

                // Every 4th intersection renders a subtle precision crosshair (+) instead of a dot
                val isCrosshair = (r % 4 == 0) && (c % 4 == 0)
                val finalColor = palette.primaryText.copy(alpha = alpha.coerceIn(0.04f, 0.65f))

                if (isCrosshair) {
                    val arm = dotRadius * 2.4f
                    drawLine(
                        color = finalColor,
                        start = Offset(drawX - arm, drawY),
                        end = Offset(drawX + arm, drawY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = finalColor,
                        start = Offset(drawX, drawY - arm),
                        end = Offset(drawX, drawY + arm),
                        strokeWidth = 1.dp.toPx()
                    )
                } else {
                    drawCircle(
                        color = finalColor,
                        radius = dotRadius,
                        center = Offset(drawX, drawY)
                    )
                }
            }
        }

        // Draw faint expanding resonance ring during intentional hold pause
        if (holdProgress > 0.02f) {
            drawCircle(
                color = palette.primaryText.copy(alpha = (1f - holdProgress) * 0.22f),
                radius = holdWaveRadius,
                center = Offset(rememberedTouchX, rememberedTouchY),
                style = Stroke(width = 1.25.dp.toPx())
            )
        }
    }
}

