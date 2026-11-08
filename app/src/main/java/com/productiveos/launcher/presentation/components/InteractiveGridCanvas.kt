package com.productiveos.launcher.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

/**
 * Interactive Background Dot/Crosshair Grid Canvas.
 * Performance-optimized for zero idle CPU/GPU drain:
 * - 48dp spacing
 * - Sleeps when no finger is touching/holding (`energy == 0f && hold == 0f`)
 * - Only runs frame-by-frame warp physics during active touch or decay
 */
@Composable
fun InteractiveGridCanvas(
    touchProvider: () -> Offset?,
    holdProvider: () -> Float = { 0f },
    tilt: State<Offset>? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalProductiveOSPalette.current

    val energy = remember { mutableFloatStateOf(0f) }
    val touchX = remember { mutableFloatStateOf(0f) }
    val touchY = remember { mutableFloatStateOf(0f) }
    val hold = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val t = touchProvider()
            val h = holdProvider()
            if (t == null && h <= 0.005f && energy.floatValue <= 0.005f && hold.floatValue <= 0.005f) {
                // Idle sleep: avoid 60/120fps busy loop when user is not interacting
                delay(48L)
                continue
            }
            withFrameNanos {
                val curTouch = touchProvider()
                val target = if (curTouch != null) 1f else 0f
                if (curTouch != null) {
                    if (energy.floatValue < 0.04f) {
                        touchX.floatValue = curTouch.x
                        touchY.floatValue = curTouch.y
                    } else {
                        touchX.floatValue += (curTouch.x - touchX.floatValue) * 0.45f
                        touchY.floatValue += (curTouch.y - touchY.floatValue) * 0.45f
                    }
                }
                energy.floatValue += (target - energy.floatValue) * 0.24f
                if (abs(energy.floatValue - target) < 0.008f) energy.floatValue = target
                val curHold = holdProvider()
                hold.floatValue += (curHold - hold.floatValue) * 0.35f
                if (abs(hold.floatValue - curHold) < 0.008f) hold.floatValue = curHold
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = 48.dp.toPx()
        val baseDotRadius = 1.25.dp.toPx()
        val wellRadiusPx = 110.dp.toPx()
        val maxWarpPx = 9.dp.toPx()

        val cols = (size.width / spacingPx).toInt() + 1
        val rows = (size.height / spacingPx).toInt() + 1

        val tiltOffset = tilt?.value ?: Offset.Zero
        val parallaxX = tiltOffset.x * 4.dp.toPx()
        val parallaxY = (tiltOffset.y - 0.9f) * 3.dp.toPx()

        val offsetX = (size.width - (cols - 1) * spacingPx) / 2f + parallaxX
        val offsetY = (size.height - (rows - 1) * spacingPx) / 2f + parallaxY

        val touchEnergy = energy.floatValue
        val holdProgress = hold.floatValue
        val tX = touchX.floatValue
        val tY = touchY.floatValue

        val holdWaveRadius = holdProgress * size.maxDimension * 0.65f
        val holdBandWidth = 60.dp.toPx()
        val hasTouchOrHold = touchEnergy > 0.01f || holdProgress > 0.01f
        val baseDotColor = palette.accentPrimary.copy(alpha = 0.12f)
        val baseCrossColor = palette.primaryText.copy(alpha = 0.16f)

        for (r in 0 until rows) {
            val baseY = offsetY + r * spacingPx
            for (c in 0 until cols) {
                val baseX = offsetX + c * spacingPx
                val isCrosshair = (r % 3 == 0) && (c % 3 == 0)

                if (!hasTouchOrHold) {
                    if (isCrosshair) {
                        val arm = baseDotRadius * 2.1f
                        drawLine(
                            color = baseCrossColor,
                            start = Offset(baseX - arm, baseY),
                            end = Offset(baseX + arm, baseY),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = baseCrossColor,
                            start = Offset(baseX, baseY - arm),
                            end = Offset(baseX, baseY + arm),
                            strokeWidth = 1.dp.toPx()
                        )
                    } else {
                        drawCircle(color = baseDotColor, radius = baseDotRadius, center = Offset(baseX, baseY))
                    }
                    continue
                }

                var drawX = baseX
                var drawY = baseY
                var alpha = 0.12f
                var dotRadius = baseDotRadius

                val dx = baseX - tX
                val dy = baseY - tY
                val dist = hypot(dx, dy).coerceAtLeast(1f)

                if (dist < wellRadiusPx * 1.5f && touchEnergy > 0.01f) {
                    val norm = dist / wellRadiusPx
                    val gaussian = exp(-norm * norm * 1.4f) * touchEnergy
                    val push = maxWarpPx * gaussian
                    drawX += (dx / dist) * push
                    drawY += (dy / dist) * push
                    alpha += 0.32f * gaussian
                    dotRadius += baseDotRadius * 0.7f * gaussian
                }

                if (holdProgress > 0.01f) {
                    val ringDist = abs(dist - holdWaveRadius)
                    if (ringDist < holdBandWidth) {
                        val ringFactor = (1f - (ringDist / holdBandWidth)) * holdProgress
                        alpha += 0.36f * ringFactor
                        dotRadius += baseDotRadius * 0.8f * ringFactor
                        val ripplePush = 5.dp.toPx() * ringFactor
                        drawX += (dx / dist) * ripplePush
                        drawY += (dy / dist) * ripplePush
                    }
                }

                val finalColor = palette.accentPrimary.copy(alpha = alpha.coerceIn(0.08f, 0.65f))
                if (isCrosshair) {
                    val arm = dotRadius * 2.1f
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

        if (holdProgress > 0.03f) {
            drawCircle(
                color = palette.accentPrimary.copy(alpha = (1f - holdProgress) * 0.28f),
                radius = holdWaveRadius,
                center = Offset(tX, tY),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }
    }
}
