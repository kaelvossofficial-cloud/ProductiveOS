package com.productiveos.launcher.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build

object WallpaperHelper {

    fun setSolidThemeWallpaper(context: Context, isLight: Boolean): Boolean {
        return runCatching {
            val wallpaperColor = if (isLight) {
                Color.parseColor("#F5F5F0") // Warm stone
            } else {
                Color.parseColor("#000000") // Pure OLED black
            }

            // Create 1080x2400 solid bitmap
            val bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(wallpaperColor)

            val wm = WallpaperManager.getInstance(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(
                    bitmap,
                    null,
                    true,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                )
            } else {
                wm.setBitmap(bitmap)
            }
            bitmap.recycle()
            true
        }.getOrDefault(false)
    }
}

