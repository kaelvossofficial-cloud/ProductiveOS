package com.productiveos.launcher.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherPreferencesRepository
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.LauncherThemeOption
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.preferences.TypographyStyleOption
import com.productiveos.launcher.data.repository.AppRepository
import com.productiveos.launcher.data.repository.DailyTelemetry
import com.productiveos.launcher.data.repository.UsageStatsHelper
import com.productiveos.launcher.util.SystemLauncherHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val usageStatsHelper: UsageStatsHelper
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

    private val _dailyTelemetry = MutableStateFlow(DailyTelemetry())
    val dailyTelemetry: StateFlow<DailyTelemetry> = _dailyTelemetry.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(true)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val settingsState: StateFlow<LauncherSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LauncherSettings()
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

    init {
        refreshAllState()
    }

    fun refreshAllState() {
        viewModelScope.launch {
            _isDefaultLauncher.value = SystemLauncherHelper.isDefaultHomeLauncher(appContext)
            appRepository.syncInstalledApps()
            _dailyTelemetry.value = usageStatsHelper.queryTodayTelemetry()
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
        _currentScreen.value = LauncherScreen.ZEN_HOME
        viewModelScope.launch {
            _isDefaultLauncher.value = SystemLauncherHelper.isDefaultHomeLauncher(appContext)
            _dailyTelemetry.value = usageStatsHelper.queryTodayTelemetry()
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

    fun onAppRequested(app: AppEntity, executeSystemLaunch: (String) -> Unit) {
        if (app.isFrictionEnabled) {
            _frictionTargetApp.value = app
        } else {
            confirmAndLaunchApp(app, executeSystemLaunch)
        }
    }

    fun confirmAndLaunchApp(app: AppEntity, executeSystemLaunch: (String) -> Unit) {
        _frictionTargetApp.value = null
        _selectedContextApp.value = null
        _searchQuery.value = ""
        _currentScreen.value = LauncherScreen.ZEN_HOME
        viewModelScope.launch {
            appRepository.recordAppLaunch(app.packageName)
        }
        executeSystemLaunch(app.packageName)
    }

    fun dismissFrictionOverlay() {
        _frictionTargetApp.value = null
        _currentScreen.value = LauncherScreen.ZEN_HOME
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
            showTemporaryMessage("Mindful friction $status for ${app.displayName.lowercase()}")
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

    fun setVaultPasscode(passcode: String) {
        viewModelScope.launch {
            preferencesRepository.setVaultPasscode(passcode)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun showTemporaryMessage(msg: String) {
        _statusMessage.value = msg
    }
}

