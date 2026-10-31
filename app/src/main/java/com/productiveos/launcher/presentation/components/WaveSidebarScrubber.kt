package com.productiveos.launcher.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.SquircleShape
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Gaussian Sine-Wave Right-Edge Sidebar Scrubber.
 *
 * When the user scrubs vertically along the right edge:
 * - Each letter in A..Z + # displaces leftward in a smooth Gaussian bell curve
 *   centered on the finger's Y coordinate.
 * - Letters near the finger scale up dynamically and brighten.
 * - A Double-Bezel squircle preview badge projects to the left of the wave crest.
 */
@Composable
fun WaveSidebarScrubber(
    modifier: Modifier = Modifier,
    compactIdleMode: Boolean = false,
    onLetterScrubbed: (String) -> Unit,
    onScrubReleased: (String?) -> Unit = {}
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val alphabet = remember { ('a'..'z').map { it.toString() } + "#" }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var fingerY by remember { mutableFloatStateOf(0f) }
    var railHeightPx by remember { mutableFloatStateOf(1f) }

    val isScrubbing = activeLetter != null
    val waveAmplitudeFraction by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "WaveAmplitude"
    )

    val maxDisplacementPx = with(density) { 46.dp.toPx() }
    val sigmaPx = with(density) { 52.dp.toPx() }

    Box(
        modifier = modifier
            .width(32.dp)
            .fillMaxHeight()
            .onGloballyPositioned { coords ->
                railHeightPx = coords.size.height.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        fingerY = offset.y.coerceIn(0f, railHeightPx)
                        val frac = (fingerY / railHeightPx).coerceIn(0f, 0.999f)
                        val idx = (frac * alphabet.size).toInt()
                        val letter = alphabet[idx]
                        activeLetter = letter
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onLetterScrubbed(letter)
                    },
                    onDragEnd = {
                        val released = activeLetter
                        activeLetter = null
                        onScrubReleased(released)
                    },
                    onDragCancel = {
                        activeLetter = null
                        onScrubReleased(null)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        fingerY = change.position.y.coerceIn(0f, railHeightPx)
                        val frac = (fingerY / railHeightPx).coerceIn(0f, 0.999f)
                        val idx = (frac * alphabet.size).toInt()
                        val letter = alphabet[idx]
                        if (activeLetter != letter) {
                            activeLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLetterScrubbed(letter)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEachIndexed { index, letter ->
                val itemCenterY = ((index + 0.5f) / alphabet.size) * railHeightPx
                val dy = itemCenterY - fingerY
                val gaussian = if (waveAmplitudeFraction > 0.01f) {
                    exp(-(dy * dy) / (2f * sigmaPx * sigmaPx)) * waveAmplitudeFraction
                } else {
                    0f
                }

                val shiftX = -maxDisplacementPx * gaussian
                val scale = 1f + (0.85f * gaussian)
                val isCurrent = activeLetter == letter

                val baseAlpha = if (compactIdleMode && !isScrubbing) {
                    if (index % 3 == 0) 0.35f else 0.14f
                } else {
                    0.45f
                }
                val finalAlpha = (baseAlpha + 0.55f * gaussian).coerceIn(0.12f, 1f)

                Text(
                    text = if (compactIdleMode && !isScrubbing && index % 3 != 0) "·" else letter,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isCurrent || gaussian > 0.55f) {
                        palette.primaryText.copy(alpha = finalAlpha)
                    } else {
                        palette.mutedText.copy(alpha = finalAlpha)
                    },
                    modifier = Modifier.graphicsLayer {
                        translationX = shiftX
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
        }

        // Floating Double-Bezel Squircle Letter Crest Preview
        if (activeLetter != null) {
            val outerSquircle = remember { SquircleShape(18.dp) }
            val innerSquircle = remember { SquircleShape(16.dp) }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = (-84).dp.roundToPx(),
                            y = (fingerY - 27.dp.toPx()).roundToInt().coerceIn(0, (railHeightPx - 54.dp.toPx()).roundToInt().coerceAtLeast(0))
                        )
                    }
                    .size(54.dp)
                    .clip(outerSquircle)
                    .background(palette.borderSubtle.copy(alpha = 0.4f))
                    .border(1.dp, palette.primaryText.copy(alpha = 0.45f), outerSquircle)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(innerSquircle)
                        .background(palette.surfaceElevated)
                        .border(0.75.dp, palette.primaryText.copy(alpha = 0.2f), innerSquircle),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeLetter.orEmpty().uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontSize = 28.sp
                        ),
                        color = palette.primaryText
                    )
                }
            }
        }
    }
}
