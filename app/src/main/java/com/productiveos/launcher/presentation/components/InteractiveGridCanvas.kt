package com.productiveos.launcher.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Layer 0: kinetic dot / crosshair matrix.
 *
 * All inputs are providers read per-frame (never during composition), so touching the screen or
 * holding an app redraws only this canvas and never recomposes the screen above it.
 * [tilt] adds a gentle parallax so the lattice drifts with the phone's orientation.
 */
@Composable
fun InteractiveGridCanvas(
    touchProvider: () -> Offset?,
    holdProvider: () -> Float = { 0f },
    tilt: State<Offset>? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalProductiveOSPalette.current
    val infinite = rememberInfiniteTransition(label = "GridAmbientWave")
    val ambientPhase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AmbientPhase"
    )

    val energy = remember { mutableFloatStateOf(0f) }
    val touchX = remember { mutableFloatStateOf(0f) }
    val touchY = remember { mutableFloatStateOf(0f) }
    val hold = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos {
                val t = touchProvider()
                val target = if (t != null) 1f else 0f
                if (t != null) {
                    if (energy.floatValue < 0.04f) {
                        touchX.floatValue = t.x
                        touchY.floatValue = t.y
                    } else {
                        touchX.floatValue += (t.x - touchX.floatValue) * 0.38f
                        touchY.floatValue += (t.y - touchY.floatValue) * 0.38f
                    }
                }
                energy.floatValue += (target - energy.floatValue) * 0.16f
                if (abs(energy.floatValue - target) < 0.004f) energy.floatValue = target
                val h = holdProvider()
                hold.floatValue += (h - hold.floatValue) * 0.3f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = 30.dp.toPx()
        val baseDotRadius = 1.25.dp.toPx()
        val wellRadiusPx = 125.dp.toPx()
        val maxWarpPx = 11.dp.toPx()

        val cols = (size.width / spacingPx).toInt() + 2
        val rows = (size.height / spacingPx).toInt() + 2

        val tiltOffset = tilt?.value ?: Offset.Zero
        val parallaxX = tiltOffset.x * 7.dp.toPx()
        val parallaxY = (tiltOffset.y - 0.9f) * 5.dp.toPx()

        val offsetX = (size.width - (cols - 1) * spacingPx) / 2f + parallaxX
        val offsetY = (size.height - (rows - 1) * spacingPx) / 2f + parallaxY

        val touchEnergy = energy.floatValue
        val holdProgress = hold.floatValue
        val tX = touchX.floatValue
        val tY = touchY.floatValue
        val phase = ambientPhase.value

        val holdWaveRadius = holdProgress * size.maxDimension * 0.65f
        val holdBandWidth = 65.dp.toPx()

        for (r in 0 until rows) {
            val baseY = offsetY + r * spacingPx
            for (c in 0 until cols) {
                val baseX = offsetX + c * spacingPx

                val wave = sin(phase + (c * 0.35f) + (r * 0.25f))
                var drawX = baseX
                var drawY = baseY
                var alpha = (0.11f + 0.04f * wave).coerceIn(0.05f, 0.25f)
                var dotRadius = baseDotRadius

                if (touchEnergy > 0.01f || holdProgress > 0.01f) {
                    val dx = baseX - tX
                    val dy = baseY - tY
                    val dist = hypot(dx, dy).coerceAtLeast(1f)

                    if (dist < wellRadiusPx * 1.6f && touchEnergy > 0.01f) {
                        val norm = dist / wellRadiusPx
                        val gaussian = exp(-norm * norm * 1.4f) * touchEnergy
                        val push = maxWarpPx * gaussian
                        drawX += (dx / dist) * push
                        drawY += (dy / dist) * push
                        alpha += 0.28f * gaussian
                        dotRadius += baseDotRadius * 0.75f * gaussian
                    }

                    if (holdProgress > 0.01f) {
                        val ringDist = abs(dist - holdWaveRadius)
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
                    drawCircle(color = finalColor, radius = dotRadius, center = Offset(drawX, drawY))
                }
            }
        }

        if (holdProgress > 0.02f) {
            drawCircle(
                color = palette.primaryText.copy(alpha = (1f - holdProgress) * 0.22f),
                radius = holdWaveRadius,
                center = Offset(tX, tY),
                style = Stroke(width = 1.25.dp.toPx())
            )
        }
    }
}
