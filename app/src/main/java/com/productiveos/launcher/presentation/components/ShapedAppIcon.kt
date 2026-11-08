package com.productiveos.launcher.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

data class CachedIconItem(
    val bitmap: Bitmap,
    val isAdaptive: Boolean
)

/**
 * High-performance in-memory LRU cache for decoded app icon bitmaps.
 * Avoids expensive PackageQuery/IPC and garbage collector churn during frame animations.
 */
object AppIconBitmapCache {
    private val cache = object : LruCache<String, CachedIconItem>(96) {}

    fun get(context: Context, packageName: String, monochrome: Boolean): CachedIconItem? {
        val key = "$packageName:$monochrome"
        synchronized(cache) {
            val cached = cache.get(key)
            if (cached != null && !cached.bitmap.isRecycled) return cached
        }
        val item = runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val isAdaptive = drawable is AdaptiveIconDrawable
            val layer: Drawable = if (monochrome && isAdaptive) {
                (if (Build.VERSION.SDK_INT >= 33) (drawable as AdaptiveIconDrawable).monochrome else null)
                    ?: (drawable as AdaptiveIconDrawable).foreground
            } else {
                drawable
            }
            val bmp = layer.toBitmap(144, 144)
            CachedIconItem(bmp, isAdaptive)
        }.getOrNull()

        if (item != null) {
            synchronized(cache) {
                cache.put(key, item)
            }
        }
        return item
    }
}

/**
 * App icon clipped to a Google-style [shape].
 * Supports [monochromeTint] to harmonize with the active OS theme palette.
 * For adaptive icons, tints the foreground glyph; for legacy full-bleed icons,
 * applies a high-contrast desaturation so inner logo details stay clear and legible.
 * Sized and scaled proportionately to prevent overflow and maintain perfect geometry.
 */
@Composable
fun ShapedAppIcon(
    packageName: String,
    size: Dp,
    shape: Shape,
    containerColor: Color,
    modifier: Modifier = Modifier,
    monochromeTint: Color? = null
) {
    val context = LocalContext.current
    val cachedItem = remember(packageName, monochromeTint != null) {
        AppIconBitmapCache.get(context, packageName, monochromeTint != null)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape),
        contentAlignment = Alignment.Center
    ) {
        if (cachedItem != null) {
            val innerScale = if (cachedItem.isAdaptive) {
                if (monochromeTint != null) 0.82f else 0.88f
            } else {
                0.68f // Leave graceful margin for legacy full-square icons inside squircle
            }

            val filter = if (monochromeTint != null) {
                if (cachedItem.isAdaptive) {
                    ColorFilter.tint(monochromeTint)
                } else {
                    // Sleek desaturation matrix for legacy icons so they don't turn into solid black/white boxes
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                }
            } else {
                null
            }

            Image(
                bitmap = cachedItem.bitmap.asImageBitmap(),
                contentDescription = null,
                colorFilter = filter,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = innerScale
                        scaleY = innerScale
                    }
            )
        }
    }
}
