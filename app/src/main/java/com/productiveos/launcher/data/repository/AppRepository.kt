package com.productiveos.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import com.productiveos.launcher.data.local.AppDao
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val preferencesRepository: LauncherPreferencesRepository
) {

    val visibleAppsFlow: Flow<List<AppEntity>> = appDao.observeVisibleApps()
    val pinnedAppsFlow: Flow<List<AppEntity>> = appDao.observePinnedApps()
    val hiddenAppsFlow: Flow<List<AppEntity>> = appDao.observeHiddenApps()

    suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val installedList = querySystemLaunchableApps()
        if (installedList.isEmpty()) return@withContext

        val existingByPkg = appDao.getAllAppsSnapshot().associateBy { it.packageName }
        val prefs = preferencesRepository.settingsFlow.first()

        val defaultPinnedMap = if (!prefs.hasInitializedDefaults) {
            resolveDefaultEssentialPackages(installedList)
        } else {
            emptyMap()
        }

        val mergedEntities = installedList.map { (pkg, systemLabel) ->
            val existing = existingByPkg[pkg]
            if (existing != null) {
                existing.copy(label = systemLabel)
            } else {
                val defaultSlot = defaultPinnedMap[pkg]
                AppEntity(
                    packageName = pkg,
                    label = systemLabel,
                    customLabel = defaultSlot?.customLabel,
                    isPinned = defaultSlot != null,
                    pinOrder = defaultSlot?.order ?: 999,
                    isHidden = false,
                    isFrictionEnabled = isDefaultFrictionPackage(pkg, systemLabel),
                    launchCount = 0,
                    lastUsedTime = 0L
                )
            }
        }

        appDao.upsertAll(mergedEntities)
        appDao.deleteRemovedApps(installedList.map { it.first })

        if (!prefs.hasInitializedDefaults && mergedEntities.isNotEmpty()) {
            val anyPinned = mergedEntities.any { it.isPinned }
            if (!anyPinned) {
                val fallbackTop5 = mergedEntities
                    .sortedBy { it.label.lowercase() }
                    .take(5)
                    .mapIndexed { index, entity ->
                        entity.copy(isPinned = true, pinOrder = index)
                    }
                appDao.upsertAll(fallbackTop5)
            }
            preferencesRepository.markDefaultsInitialized()
        }
    }

    private fun querySystemLaunchableApps(): List<Pair<String, String>> {
        val result = linkedMapOf<String, String>()
        val selfPkg = context.packageName

        runCatching {
            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val activities =
                launcherApps?.getActivityList(null, Process.myUserHandle()).orEmpty()
            for (info in activities) {
                val pkg = info.applicationInfo.packageName
                if (pkg != selfPkg && !result.containsKey(pkg)) {
                    val label = info.label?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: pkg
                    result[pkg] = label
                }
            }
        }

        if (result.isEmpty()) {
            runCatching {
                val pm = context.packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                for (ri in resolveInfos) {
                    val pkg = ri.activityInfo?.packageName ?: continue
                    if (pkg != selfPkg && !result.containsKey(pkg)) {
                        val label = ri.loadLabel(pm)?.toString()?.trim()
                            .takeUnless { it.isNullOrEmpty() } ?: pkg
                        result[pkg] = label
                    }
                }
            }
        }

        return result.entries.map { it.key to it.value }
    }

    private data class DefaultPinSlot(val order: Int, val customLabel: String)

    private fun resolveDefaultEssentialPackages(
        installed: List<Pair<String, String>>
    ): Map<String, DefaultPinSlot> {
        val assigned = linkedMapOf<String, DefaultPinSlot>()
        val installedSet = installed.map { it.first }.toSet()

        fun pickCandidate(
            intentCandidate: Intent?,
            keywords: List<String>
        ): String? {
            if (intentCandidate != null) {
                val resolvedPkg = runCatching {
                    context.packageManager.resolveActivity(
                        intentCandidate,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )?.activityInfo?.packageName
                }.getOrNull()
                if (resolvedPkg != null && resolvedPkg in installedSet && resolvedPkg !in assigned) {
                    return resolvedPkg
                }
            }

            for (kw in keywords) {
                val match = installed.firstOrNull { (pkg, label) ->
                    pkg !in assigned && (
                        label.lowercase() == kw ||
                            label.lowercase().contains(kw) ||
                            pkg.lowercase().contains(kw)
                        )
                }
                if (match != null) return match.first
            }
            return null
        }

        val slots = listOf(
            Triple(
                0,
                "phone",
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:")) to listOf("phone", "dialer", "telecom", "call")
            ),
            Triple(
                1,
                "messages",
                Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) } to
                    listOf("messages", "messaging", "mms", "sms", "chat")
            ),
            Triple(
                2,
                "browser",
                Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")) to
                    listOf("chrome", "browser", "brave", "firefox", "opera", "duckduckgo", "webview")
            ),
            Triple(
                3,
                "notes",
                null to listOf("keep", "notes", "notion", "obsidian", "memo", "calendar", "files", "clock")
            ),
            Triple(
                4,
                "camera",
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA) to
                    listOf("camera", "cam", "photos", "gallery")
            )
        )

        for ((order, slotName, criteria) in slots) {
            val (intent, keywords) = criteria
            val chosenPkg = pickCandidate(intent, keywords)
            if (chosenPkg != null) {
                assigned[chosenPkg] = DefaultPinSlot(order = order, customLabel = slotName)
            }
        }

        if (assigned.size < 5) {
            val remaining = installed
                .map { it.first }
                .filter { it !in assigned }
                .take(5 - assigned.size)
            var nextOrder = assigned.size
            for (pkg in remaining) {
                val label = installed.first { it.first == pkg }.second.lowercase()
                assigned[pkg] = DefaultPinSlot(order = nextOrder++, customLabel = label)
            }
        }

        return assigned
    }

    private fun isDefaultFrictionPackage(packageName: String, label: String): Boolean {
        val pkgLower = packageName.lowercase()
        val labelLower = label.lowercase()
        val timeSinkTokens = listOf(
            "instagram",
            "tiktok",
            "musically",
            "youtube",
            "twitter",
            "com.x.android",
            "facebook",
            "reddit",
            "snapchat",
            "netflix",
            "twitch"
        )
        return timeSinkTokens.any { token -> pkgLower.contains(token) || labelLower.contains(token) }
    }

    suspend fun togglePinApp(app: AppEntity): Boolean = withContext(Dispatchers.IO) {
        if (app.isPinned) {
            appDao.updatePinned(app.packageName, pinned = false, pinOrder = 999)
            return@withContext true
        }
        val currentPinned = appDao.getAllAppsSnapshot().filter { it.isPinned && !it.isHidden }
        if (currentPinned.size >= 6) {
            return@withContext false
        }
        val nextOrder = (currentPinned.maxOfOrNull { it.pinOrder } ?: -1) + 1
        appDao.updatePinned(app.packageName, pinned = true, pinOrder = nextOrder)
        true
    }

    suspend fun swapPinnedApp(oldPackageName: String, newPackageName: String) =
        withContext(Dispatchers.IO) {
            val oldApp = appDao.getAppByPackage(oldPackageName)
            val order = oldApp?.pinOrder ?: 0
            appDao.updatePinned(oldPackageName, pinned = false, pinOrder = 999)
            appDao.updatePinned(newPackageName, pinned = true, pinOrder = order)
        }

    suspend fun movePinnedAppOrder(packageName: String, moveUp: Boolean) =
        withContext(Dispatchers.IO) {
            val pinned = appDao.getAllAppsSnapshot()
                .filter { it.isPinned && !it.isHidden }
                .sortedWith(compareBy({ it.pinOrder }, { it.displayName.lowercase() }))
                .toMutableList()
            val index = pinned.indexOfFirst { it.packageName == packageName }
            if (index == -1) return@withContext
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in pinned.indices) return@withContext

            val item = pinned.removeAt(index)
            pinned.add(targetIndex, item)
            val updated = pinned.mapIndexed { idx, appEntity ->
                appEntity.copy(pinOrder = idx)
            }
            appDao.upsertAll(updated)
        }

    suspend fun renameApp(packageName: String, newCustomLabel: String?) =
        withContext(Dispatchers.IO) {
            val cleaned = newCustomLabel?.trim()?.takeIf { it.isNotEmpty() }
            appDao.updateCustomLabel(packageName, cleaned)
        }

    suspend fun setFrictionEnabled(packageName: String, enabled: Boolean) =
        withContext(Dispatchers.IO) {
            appDao.updateFriction(packageName, enabled)
        }

    suspend fun setAppSwipeAction(packageName: String, swipePackage: String?) =
        withContext(Dispatchers.IO) {
            appDao.updateSwipeAction(packageName, swipePackage)
        }

    suspend fun setAppHidden(packageName: String, hidden: Boolean) =
        withContext(Dispatchers.IO) {
            appDao.updateHidden(packageName, hidden)
        }

    suspend fun recordAppLaunch(packageName: String) = withContext(Dispatchers.IO) {
        appDao.incrementLaunchCount(packageName, System.currentTimeMillis())
    }

    fun filterApps(
        apps: List<AppEntity>,
        rawQuery: String,
        enableT9: Boolean
    ): List<AppEntity> {
        val query = rawQuery.trim().lowercase()
        if (query.isEmpty()) return apps

        val isNumericQuery = query.all { it in '2'..'9' || it == '0' }

        return apps.filter { app ->
            val display = app.displayName.lowercase()
            val systemLabel = app.label.lowercase()
            val directMatch = display.contains(query) || systemLabel.contains(query)
            val t9Match = if (enableT9 && isNumericQuery) {
                matchesT9(display, query) || matchesT9(systemLabel, query)
            } else {
                false
            }
            directMatch || t9Match
        }.sortedWith(
            compareByDescending<AppEntity> { it.displayName.lowercase().startsWith(query) }
                .thenByDescending { it.launchCount }
                .thenBy { it.displayName.lowercase() }
        )
    }

    private fun matchesT9(text: String, digitsQuery: String): Boolean {
        val t9Digits = buildString(text.length) {
            for (ch in text) {
                when (ch) {
                    in 'a'..'c' -> append('2')
                    in 'd'..'f' -> append('3')
                    in 'g'..'i' -> append('4')
                    in 'j'..'l' -> append('5')
                    in 'm'..'o' -> append('6')
                    in 'p'..'s' -> append('7')
                    in 't'..'v' -> append('8')
                    in 'w'..'z' -> append('9')
                    ' ' -> append('0')
                    in '0'..'9' -> append(ch)
                }
            }
        }
        return t9Digits.contains(digitsQuery)
    }
}

