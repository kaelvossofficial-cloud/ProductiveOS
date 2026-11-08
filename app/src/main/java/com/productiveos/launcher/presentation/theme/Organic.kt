package com.productiveos.launcher.presentation.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Continuous Superellipse Squircle Shape with cached Outline for 120fps rendering.
 */
class SquircleShape(
    private val cornerRadius: Dp = 24.dp,
    private val smoothing: Float = 0.62f
) : Shape {
    private var cachedW: Int = -1
    private var cachedH: Int = -1
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val wi = w.toInt()
        val hi = h.toInt()
        val existing = cachedOutline
        if (existing != null && wi == cachedW && hi == cachedH) {
            return existing
        }

        val rPx = with(density) { cornerRadius.toPx() }
        val r = min(rPx, min(w, h) / 2f)
        val c = r * (0.55228475f + smoothing * 0.14f)

        val path = Path().apply {
            moveTo(0f, r)
            cubicTo(0f, r - c, r - c, 0f, r, 0f)
            lineTo(w - r, 0f)
            cubicTo(w - r + c, 0f, w, r - c, w, r)
            lineTo(w, h - r)
            cubicTo(w, h - r + c, w - r + c, h, w - r, h)
            lineTo(r, h)
            cubicTo(r - c, h, 0f, h - r + c, 0f, h - r)
            close()
        }
        return Outline.Generic(path).also {
            cachedW = wi
            cachedH = hi
            cachedOutline = it
        }
    }
}

/**
 * Parametric Lobed Squircle ("Clover / Soft Scallop" silhouette) with cached Outline.
 */
class LobedSquircleShape(
    private val lobes: Int = 4,
    private val lobeDepth: Float = 0.065f,
    private val superellipsePower: Float = 3.2f
) : Shape {
    private var cachedW: Int = -1
    private var cachedH: Int = -1
    private var cachedOutline: Outline? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val wi = size.width.toInt()
        val hi = size.height.toInt()
        val existing = cachedOutline
        if (existing != null && wi == cachedW && hi == cachedH) {
            return existing
        }

        val cx = size.width / 2f
        val cy = size.height / 2f
        val rx = cx * (1f - lobeDepth)
        val ry = cy * (1f - lobeDepth)
        val steps = 120
        val path = Path()
        val exp = 2.0 / superellipsePower.toDouble()

        for (i in 0..steps) {
            val theta = (2.0 * PI * i.toDouble()) / steps.toDouble()
            val cosT = cos(theta)
            val sinT = sin(theta)
            val baseX = sign(cosT) * abs(cosT).pow(exp)
            val baseY = sign(sinT) * abs(sinT).pow(exp)
            val modulation = 1.0 + lobeDepth * cos(lobes * theta)
            val x = (cx + rx * baseX * modulation).toFloat()
            val y = (cy + ry * baseY * modulation).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path).also {
            cachedW = wi
            cachedH = hi
            cachedOutline = it
        }
    }
}

/**
 * Liquid Glass Double-Bezel ("Doppelrand") Squircle Container.
 * Upgraded with specular top-left liquid glass rim light and translucent multi-stop fill.
 */
@Composable
fun DoubleBezelSquircle(
    modifier: Modifier = Modifier,
    outerRadius: Dp = 24.dp,
    bezelThickness: Dp = 1.5.dp,
    innerPaddingHorizontal: Dp = 18.dp,
    innerPaddingVertical: Dp = 14.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val outerShape = remember(outerRadius) { SquircleShape(outerRadius) }
    val innerRadius = (outerRadius - bezelThickness).coerceAtLeast(8.dp)
    val innerShape = remember(innerRadius) { SquircleShape(innerRadius) }

    val outerBrush = remember(palette) {
        Brush.linearGradient(
            colors = listOf(
                palette.glassBorderTop,
                palette.borderSubtle.copy(alpha = 0.25f),
                palette.glassBorderBottom
            )
        )
    }

    val innerBrush = remember(palette, elevated) {
        if (elevated) {
            Brush.linearGradient(
                colors = listOf(
                    palette.glassSurfaceElevated,
                    palette.surface.copy(alpha = 0.88f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    palette.glassSurface,
                    palette.surface.copy(alpha = 0.76f)
                )
            )
        }
    }

    val innerRimBrush = remember(palette) {
        Brush.linearGradient(
            colors = listOf(
                palette.primaryText.copy(alpha = if (palette.isLight) 0.45f else 0.16f),
                palette.accentPrimary.copy(alpha = 0.12f)
            )
        )
    }

    val interactiveMod = if (onClick != null) {
        Modifier
            .jellyPress()
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(interactiveMod)
            .clip(outerShape)
            .background(outerBrush)
            .border(width = 0.8.dp, brush = outerBrush, shape = outerShape)
            .padding(bezelThickness)
    ) {
        Box(
            modifier = Modifier
                .clip(innerShape)
                .background(innerBrush)
                .border(width = 0.75.dp, brush = innerRimBrush, shape = innerShape)
                .padding(horizontal = innerPaddingHorizontal, vertical = innerPaddingVertical),
            content = content
        )
    }
}

/**
 * Themed Squircle App Icon that renders the app's monochrome / foreground glyph
 * tinted in the active ProductiveOS theme palette inside a Double-Bezel squircle tile.
 */
@Composable
fun AppIconSquircle(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    lobed: Boolean = false
) {
    val context = LocalContext.current
    val palette = LocalProductiveOSPalette.current
    val shape: Shape = remember(cornerRadius, lobed) {
        if (lobed) LobedSquircleShape(lobes = 4, lobeDepth = 0.055f) else SquircleShape(cornerRadius)
    }

    val iconBitmap = remember(packageName) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toTintableBitmap(160)
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(palette.glassSurfaceElevated)
            .border(0.8.dp, palette.glassBorderTop, shape),
        contentAlignment = Alignment.Center
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap.asImageBitmap(),
                contentDescription = null,
                colorFilter = ColorFilter.tint(palette.primaryText.copy(alpha = 0.90f)),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    }
}

private fun Drawable.toTintableBitmap(px: Int): Bitmap {
    val target: Drawable = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && this is AdaptiveIconDrawable -> {
            this.monochrome ?: this.foreground ?: this
        }
        this is AdaptiveIconDrawable -> this.foreground ?: this
        else -> this
    }
    if (target is BitmapDrawable && target.bitmap != null) {
        return target.bitmap
    }
    val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    target.setBounds(0, 0, px, px)
    target.draw(canvas)
    return bmp
}
