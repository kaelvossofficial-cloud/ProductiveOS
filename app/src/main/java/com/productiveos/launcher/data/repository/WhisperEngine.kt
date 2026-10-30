package com.productiveos.launcher.data.repository

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed class WhisperItem(val text: String, val category: String) {
    data class CalendarEvent(val title: String, val startFormatted: String) :
        WhisperItem(text = "$title in $startFormatted", category = "calendar")

    data class MediaNowPlaying(val title: String, val isPlaying: Boolean) :
        WhisperItem(text = "♪ $title", category = "media")

    data class BatteryStatus(val percentage: Int, val isCharging: Boolean) :
        WhisperItem(
            text = if (isCharging) "$percentage% charged" else "$percentage% battery",
            category = "battery"
        )

    data class MindfulnessMotto(val quote: String) :
        WhisperItem(text = quote, category = "zen")
}

@Singleton
class WhisperEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _currentWhisper = MutableStateFlow<WhisperItem>(
        WhisperItem.MindfulnessMotto("stay intentional today")
    )
    val currentWhisper: StateFlow<WhisperItem> = _currentWhisper.asStateFlow()

    private var whisperIndex = 0

    suspend fun refreshWhisper() = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<WhisperItem>()

        // 1. Check Music / Media state
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager?.isMusicActive == true) {
                candidates.add(WhisperItem.MediaNowPlaying("audio playing · tap to pause", true))
            }
        }

        // 2. Check Upcoming Calendar Event within 3 hours
        val nextCalendar = queryNextUpcomingCalendarEvent()
        if (nextCalendar != null) {
            candidates.add(nextCalendar)
        }

        // 3. Battery status
        val battery = queryBatteryStatus()
        candidates.add(battery)

        // 4. Fallback Zen motto
        candidates.add(WhisperItem.MindfulnessMotto("breathe · intentional mode"))

        if (candidates.isNotEmpty()) {
            val selected = candidates[whisperIndex % candidates.size]
            _currentWhisper.value = selected
            whisperIndex++
        }
    }

    private fun queryBatteryStatus(): WhisperItem.BatteryStatus {
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, batteryFilter)
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val pct = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100).toInt()
        } else {
            100
        }
        return WhisperItem.BatteryStatus(percentage = pct, isCharging = isCharging)
    }

    private fun queryNextUpcomingCalendarEvent(): WhisperItem.CalendarEvent? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        return runCatching {
            val now = System.currentTimeMillis()
            val threeHoursLater = now + (3 * 3600_000L)

            val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(uriBuilder, now)
            ContentUris.appendId(uriBuilder, threeHoursLater)

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN
            )

            context.contentResolver.query(
                uriBuilder.build(),
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)

                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) else "Event"
                    val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else now

                    val diffMinutes = ((begin - now) / 60_000L).coerceAtLeast(1)
                    val startLabel = if (diffMinutes < 60) {
                        "${diffMinutes}m"
                    } else {
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        timeFormat.format(begin).lowercase()
                    }

                    WhisperItem.CalendarEvent(title = title, startFormatted = startLabel)
                } else null
            }
        }.getOrNull()
    }
}

