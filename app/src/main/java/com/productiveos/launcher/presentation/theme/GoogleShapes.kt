package com.productiveos.launcher.presentation.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Polar "lobed" shape: r(t) = 1 + amplitude * cos(lobes * (t - rotation)), blended with a
 * superellipse so square-ish silhouettes stay possible.
 *
 * Performance-optimized: Caches the generated [Outline] by pixel dimensions so scrolling
 * lists and floating animations never recompute trigonometry on the UI thread.
 */
class PolarLobeShape(
    private val lobes: Int,
    private val amplitude: Float,
    private val rotationDeg: Float = 0f,
    private val squareness: Float = 2f
) : Shape {
    private var cachedWidth: Int = -1
    private var cachedHeight: Int = -1
    private var cachedOutline: Outline? = null

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width.toInt()
        val h = size.height.toInt()
        val existing = cachedOutline
        if (existing != null && w == cachedWidth && h == cachedHeight) {
            return existing
        }

        val cx = size.width / 2f
        val cy = size.height / 2f
        val steps = 120
        val rot = (rotationDeg * PI / 180.0).toFloat()
        val path = Path()
        val ex = 2f / squareness
        for (i in 0..steps) {
            val t = (2.0 * PI * i / steps).toFloat()
            val c = cos(t)
            val s = sin(t)
            val bx = sign(c) * abs(c).toDouble().pow(ex.toDouble()).toFloat()
            val by = sign(s) * abs(s).toDouble().pow(ex.toDouble()).toFloat()
            val wobble = (1f + amplitude * cos(lobes * (t - rot))) / (1f + amplitude)
            val x = cx + bx * cx * wobble
            val y = cy + by * cy * wobble
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path).also {
            cachedWidth = w
            cachedHeight = h
            cachedOutline = it
        }
    }
}

/** The Google-style shape vocabulary used for floating home icons and drawer grid icons. */
enum class GoogleShape(val shape: Shape) {
    COOKIE(PolarLobeShape(lobes = 9, amplitude = 0.07f)),
    FLOWER(PolarLobeShape(lobes = 8, amplitude = 0.13f)),
    CLOVER(PolarLobeShape(lobes = 4, amplitude = 0.20f, rotationDeg = 45f)),
    SUNNY(PolarLobeShape(lobes = 12, amplitude = 0.075f)),
    SOFT_TRIANGLE(PolarLobeShape(lobes = 3, amplitude = 0.16f, rotationDeg = -90f)),
    SQUIRCLE(PolarLobeShape(lobes = 4, amplitude = 0.0f, squareness = 4f)),
    PEBBLE(PolarLobeShape(lobes = 2, amplitude = 0.09f, rotationDeg = 20f)),
    CIRCLE(PolarLobeShape(lobes = 1, amplitude = 0.0f));

    companion object {
        /** Stable shape for an index/hash so each app always keeps its own silhouette. */
        fun forKey(key: Int): GoogleShape = entries[abs(key) % entries.size]
    }
}
