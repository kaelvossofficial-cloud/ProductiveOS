package com.productiveos.launcher.data.repository

import android.content.ClipboardManager
import android.content.Context
import com.productiveos.launcher.data.local.ClipEntity
import com.productiveos.launcher.data.local.MicroLogEntity
import com.productiveos.launcher.data.local.MicroOsDao
import com.productiveos.launcher.data.local.PathCheckLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class WeeklyAwarenessSummary(
    val totalChecks: Int = 0,
    val needInfoPct: Int = 0,
    val boredomPct: Int = 0,
    val habitPct: Int = 0
) {
    val summaryText: String
        get() = if (totalChecks == 0) {
            "no pathcheck reflections logged this week yet"
        } else {
            "intentional: $needInfoPct% · boredom: $boredomPct% · habit: $habitPct% ($totalChecks checks)"
        }
}

@Singleton
class MicroOsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val microOsDao: MicroOsDao
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var clipboardListenerRegistered = false

    val recentClipsFlow: Flow<List<ClipEntity>> = microOsDao.observeRecentClips()
    val microLogsFlow: Flow<List<MicroLogEntity>> = microOsDao.observeMicroLogs()

    fun observeWeeklyAwareness(): Flow<WeeklyAwarenessSummary> {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 3600_000L)
        return microOsDao.observePathCheckLogsSince(sevenDaysAgo).map { list ->
            if (list.isEmpty()) {
                WeeklyAwarenessSummary()
            } else {
                val total = list.size
                val needInfo = list.count { it.intentReason.equals("Need Info", ignoreCase = true) }
                val boredom = list.count { it.intentReason.equals("Boredom", ignoreCase = true) }
                val habit = list.count { it.intentReason.equals("Habit", ignoreCase = true) }
                WeeklyAwarenessSummary(
                    totalChecks = total,
                    needInfoPct = ((needInfo * 100f) / total).roundToInt(),
                    boredomPct = ((boredom * 100f) / total).roundToInt(),
                    habitPct = ((habit * 100f) / total).roundToInt()
                )
            }
        }
    }

    fun startClipboardVaultListener() {
        if (clipboardListenerRegistered) return
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                clipboard.addPrimaryClipChangedListener {
                    captureCurrentPrimaryClip()
                }
                clipboardListenerRegistered = true
            }
        }
    }

    fun captureCurrentPrimaryClip() {
        repoScope.launch {
            runCatching {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = clipboard?.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).coerceToText(context)?.toString()?.trim()
                    if (!text.isNullOrEmpty() && text.length <= 4000) {
                        val now = System.currentTimeMillis()
                        val sevenDaysAgo = now - (7 * 24 * 3600_000L)
                        microOsDao.deleteDuplicateClip(text)
                        microOsDao.insertClip(ClipEntity(content = text, timestamp = now))
                        microOsDao.deleteClipsOlderThan(sevenDaysAgo)
                        microOsDao.trimClipsToMax50()
                    }
                }
            }
        }
    }

    suspend fun deleteClip(id: Long) = withContext(Dispatchers.IO) {
        microOsDao.deleteClipById(id)
    }

    suspend fun clearAllClips() = withContext(Dispatchers.IO) {
        microOsDao.clearAllClips()
    }

    suspend fun appendMicroLog(thought: String): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        microOsDao.insertMicroLog(MicroLogEntity(thought = thought, timestamp = now))

        val isoFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val shortTimeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        val line = "- [${isoFormatter.format(Date(now))}] $thought\n"

        runCatching {
            val internalFile = File(context.filesDir, "ProductiveOS_Inbox.md")
            internalFile.appendText(line)

            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                extDir.mkdirs()
                File(extDir, "Inbox.md").appendText(line)
            }
        }

        shortTimeFormatter.format(Date(now))
    }

    suspend fun deleteMicroLog(id: Long) = withContext(Dispatchers.IO) {
        microOsDao.deleteMicroLogById(id)
    }

    suspend fun readFullMarkdownJournal(): String = withContext(Dispatchers.IO) {
        runCatching {
            val internalFile = File(context.filesDir, "ProductiveOS_Inbox.md")
            if (internalFile.exists()) internalFile.readText() else ""
        }.getOrDefault("")
    }

    suspend fun recordPathCheckChoice(
        packageName: String,
        appName: String,
        intentReason: String,
        proceeded: Boolean
    ) = withContext(Dispatchers.IO) {
        microOsDao.insertPathCheckLog(
            PathCheckLogEntity(
                packageName = packageName,
                appName = appName,
                intentReason = intentReason,
                proceeded = proceeded,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}

