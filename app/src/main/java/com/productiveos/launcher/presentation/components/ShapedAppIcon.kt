package com.productiveos.launcher.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap

/**
 * High-performance in-memory LRU cache for decoded app icon bitmaps.
 * Avoids expensive PackageQuery/IPC and garbage collector churn during frame animations.
 */
object AppIconBitmapCache {
    private val cache = object : LruCache<String, Bitmap>(96) {}

    fun get(context: Context, packageName: String, monochrome: Boolean): Bitmap? {
        val key = "$packageName:$monochrome"
        synchronized(cache) {
            val cached = cache.get(key)
            if (cached != null && !cached.isRecycled) return cached
        }
        val bitmap = runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val layer: Drawable = if (monochrome && drawable is AdaptiveIconDrawable) {
                (if (Build.VERSION.SDK_INT >= 33) drawable.monochrome else null) ?: drawable.foreground
            } else drawable
            layer.toBitmap(144, 144)
        }.getOrNull()

        if (bitmap != null) {
            synchronized(cache) {
                cache.put(key, bitmap)
            }
        }
        return bitmap
    }
}

/**
 * App icon clipped to a Google-style [shape].
 * Supports [monochromeTint] to unify all icon colors into the OS theme palette.
 * When clipped to custom shapes, icon is scaled proportionately to prevent overflow and maintain perfect geometry.
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
    val bitmap = remember(packageName, monochromeTint != null) {
        AppIconBitmapCache.get(context, packageName, monochromeTint != null)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val innerScale = if (monochromeTint != null) 0.68f else 0.82f
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                colorFilter = monochromeTint?.let { ColorFilter.tint(it) },
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
