package com.productiveos.launcher.presentation.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

/**
 * Hold-to-open gesture with a quick-action milestone.
 *
 * - Reports progress 0..1 every ~16ms while the finger rests on the target.
 * - At [quickActionMs] fires [onQuickActions] once (the small capsule appears).
 * - At 100% fires [onComplete] (launches the app).
 * - Moving past touch slop cancels so scrolling/swiping stays natural.
 * When [enabled] is false it degrades to a plain tap.
 */
fun Modifier.holdToOpenGesture(
    key: Any?,
    enabled: Boolean,
    holdMs: Long,
    quickActionMs: Long,
    onHoldStart: () -> Unit,
    onProgress: (Float) -> Unit,
    onQuickActions: () -> Unit,
    onComplete: () -> Unit,
    onRelease: () -> Unit,
    onTap: () -> Unit
): Modifier = pointerInput(key, enabled, holdMs, quickActionMs) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!enabled) {
            val up = waitForUpOrCancellation()
            if (up != null) onTap()
            return@awaitEachGesture
        }
        val slop = viewConfiguration.touchSlop * 2f
        val start = SystemClock.uptimeMillis()
        var quickFired = false
        var completed = false
        onHoldStart()
        while (true) {
            val event = withTimeoutOrNull(16L) { awaitPointerEvent() }
            val elapsed = SystemClock.uptimeMillis() - start
            if (event != null) {
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                if ((change.position - down.position).getDistance() > slop) break
            }
            if (!quickFired && elapsed >= quickActionMs) {
                quickFired = true
                onQuickActions()
            }
            val p = (elapsed.toFloat() / holdMs.toFloat()).coerceIn(0f, 1f)
            onProgress(p)
            if (p >= 1f) {
                completed = true
                break
            }
        }
        if (completed) onComplete()
        onRelease()
    }
}

/** Soft magical halo that blooms behind its content while [progress] rises. Never clips. */
fun Modifier.holdGlow(progress: () -> Float, core: Color, halo: Color): Modifier = drawBehind {
    val p = progress()
    if (p > 0.01f) {
        val radius = size.minDimension * (0.8f + 0.95f * p)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    core.copy(alpha = 0.55f * p),
                    halo.copy(alpha = 0.24f * p),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}

/**
 * Smoothed device tilt. x: +1 = phone tipped to the right, y: +1 = upright, -1 = upside down.
 * Read the returned State only inside graphicsLayer / draw lambdas to avoid recomposition.
 */
@Composable
fun rememberTilt(enabled: Boolean = true): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var smoothX = 0f
        var smoothY = 1f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val targetX = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
                val targetY = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
                smoothX += (targetX - smoothX) * 0.09f
                smoothY += (targetY - smoothY) * 0.09f
                tilt.value = Offset(smoothX, smoothY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (enabled && manager != null && sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { manager?.unregisterListener(listener) }
    }
    return tilt
}

