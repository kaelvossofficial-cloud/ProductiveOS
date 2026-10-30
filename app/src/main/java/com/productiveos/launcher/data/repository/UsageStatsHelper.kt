package com.productiveos.launcher.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class DailyTelemetry(
    val hasPermission: Boolean = false,
    val totalScreenTimeMinutes: Int = 0,
    val unlockCount: Int = 0
) {
    val formattedText: String
        get() {
            if (!hasPermission) return "enable usage access for screen time →"
            val hours = totalScreenTimeMinutes / 60
            val minutes = totalScreenTimeMinutes % 60
            val timePart = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
            val unlockLabel = if (unlockCount == 1) "1 unlock" else "$unlockCount unlocks"
            return "$timePart screen time · $unlockLabel"
        }
}

@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasUsageStatsPermission(): Boolean {
        return runCatching {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }

    suspend fun queryTodayTelemetry(): DailyTelemetry = withContext(Dispatchers.IO) {
        if (!hasUsageStatsPermission()) {
            return@withContext DailyTelemetry(hasPermission = false)
        }

        runCatching {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return@withContext DailyTelemetry(hasPermission = false)

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val usageEvents = usageStatsManager.queryEvents(startOfDay, now)
            val event = UsageEvents.Event()

            var unlocks = 0
            val foregroundStarts = mutableMapOf<String, Long>()
            var totalForegroundMs = 0L

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.KEYGUARD_HIDDEN -> {
                        unlocks++
                    }
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        val pkg = event.packageName ?: continue
                        if (pkg != context.packageName && !pkg.contains("systemui")) {
                            foregroundStarts[pkg] = event.timeStamp
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val pkg = event.packageName ?: continue
                        val startTs = foregroundStarts.remove(pkg)
                        if (startTs != null && event.timeStamp > startTs) {
                            val delta = event.timeStamp - startTs
                            if (delta in 1_000L..(6 * 3600_000L)) {
                                totalForegroundMs += delta
                            }
                        }
                    }
                }
            }

            if (totalForegroundMs == 0L) {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startOfDay,
                    now
                )
                totalForegroundMs = stats
                    ?.filter { it.packageName != context.packageName && !it.packageName.contains("systemui") }
                    ?.sumOf { it.totalTimeInForeground.coerceAtLeast(0L) }
                    ?: 0L
            }

            val totalMinutes = (totalForegroundMs / 60_000L).toInt().coerceAtLeast(0)
            DailyTelemetry(
                hasPermission = true,
                totalScreenTimeMinutes = totalMinutes,
                unlockCount = unlocks.coerceAtLeast(1)
            )
        }.getOrElse {
            DailyTelemetry(hasPermission = true, totalScreenTimeMinutes = 0, unlockCount = 0)
        }
    }
}

