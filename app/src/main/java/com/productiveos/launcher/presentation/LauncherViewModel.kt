package com.productiveos.launcher.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.local.ClipEntity
import com.productiveos.launcher.data.local.MicroLogEntity
import com.productiveos.launcher.data.preferences.LauncherPreferencesRepository
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.LauncherThemeOption
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.preferences.TypographyStyleOption
import com.productiveos.launcher.data.repository.AppRepository
import com.productiveos.launcher.data.repository.DailyTelemetry
import com.productiveos.launcher.data.repository.MicroOsRepository
import com.productiveos.launcher.data.repository.UsageStatsHelper
import com.productiveos.launcher.data.repository.WeeklyAwarenessSummary
import com.productiveos.launcher.data.repository.WhisperEngine
import com.productiveos.launcher.data.repository.WhisperItem
import com.productiveos.launcher.util.FocusAudioGenerator
import com.productiveos.launcher.util.FocusSoundMode
import com.productiveos.launcher.util.OmniSearchEvaluator
import com.productiveos.launcher.util.OmniSearchResult
import com.productiveos.launcher.util.SystemLauncherHelper
import com.productiveos.launcher.util.WallpaperHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class LauncherScreen {
    ZEN_HOME,
    SEARCH_DRAWER,
    SETTINGS
}

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appRepository: AppRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val usageStatsHelper: UsageStatsHelper,
    private val whisperEngine: WhisperEngine,
    private val microOsRepository: MicroOsRepository,
    private val focusAudioGenerator: FocusAudioGenerator
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(LauncherScreen.ZEN_HOME)
    val currentScreen: StateFlow<LauncherScreen> = _currentScreen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _frictionTargetApp = MutableStateFlow<AppEntity?>(null)
    val frictionTargetApp: StateFlow<AppEntity?> = _frictionTargetApp.asStateFlow()

    private val _selectedContextApp = MutableStateFlow<AppEntity?>(null)
    val selectedContextApp: StateFlow<AppEntity?> = _selectedContextApp.asStateFlow()

    private val _swapTargetApp = MutableStateFlow<AppEntity?>(null)
    val swapTargetApp: StateFlow<AppEntity?> = _swapTargetApp.asStateFlow()

    private val _isQuantumClipOpen = MutableStateFlow(false)
    val isQuantumClipOpen: StateFlow<Boolean> = _isQuantumClipOpen.asStateFlow()

    private val _quantumClipFilter = MutableStateFlow("")
    val quantumClipFilter: StateFlow<String> = _quantumClipFilter.asStateFlow()

    private val _isMicroLogOpen = MutableStateFlow(false)
    val isMicroLogOpen: StateFlow<Boolean> = _isMicroLogOpen.asStateFlow()

    private val _isBurnerNoteOpen = MutableStateFlow(false)
    val isBurnerNoteOpen: StateFlow<Boolean> = _isBurnerNoteOpen.asStateFlow()

    private val _dailyTelemetry = MutableStateFlow(DailyTelemetry())
    val dailyTelemetry: StateFlow<DailyTelemetry> = _dailyTelemetry.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(true)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val currentWhisper: StateFlow<WhisperItem> = whisperEngine.currentWhisper
    val activeFocusSound: StateFlow<FocusSoundMode?> = focusAudioGenerator.activeMode

    val recentClips: StateFlow<List<ClipEntity>> = microOsRepository.recentClipsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val microLogs: StateFlow<List<MicroLogEntity>> = microOsRepository.microLogsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val weeklyAwareness: StateFlow<WeeklyAwarenessSummary> = microOsRepository.observeWeeklyAwareness()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeeklyAwarenessSummary()
        )

    val settingsState: StateFlow<LauncherSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LauncherSettings()
        )

    val effectiveTheme: StateFlow<LauncherThemeOption> = combine(
        settingsState
    ) { settingsArray ->
        val settings = settingsArray[0]
        if (settings.autoDayNightTheme) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour in 6..16) {
                LauncherThemeOption.WARM_STONE
            } else {
                LauncherThemeOption.OLED_BLACK
            }
        } else {
            settings.themeOption
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherThemeOption.OLED_BLACK
    )

    val visibleApps: StateFlow<List<AppEntity>> = appRepository.visibleAppsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val pinnedApps: StateFlow<List<AppEntity>> = appRepository.pinnedAppsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val hiddenApps: StateFlow<List<AppEntity>> = appRepository.hiddenAppsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val filteredSearchApps: StateFlow<List<AppEntity>> = combine(
        visibleApps,
        _searchQuery,
        settingsState
    ) { apps, query, settings ->
        appRepository.filterApps(apps, query, settings.enableT9Search)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val omniSearchResult: StateFlow<OmniSearchResult?> = combine(
        _searchQuery,
        settingsState
    ) { query, settings ->
        if (settings.omniSearchToolsEnabled) {
            OmniSearchEvaluator.evaluate(query)
        } else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    init {
        runCatching { microOsRepository.startClipboardVaultListener() }
        refreshAllState()
    }

    fun refreshAllState() {
        viewModelScope.launch {
            runCatching {
                _isDefaultLauncher.value = SystemLauncherHelper.isDefaultHomeLauncher(appContext)
            }
            runCatching {
                preferencesRepository.purgeExpiredBurnerNoteIfNeeded()
            }
            runCatching {
                microOsRepository.captureCurrentPrimaryClip()
            }
            runCatching {
                appRepository.syncInstalledApps()
            }
            runCatching {
                _dailyTelemetry.value = usageStatsHelper.queryTodayTelemetry()
            }
            runCatching {
                whisperEngine.refreshWhisper()
            }
        }
    }

    fun openSearchDrawer() {
        _searchQuery.value = ""
        _currentScreen.value = LauncherScreen.SEARCH_DRAWER
    }

    fun openSettingsScreen() {
        _currentScreen.value = LauncherScreen.SETTINGS
    }

    fun returnToHome() {
        _searchQuery.value = ""
        _selectedContextApp.value = null
        _swapTargetApp.value = null
        _frictionTargetApp.value = null
        _isQuantumClipOpen.value = false
        _isMicroLogOpen.value = false
        _isBurnerNoteOpen.value = false
        _currentScreen.value = LauncherScreen.ZEN_HOME
        viewModelScope.launch {
            _isDefaultLauncher.value = SystemLauncherHelper.isDefaultHomeLauncher(appContext)
            _dailyTelemetry.value = usageStatsHelper.queryTodayTelemetry()
            whisperEngine.refreshWhisper()
        }
    }

    fun updateSearchQuery(newQuery: String, onAutoLaunch: (AppEntity) -> Unit) {
        val previousQuery = _searchQuery.value
        _searchQuery.value = newQuery

        val settings = settingsState.value
        if (
            settings.autoLaunchSingleResult &&
            newQuery.trim().length > previousQuery.trim().length &&
            newQuery.trim().length >= 2
        ) {
            val omni = if (settings.omniSearchToolsEnabled) OmniSearchEvaluator.evaluate(newQuery) else null
            if (omni == null) {
                val matches = appRepository.filterApps(
                    visibleApps.value,
                    newQuery,
                    settings.enableT9Search
                )
                if (matches.size == 1) {
                    onAutoLaunch(matches.first())
                }
            }
        }
    }

    fun onAppRequested(app: AppEntity, executeSystemLaunch: (String) -> Unit) {
        val settings = settingsState.value
        if (app.isFrictionEnabled && settings.mindfulFrictionEnabled) {
            _frictionTargetApp.value = app
        } else {
            confirmAndLaunchApp(app, null, executeSystemLaunch)
        }
    }

    fun confirmAndLaunchApp(
        app: AppEntity,
        intentReason: String? = null,
        executeSystemLaunch: (String) -> Unit
    ) {
        _frictionTargetApp.value = null
        _selectedContextApp.value = null
        _searchQuery.value = ""
        _currentScreen.value = LauncherScreen.ZEN_HOME
        viewModelScope.launch {
            if (intentReason != null) {
                microOsRepository.recordPathCheckChoice(
                    packageName = app.packageName,
                    appName = app.displayName,
                    intentReason = intentReason,
                    proceeded = true
                )
            }
            appRepository.recordAppLaunch(app.packageName)
        }
        executeSystemLaunch(app.packageName)
    }

    fun dismissFrictionOverlay(intentReason: String = "Habit") {
        val target = _frictionTargetApp.value
        if (target != null) {
            viewModelScope.launch {
                microOsRepository.recordPathCheckChoice(
                    packageName = target.packageName,
                    appName = target.displayName,
                    intentReason = intentReason,
                    proceeded = false
                )
            }
        }
        _frictionTargetApp.value = null
        _currentScreen.value = LauncherScreen.ZEN_HOME
    }

    // Micro-OS 1: Quantum Clip
    fun openQuantumClipVault(initialFilter: String = "") {
        microOsRepository.captureCurrentPrimaryClip()
        _quantumClipFilter.value = initialFilter
        _isQuantumClipOpen.value = true
    }

    fun closeQuantumClipVault() {
        _isQuantumClipOpen.value = false
    }

    fun deleteClip(id: Long) {
        viewModelScope.launch {
            microOsRepository.deleteClip(id)
        }
    }

    fun clearAllClips() {
        viewModelScope.launch {
            microOsRepository.clearAllClips()
            showTemporaryMessage("Quantum Clip vault cleared")
        }
    }

    // Micro-OS 2: Focus Anchor
    fun triggerFocusSound(mode: FocusSoundMode?) {
        if (mode == null) {
            focusAudioGenerator.stopSound()
            showTemporaryMessage("Focus Anchor stopped")
        } else {
            focusAudioGenerator.startSound(mode)
            showTemporaryMessage("♪ Playing Focus Anchor: ${mode.label}")
        }
        _searchQuery.value = ""
        _currentScreen.value = LauncherScreen.ZEN_HOME
    }

    fun toggleFocusSoundOnHome() {
        focusAudioGenerator.toggleOrCycleSound()
    }

    // Micro-OS 3: Micro-Log
    fun captureMicroLogFromOmni(thought: String) {
        viewModelScope.launch {
            val timeFormatted = microOsRepository.appendMicroLog(thought)
            _searchQuery.value = ""
            showTemporaryMessage("✓ Saved to Journal ($timeFormatted)")
        }
    }

    fun openMicroLogJournal() {
        _isMicroLogOpen.value = true
    }

    fun closeMicroLogJournal() {
        _isMicroLogOpen.value = false
    }

    fun deleteMicroLog(id: Long) {
        viewModelScope.launch {
            microOsRepository.deleteMicroLog(id)
        }
    }

    fun shareMicroLogMarkdown(onShareText: (String) -> Unit) {
        viewModelScope.launch {
            val md = microOsRepository.readFullMarkdownJournal()
            if (md.isNotBlank()) {
                onShareText(md)
            } else {
                showTemporaryMessage("Journal is empty")
            }
        }
    }

    // Micro-OS 4: Burner Notes
    fun openBurnerNote() {
        viewModelScope.launch {
            preferencesRepository.purgeExpiredBurnerNoteIfNeeded()
            _isBurnerNoteOpen.value = true
        }
    }

    fun closeBurnerNote() {
        _isBurnerNoteOpen.value = false
    }

    fun updateBurnerNoteText(text: String) {
        viewModelScope.launch {
            preferencesRepository.updateBurnerNote(text)
        }
    }

    fun openAppContextSheet(app: AppEntity) {
        _selectedContextApp.value = app
    }

    fun dismissAppContextSheet() {
        _selectedContextApp.value = null
    }

    fun openSwapPinnedSheet(currentPinnedApp: AppEntity) {
        _selectedContextApp.value = null
        _swapTargetApp.value = currentPinnedApp
    }

    fun dismissSwapPinnedSheet() {
        _swapTargetApp.value = null
    }

    fun confirmSwapPinnedApp(oldApp: AppEntity, replacementApp: AppEntity) {
        viewModelScope.launch {
            appRepository.swapPinnedApp(oldApp.packageName, replacementApp.packageName)
            _swapTargetApp.value = null
            showTemporaryMessage("Swapped ${oldApp.displayName.lowercase()} → ${replacementApp.displayName.lowercase()}")
        }
    }

    fun togglePin(app: AppEntity) {
        viewModelScope.launch {
            val success = appRepository.togglePinApp(app)
            _selectedContextApp.value = null
            if (!success) {
                showTemporaryMessage("Max 6 pinned apps reached. Unpin or swap one first.")
            }
        }
    }

    fun movePinnedOrder(app: AppEntity, moveUp: Boolean) {
        viewModelScope.launch {
            appRepository.movePinnedAppOrder(app.packageName, moveUp)
        }
    }

    fun renameAppLabel(app: AppEntity, newLabel: String?) {
        viewModelScope.launch {
            appRepository.renameApp(app.packageName, newLabel)
            _selectedContextApp.value = null
        }
    }

    fun toggleMindfulFriction(app: AppEntity) {
        viewModelScope.launch {
            val nextState = !app.isFrictionEnabled
            appRepository.setFrictionEnabled(app.packageName, nextState)
            _selectedContextApp.value = null
            val status = if (nextState) "enabled" else "disabled"
            showTemporaryMessage("PathCheck gate $status for ${app.displayName.lowercase()}")
        }
    }

    fun setAppSwipeAction(app: AppEntity, swipePackage: String?) {
        viewModelScope.launch {
            appRepository.setAppSwipeAction(app.packageName, swipePackage)
            _selectedContextApp.value = null
            showTemporaryMessage("Swipe action updated for ${app.displayName.lowercase()}")
        }
    }

    fun setHidden(app: AppEntity, hidden: Boolean) {
        viewModelScope.launch {
            appRepository.setAppHidden(app.packageName, hidden)
            _selectedContextApp.value = null
            if (hidden) {
                showTemporaryMessage("${app.displayName.lowercase()} moved to Hidden Vault")
            } else {
                showTemporaryMessage("${app.displayName.lowercase()} restored from Hidden Vault")
            }
        }
    }

    fun setTextAlignment(option: TextAlignmentOption) {
        viewModelScope.launch {
            preferencesRepository.setTextAlignment(option)
        }
    }

    fun setTypographyStyle(option: TypographyStyleOption) {
        viewModelScope.launch {
            preferencesRepository.setTypographyStyle(option)
        }
    }

    fun setThemeOption(option: LauncherThemeOption) {
        viewModelScope.launch {
            preferencesRepository.setThemeOption(option)
        }
    }

    fun toggleContrastThemeOnDoubleTap() {
        viewModelScope.launch {
            preferencesRepository.toggleQuickContrastTheme()
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            preferencesRepository.setFontScale(scale)
        }
    }

    fun setAutoLaunchSingleResult(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoLaunchSingleResult(enabled)
        }
    }

    fun setEnableT9Search(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setEnableT9Search(enabled)
        }
    }

    fun setFrictionSeconds(seconds: Int) {
        viewModelScope.launch {
            preferencesRepository.setFrictionSeconds(seconds)
        }
    }

    fun setMindfulFrictionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMindfulFrictionEnabled(enabled)
        }
    }

    fun setVaultPasscode(passcode: String) {
        viewModelScope.launch {
            preferencesRepository.setVaultPasscode(passcode)
        }
    }

    fun setStatusBarStealth(stealth: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setStatusBarStealth(stealth)
        }
    }

    fun setBottomDockedSearch(bottom: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBottomDockedSearch(bottom)
        }
    }

    fun setSwipeDownForNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSwipeDownForNotifications(enabled)
        }
    }

    fun setOmniSearchToolsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setOmniSearchToolsEnabled(enabled)
        }
    }

    fun setAutoDayNightTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoDayNightTheme(enabled)
        }
    }

    fun syncWallpapersWithCurrentTheme() {
        viewModelScope.launch {
            val isLight = effectiveTheme.value == LauncherThemeOption.WARM_STONE
            val ok = WallpaperHelper.setSolidThemeWallpaper(appContext, isLight)
            if (ok) {
                showTemporaryMessage("System & Lock wallpapers synced to minimalist theme")
            } else {
                showTemporaryMessage("Wallpaper permission required or not supported")
            }
        }
    }

    fun nextWhisper() {
        viewModelScope.launch {
            whisperEngine.refreshWhisper()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun showTemporaryMessage(msg: String) {
        _statusMessage.value = msg
    }
}
