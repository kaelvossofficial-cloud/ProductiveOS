package com.productiveos.launcher.presentation.theme

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Jelly-like press feedback. Observes presses on the Initial pass without consuming them,
 * so it composes with clickable / detectTapGestures on the same element.
 */
fun Modifier.jellyPress(pressedScale: Float = 0.9f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
        label = "jellyScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (pressed) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
        label = "jellyAlpha"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha.coerceIn(0f, 1f)
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                pressed = true
                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                pressed = false
            }
        }
}

/** Continuous-curvature rounded rectangle ("squircle"): corners are softer than a plain arc. */
class SquircleShape(private val cornerRadius: Dp = 26.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val r = with(density) { cornerRadius.toPx() }
        val p = (r * 1.3f).coerceAtMost(minOf(w, h) / 2f)
        val k = p * 0.28f
        val path = Path().apply {
            moveTo(p, 0f)
            lineTo(w - p, 0f)
            cubicTo(w - k, 0f, w, k, w, p)
            lineTo(w, h - p)
            cubicTo(w, h - k, w - k, h, w - p, h)
            lineTo(p, h)
            cubicTo(k, h, 0f, h - k, 0f, h - p)
            lineTo(0f, p)
            cubicTo(0f, k, k, 0f, p, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Organic multi-lobed squircle (default: 12 soft lobes) used for icon containers. */
class LobedSquircleShape(
    private val lobes: Int = 12,
    private val amplitude: Float = 0.045f,
    private val exponent: Float = 4f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val steps = 240
        val path = Path()
        for (i in 0..steps) {
            val t = (2.0 * PI * i / steps).toFloat()
            val c = cos(t)
            val s = sin(t)
            val ex = 2f / exponent
            val bx = sign(c) * abs(c).toDouble().pow(ex.toDouble()).toFloat()
            val by = sign(s) * abs(s).toDouble().pow(ex.toDouble()).toFloat()
            val wobble = 1f + amplitude * cos(lobes * t)
            val x = cx + bx * cx * wobble / (1f + amplitude)
            val y = cy + by * cy * wobble / (1f + amplitude)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private fun Drawable.toTintableBitmap(sizePx: Int) = runCatching {
    val layer: Drawable = if (this is AdaptiveIconDrawable) {
        val mono = if (Build.VERSION.SDK_INT >= 33) this.monochrome else null
        mono ?: this.foreground
    } else this
    layer.toBitmap(sizePx, sizePx)
}.getOrNull()

/**
 * App icon inside a lobed squircle container; the icon is flattened to a monochrome glyph
 * tinted with [tint] so it follows the active theme.
 */
@Composable
fun AppIconSquircle(
    packageName: String,
    containerSize: Dp,
    containerColor: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toTintableBitmap(216)
        }.getOrNull()
    }
    Box(
        modifier = modifier
            .size(containerSize)
            .clip(LobedSquircleShape())
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(containerSize * 0.95f)
            )
        }
    }
}

/**
 * Double-Bezel ("Doppelrand") nested hardware enclosure.
 * Simulates a machined outer bezel tray housing an inner glass/matte core.
 */
@Composable
fun DoubleBezelSquircle(
    modifier: Modifier = Modifier,
    outerRadius: Dp = 22.dp,
    bezelThickness: Dp = 2.dp,
    innerPaddingHorizontal: Dp = 16.dp,
    innerPaddingVertical: Dp = 12.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val outerShape = remember(outerRadius) { SquircleShape(outerRadius) }
    val innerRadius = (outerRadius - bezelThickness).coerceAtLeast(8.dp)
    val innerShape = remember(innerRadius) { SquircleShape(innerRadius) }

    Box(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .jellyPress(pressedScale = 0.97f)
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .clip(outerShape)
            .background(palette.borderSubtle.copy(alpha = 0.28f))
            .border(0.75.dp, palette.borderSubtle.copy(alpha = 0.75f), outerShape)
            .padding(bezelThickness)
    ) {
        Box(
            modifier = Modifier
                .clip(innerShape)
                .background(if (elevated) palette.surfaceElevated else palette.surface)
                .border(0.75.dp, palette.primaryText.copy(alpha = 0.12f), innerShape)
                .padding(horizontal = innerPaddingHorizontal, vertical = innerPaddingVertical)
        ) {
            content()
        }
    }
}
