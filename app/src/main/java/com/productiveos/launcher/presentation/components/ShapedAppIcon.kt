package com.productiveos.launcher.presentation.components

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
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

private fun Drawable.toIconBitmap(sizePx: Int, monochrome: Boolean) = runCatching {
    val layer: Drawable = if (monochrome && this is AdaptiveIconDrawable) {
        (if (Build.VERSION.SDK_INT >= 33) this.monochrome else null) ?: this.foreground
    } else this
    layer.toBitmap(sizePx, sizePx)
}.getOrNull()

/**
 * App icon clipped to a Google-style [shape]. Colored by default; set [monochromeTint] to flatten
 * the glyph into the active theme instead.
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
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toIconBitmap(192, monochromeTint != null)
        }.getOrNull()
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                colorFilter = monochromeTint?.let { ColorFilter.tint(it) },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        val s = if (monochromeTint == null) 1.2f else 0.9f
                        scaleX = s
                        scaleY = s
                    }
            )
        }
    }
}

