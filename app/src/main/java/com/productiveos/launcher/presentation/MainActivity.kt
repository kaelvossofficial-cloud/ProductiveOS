package com.productiveos.launcher.presentation

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productiveos.launcher.data.preferences.LauncherThemeOption
import com.productiveos.launcher.data.repository.WhisperItem
import com.productiveos.launcher.data.service.ProductiveNotificationListenerService
import com.productiveos.launcher.presentation.components.AppActionBottomSheet
import com.productiveos.launcher.presentation.components.BurnerNoteOverlay
import com.productiveos.launcher.presentation.components.MicroLogJournalSheet
import com.productiveos.launcher.presentation.components.QuantumClipSheet
import com.productiveos.launcher.presentation.components.SwapPinnedAppBottomSheet
import com.productiveos.launcher.presentation.screens.MindfulFrictionOverlay
import com.productiveos.launcher.presentation.screens.ReportsDashboardScreen
import com.productiveos.launcher.presentation.screens.SearchDrawerScreen
import com.productiveos.launcher.presentation.screens.SettingsScreen
import com.productiveos.launcher.presentation.screens.WidgetGalleryScreen
import com.productiveos.launcher.presentation.screens.ZenHomeScreen
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.ProductiveOSTheme
import com.productiveos.launcher.util.OmniSearchResult
import com.productiveos.launcher.util.SystemLauncherHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private var backPressPagerHandler: (() -> Boolean)? = null

    private val dynamicPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refreshAllState()
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { appWidgetHost.stopListening() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, 1024)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )

        // Override physical/gesture Back button so pressing Back on the Home Screen never exits the launcher
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        viewModel.frictionTargetApp.value != null -> {
                            viewModel.dismissFrictionOverlay()
                        }
                        viewModel.isBurnerNoteOpen.value -> {
                            viewModel.closeBurnerNote()
                        }
                        viewModel.isQuantumClipOpen.value -> {
                            viewModel.closeQuantumClipVault()
                        }
                        viewModel.isMicroLogOpen.value -> {
                            viewModel.closeMicroLogJournal()
                        }
                        viewModel.currentScreen.value != LauncherScreen.ZEN_HOME -> {
                            viewModel.returnToHome()
                        }
                        backPressPagerHandler?.invoke() == true -> {
                            // Handled return to center home page
                        }
                    }
                }
            }
        )

        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        runCatching {
            ContextCompat.registerReceiver(
                this,
                dynamicPackageReceiver,
                pkgFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }

        setContent {
            val context = LocalContext.current
            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val effectiveTheme by viewModel.effectiveTheme.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val pinnedApps by viewModel.pinnedApps.collectAsStateWithLifecycle()
            val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()
            val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
            val filteredApps by viewModel.filteredSearchApps.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val telemetry by viewModel.dailyTelemetry.collectAsStateWithLifecycle()
            val whisperItem by viewModel.currentWhisper.collectAsStateWithLifecycle()
            val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()
            val frictionTargetApp by viewModel.frictionTargetApp.collectAsStateWithLifecycle()
            val selectedContextApp by viewModel.selectedContextApp.collectAsStateWithLifecycle()
            val swapTargetApp by viewModel.swapTargetApp.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
            val omniResult by viewModel.omniSearchResult.collectAsStateWithLifecycle()
            val activeFocusSound by viewModel.activeFocusSound.collectAsStateWithLifecycle()
            val recentClips by viewModel.recentClips.collectAsStateWithLifecycle()
            val microLogs by viewModel.microLogs.collectAsStateWithLifecycle()
            val weeklyAwareness by viewModel.weeklyAwareness.collectAsStateWithLifecycle()
            val isQuantumClipOpen by viewModel.isQuantumClipOpen.collectAsStateWithLifecycle()
            val quantumClipFilter by viewModel.quantumClipFilter.collectAsStateWithLifecycle()
            val isMicroLogOpen by viewModel.isMicroLogOpen.collectAsStateWithLifecycle()
            val isBurnerNoteOpen by viewModel.isBurnerNoteOpen.collectAsStateWithLifecycle()
            val usageReportData by viewModel.usageReportData.collectAsStateWithLifecycle()
            val reportPeriod by viewModel.reportPeriod.collectAsStateWithLifecycle()
            val pomodoroState by viewModel.pomodoroState.collectAsStateWithLifecycle()
            val importantNotifications by viewModel.importantNotifications.collectAsStateWithLifecycle()

            val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(pagerState.currentPage) {
                backPressPagerHandler = {
                    if (pagerState.currentPage != 1) {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        true
                    } else {
                        false
                    }
                }
                onDispose {
                    backPressPagerHandler = null
                }
            }

            val widgetPickLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val widgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                    if (widgetId != -1) {
                        viewModel.addInstalledAppWidgetId(widgetId)
                    }
                }
            }

            // BLOCK 1, ITEM 1: System Bar Contrast & Stealth Mode Fix
            LaunchedEffect(effectiveTheme, settings.statusBarStealth) {
                val isLight = effectiveTheme.isLight
                SystemLauncherHelper.applySystemBarContrast(window, isLight)
                SystemLauncherHelper.applyStatusBarVisibility(window, settings.statusBarStealth)
            }

            val roleRequestLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                viewModel.refreshAllState()
            }

            val requestDefaultHome: () -> Unit = {
                SystemLauncherHelper.performSubtleHaptic(context)
                runCatching {
                    val intent = SystemLauncherHelper.createRequestDefaultLauncherIntent(context)
                    roleRequestLauncher.launch(intent)
                }.onFailure {
                    SystemLauncherHelper.openHomeSettingsPicker(context)
                }
            }

            LaunchedEffect(statusMessage) {
                if (statusMessage != null) {
                    delay(2_600L)
                    viewModel.clearStatusMessage()
                }
            }

            ProductiveOSTheme(
                themeOption = effectiveTheme,
                typographyStyle = settings.typographyStyle,
                fontScale = settings.fontScale
            ) {
                val palette = LocalProductiveOSPalette.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.background)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    // BLOCK 4: Scale-95 & Custom CubicBezier Screen Transitions
                    val drawerCurve = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(
                                animationSpec = tween(
                                    durationMillis = 210,
                                    easing = drawerCurve
                                )
                            ) + scaleIn(
                                initialScale = 0.95f,
                                animationSpec = tween(
                                    durationMillis = 220,
                                    easing = drawerCurve
                                )
                            )) togetherWith (fadeOut(
                                animationSpec = tween(
                                    durationMillis = 160,
                                    easing = FastOutSlowInEasing
                                )
                            ) + scaleOut(
                                targetScale = 0.97f,
                                animationSpec = tween(
                                    durationMillis = 160,
                                    easing = FastOutSlowInEasing
                                )
                            ))
                        },
                        label = "LauncherScreenTransition"
                    ) { screen ->
                        when (screen) {
                            LauncherScreen.ZEN_HOME -> {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    when (page) {
                                        0 -> {
                                            // Swipe Right from Home reveals Mindful Reports Dashboard
                                            ReportsDashboardScreen(
                                                reportData = usageReportData,
                                                selectedPeriod = reportPeriod,
                                                onPeriodSelected = { viewModel.setReportPeriod(it) },
                                                onRequestUsagePermission = {
                                                    SystemLauncherHelper.openUsageAccessSettings(context)
                                                },
                                                onAppClick = { pkg ->
                                                    SystemLauncherHelper.performGlowAppOpenHaptic(context)
                                                    SystemLauncherHelper.launchPackage(context, pkg)
                                                },
                                                onBackToHome = {
                                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                                }
                                            )
                                        }
                                        1 -> {
                                            // Center Home Canvas
                                            ZenHomeScreen(
                                                pinnedApps = pinnedApps,
                                                telemetry = telemetry,
                                                whisperItem = whisperItem,
                                                activeFocusSound = activeFocusSound,
                                                settings = settings,
                                                isDefaultLauncher = isDefaultLauncher,
                                                onSetDefaultLauncherClick = requestDefaultHome,
                                                onClockClick = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    SystemLauncherHelper.openClockApp(context)
                                                },
                                                onDateClick = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    SystemLauncherHelper.openCalendarApp(context)
                                                },
                                                onTelemetryPillClick = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    if (!telemetry.hasPermission) {
                                                        SystemLauncherHelper.openUsageAccessSettings(context)
                                                    } else {
                                                        viewModel.refreshAllState()
                                                    }
                                                },
                                                onWhisperClick = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    when (whisperItem) {
                                                        is WhisperItem.MediaNowPlaying -> {
                                                            SystemLauncherHelper.toggleMediaPlayback(context)
                                                        }
                                                        is WhisperItem.CalendarEvent -> {
                                                            SystemLauncherHelper.openCalendarApp(context)
                                                        }
                                                        is WhisperItem.BatteryStatus -> {
                                                            runCatching {
                                                                context.startActivity(
                                                                    Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                    }
                                                                )
                                                            }.onFailure { viewModel.nextWhisper() }
                                                        }
                                                        else -> {
                                                            viewModel.nextWhisper()
                                                        }
                                                    }
                                                },
                                                onToggleFocusSound = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    viewModel.triggerFocusSound(null)
                                                },
                                                onOpenBurnerNote = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    viewModel.openBurnerNote()
                                                },
                                                onOpenQuantumClip = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    viewModel.openQuantumClipVault()
                                                },
                                                onAppClick = { app ->
                                                    SystemLauncherHelper.performGlowAppOpenHaptic(context)
                                                    viewModel.onAppRequested(app) { pkg ->
                                                        SystemLauncherHelper.launchPackage(context, pkg)
                                                    }
                                                },
                                                onAppLongClick = { app ->
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    viewModel.openAppContextSheet(app)
                                                },
                                                onAppSwipeRight = { app ->
                                                    val targetPkg = app.swipeActionPackage
                                                    if (targetPkg != null) {
                                                        SystemLauncherHelper.performGlowAppOpenHaptic(context)
                                                        SystemLauncherHelper.launchPackage(context, targetPkg)
                                                    } else {
                                                        when {
                                                            app.displayName.contains("message", ignoreCase = true) ||
                                                                app.label.contains("message", ignoreCase = true) -> {
                                                                runCatching {
                                                                    context.startActivity(
                                                                        Intent(Intent.ACTION_DIAL).apply {
                                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            app.displayName.contains("phone", ignoreCase = true) ||
                                                                app.label.contains("phone", ignoreCase = true) -> {
                                                                runCatching {
                                                                    context.startActivity(
                                                                        Intent(Intent.ACTION_MAIN).apply {
                                                                            addCategory(Intent.CATEGORY_APP_MESSAGING)
                                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            app.displayName.contains("camera", ignoreCase = true) ||
                                                                app.label.contains("camera", ignoreCase = true) -> {
                                                                runCatching {
                                                                    context.startActivity(
                                                                        Intent(Intent.ACTION_VIEW).apply {
                                                                            type = "image/*"
                                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                            else -> {
                                                                SystemLauncherHelper.openAppInfo(context, app.packageName)
                                                            }
                                                        }
                                                    }
                                                },
                                                onSwipeUpSearch = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    viewModel.openSearchDrawer()
                                                },
                                                onSwipeDownNotifications = {
                                                    SystemLauncherHelper.expandNotificationShade(context)
                                                },
                                                onCanvasSwipeLeft = {
                                                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                                },
                                                onCanvasSwipeRight = {
                                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                                },
                                                onDoubleTapEmptySpace = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    if (settings.doubleTapToLock) {
                                                        SystemLauncherHelper.lockScreen(context)
                                                    } else {
                                                        viewModel.toggleContrastThemeOnDoubleTap()
                                                    }
                                                },
                                                onLongPressEmptySpace = {
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    viewModel.openSettingsScreen()
                                                },
                                                onHomeEdgeLetterSelected = { letter ->
                                                    SystemLauncherHelper.performSubtleHaptic(context)
                                                    viewModel.openSearchDrawer()
                                                    if (letter != "#") {
                                                        viewModel.updateSearchQuery(letter) {}
                                                    }
                                                }
                                            )
                                        }
                                        2 -> {
                                            // Swipe Left from Home reveals Widget Gallery
                                            WidgetGalleryScreen(
                                                telemetry = telemetry,
                                                reportData = usageReportData,
                                                importantNotifications = importantNotifications,
                                                pomodoroState = pomodoroState,
                                                activeFocusSound = activeFocusSound,
                                                installedWidgetIds = settings.installedWidgetIds,
                                                appWidgetHost = appWidgetHost,
                                                appWidgetManager = appWidgetManager,
                                                onStartPomodoro = { viewModel.startPomodoro() },
                                                onPausePomodoro = { viewModel.pausePomodoro() },
                                                onResetPomodoro = { viewModel.resetPomodoro() },
                                                onSwitchPomodoroMode = { viewModel.switchPomodoroMode(it) },
                                                onToggleFocusSound = { viewModel.triggerFocusSound(null) },
                                                onAppClick = { pkg ->
                                                    SystemLauncherHelper.performGlowAppOpenHaptic(context)
                                                    SystemLauncherHelper.launchPackage(context, pkg)
                                                },
                                                onRequestNotificationAccess = {
                                                    ProductiveNotificationListenerService.openNotificationAccessSettings(context)
                                                },
                                                onRequestUsageAccess = {
                                                    SystemLauncherHelper.openUsageAccessSettings(context)
                                                },
                                                onAddCustomWidgetClick = {
                                                    runCatching {
                                                        val newId = appWidgetHost.allocateAppWidgetId()
                                                        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                                                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                                                        }
                                                        widgetPickLauncher.launch(pickIntent)
                                                    }
                                                },
                                                onRemoveCustomWidgetClick = { id ->
                                                    runCatching { appWidgetHost.deleteAppWidgetId(id) }
                                                    viewModel.removeInstalledAppWidgetId(id)
                                                },
                                                onBackToHome = {
                                                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            LauncherScreen.SEARCH_DRAWER -> {
                                SearchDrawerScreen(
                                    query = searchQuery,
                                    filteredApps = filteredApps,
                                    omniResult = omniResult,
                                    settings = settings,
                                    onBrowserEngineSearch = { query, engine ->
                                        SystemLauncherHelper.searchBrowserEngine(context, query, engine)
                                        viewModel.returnToHome()
                                    },
                                    onQueryChange = { newQuery ->
                                        viewModel.updateSearchQuery(newQuery) { matchedApp ->
                                            SystemLauncherHelper.performGlowAppOpenHaptic(context)
                                            viewModel.onAppRequested(matchedApp) { pkg ->
                                                SystemLauncherHelper.launchPackage(context, pkg)
                                            }
                                        }
                                    },
                                    onAppClick = { app ->
                                        SystemLauncherHelper.performGlowAppOpenHaptic(context)
                                        viewModel.onAppRequested(app) { pkg ->
                                            SystemLauncherHelper.launchPackage(context, pkg)
                                        }
                                    },
                                    onAppLongClick = { app ->
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.openAppContextSheet(app)
                                    },
                                    onOmniClick = { result ->
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        when (result) {
                                            is OmniSearchResult.MathResult -> {
                                                SystemLauncherHelper.copyToClipboard(
                                                    context,
                                                    "Calculator Result",
                                                    result.copyText
                                                )
                                                viewModel.returnToHome()
                                            }
                                            is OmniSearchResult.WebSearch -> {
                                                SystemLauncherHelper.searchWeb(context, result.query)
                                                viewModel.returnToHome()
                                            }
                                            is OmniSearchResult.MapsSearch -> {
                                                SystemLauncherHelper.openMapsSearch(context, result.query)
                                                viewModel.returnToHome()
                                            }
                                            is OmniSearchResult.TimerCommand -> {
                                                SystemLauncherHelper.startNativeTimer(context, result.minutes)
                                                viewModel.returnToHome()
                                            }
                                            is OmniSearchResult.QuantumClipCommand -> {
                                                viewModel.openQuantumClipVault(result.filterQuery)
                                            }
                                            is OmniSearchResult.FocusAudioCommand -> {
                                                viewModel.triggerFocusSound(result.mode)
                                            }
                                            is OmniSearchResult.MicroLogCapture -> {
                                                viewModel.captureMicroLogFromOmni(result.thought)
                                            }
                                            is OmniSearchResult.OpenMicroLogVault -> {
                                                viewModel.openMicroLogJournal()
                                            }
                                            is OmniSearchResult.OpenBurnerNote -> {
                                                viewModel.openBurnerNote()
                                            }
                                        }
                                    },
                                    onCloseDrawer = {
                                        viewModel.returnToHome()
                                    },
                                    onOpenSettings = {
                                        viewModel.openSettingsScreen()
                                    }
                                )
                            }

                            LauncherScreen.SETTINGS -> {
                                SettingsScreen(
                                    settings = settings,
                                    isDefaultLauncher = isDefaultLauncher,
                                    pinnedApps = pinnedApps,
                                    hiddenApps = hiddenApps,
                                    weeklyAwareness = weeklyAwareness,
                                    activeFocusSound = activeFocusSound,
                                    onBackToHome = { viewModel.returnToHome() },
                                    onSetDefaultLauncher = requestDefaultHome,
                                    onSwitchSystemLauncher = {
                                        SystemLauncherHelper.openHomeSettingsPicker(context)
                                    },
                                    onTextAlignmentSelected = { viewModel.setTextAlignment(it) },
                                    onTypographySelected = { viewModel.setTypographyStyle(it) },
                                    onThemeSelected = { viewModel.setThemeOption(it) },
                                    onFontScaleChanged = { viewModel.setFontScale(it) },
                                    onAutoLaunchChanged = { viewModel.setAutoLaunchSingleResult(it) },
                                    onT9SearchChanged = { viewModel.setEnableT9Search(it) },
                                    onFrictionSecondsChanged = { viewModel.setFrictionSeconds(it) },
                                    onMindfulFrictionEnabledChanged = { viewModel.setMindfulFrictionEnabled(it) },
                                    onHoldToOpenChanged = { viewModel.setEnableHoldToOpen(it) },
                                    onHoldSecondsChanged = { viewModel.setHoldToOpenSeconds(it) },
                                    onRequestNotificationAccess = {
                                        ProductiveNotificationListenerService.openNotificationAccessSettings(context)
                                    },
                                    onStatusBarStealthChanged = { viewModel.setStatusBarStealth(it) },
                                    onBottomDockedSearchChanged = { viewModel.setBottomDockedSearch(it) },
                                    onSwipeDownNotificationsChanged = { viewModel.setSwipeDownForNotifications(it) },
                                    onOmniSearchToolsChanged = { viewModel.setOmniSearchToolsEnabled(it) },
                                    onAutoDayNightThemeChanged = { viewModel.setAutoDayNightTheme(it) },
                                    onSyncWallpaperClicked = { viewModel.syncWallpapersWithCurrentTheme() },
                                    onOpenQuantumClip = { viewModel.openQuantumClipVault() },
                                    onOpenMicroLog = { viewModel.openMicroLogJournal() },
                                    onOpenBurnerNote = { viewModel.openBurnerNote() },
                                    onStartFocusSound = { mode -> viewModel.triggerFocusSound(mode) },
                                    onMovePinnedApp = { app, moveUp ->
                                        viewModel.movePinnedOrder(app, moveUp)
                                    },
                                    onUnpinApp = { app ->
                                        viewModel.togglePin(app)
                                    },
                                    onUnhideApp = { app ->
                                        viewModel.setHidden(app, false)
                                    },
                                    onLaunchHiddenApp = { app ->
                                        viewModel.onAppRequested(app) { pkg ->
                                            SystemLauncherHelper.launchPackage(context, pkg)
                                        }
                                    },
                                    onUpdateVaultPasscode = { passcode ->
                                        viewModel.setVaultPasscode(passcode)
                                    },
                                    onHomeToggleChanged = { key, value ->
                                        viewModel.setHomeToggle(key, value)
                                    }
                                )
                            }
                        }
                    }

                    // Screen 3: Mindful Friction Interstitial Overlay (with PathCheck Intent Trigger)
                    val activeFrictionApp = frictionTargetApp
                    if (activeFrictionApp != null) {
                        MindfulFrictionOverlay(
                            targetApp = activeFrictionApp,
                            totalSeconds = settings.frictionSeconds,
                            onCancelAndReturnHome = { triggerReason ->
                                SystemLauncherHelper.performSubtleHaptic(context)
                                viewModel.dismissFrictionOverlay(triggerReason)
                            },
                            onContinueToApp = { triggerReason ->
                                SystemLauncherHelper.performGlowAppOpenHaptic(context)
                                viewModel.confirmAndLaunchApp(activeFrictionApp, triggerReason) { pkg ->
                                    SystemLauncherHelper.launchPackage(context, pkg)
                                }
                            }
                        )
                    }

                    // Micro-App 1: Quantum Clip (Zero-Click Clipboard Vault)
                    if (isQuantumClipOpen) {
                        QuantumClipSheet(
                            clips = recentClips,
                            initialFilter = quantumClipFilter,
                            onDismiss = { viewModel.closeQuantumClipVault() },
                            onCopyClip = { content ->
                                SystemLauncherHelper.copyToClipboard(context, "Quantum Clip", content)
                                viewModel.closeQuantumClipVault()
                            },
                            onShareClip = { content ->
                                runCatching {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, content)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Share Clip").apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                            onDeleteClip = { id ->
                                viewModel.deleteClip(id)
                            },
                            onClearAll = {
                                viewModel.clearAllClips()
                            }
                        )
                    }

                    // Micro-App 3: Micro-Log (Second-Brain Capture & Inbox.md Journal)
                    if (isMicroLogOpen) {
                        MicroLogJournalSheet(
                            logs = microLogs,
                            onDismiss = { viewModel.closeMicroLogJournal() },
                            onAddLog = { thought ->
                                viewModel.captureMicroLogFromOmni(thought)
                            },
                            onCopyLog = { text ->
                                SystemLauncherHelper.copyToClipboard(context, "Micro-Log", text)
                            },
                            onDeleteLog = { id ->
                                viewModel.deleteMicroLog(id)
                            },
                            onExportMarkdown = {
                                viewModel.shareMicroLogMarkdown { mdContent ->
                                    runCatching {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/markdown"
                                            putExtra(Intent.EXTRA_SUBJECT, "ProductiveOS Inbox.md")
                                            putExtra(Intent.EXTRA_TEXT, mdContent)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Export Inbox.md").apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Micro-App 4: Burner Notes (Ephemeral 24-Hour Scratchpad)
                    if (isBurnerNoteOpen) {
                        BurnerNoteOverlay(
                            noteText = settings.burnerNoteText,
                            updatedAtMillis = settings.burnerNoteUpdatedAt,
                            onNoteChange = { newText ->
                                viewModel.updateBurnerNoteText(newText)
                            },
                            onCopyNote = { text ->
                                SystemLauncherHelper.copyToClipboard(context, "Burner Note", text)
                            },
                            onClearNote = {
                                viewModel.updateBurnerNoteText("")
                            },
                            onClose = {
                                viewModel.closeBurnerNote()
                            }
                        )
                    }

                    // Long-Press Contextual Action Sheet
                    val contextApp = selectedContextApp
                    if (contextApp != null) {
                        AppActionBottomSheet(
                            app = contextApp,
                            onDismiss = { viewModel.dismissAppContextSheet() },
                            onTogglePin = { viewModel.togglePin(contextApp) },
                            onSwapPinnedSlot = if (contextApp.isPinned) {
                                { viewModel.openSwapPinnedSheet(contextApp) }
                            } else {
                                null
                            },
                            onRenameApp = { newName ->
                                viewModel.renameAppLabel(contextApp, newName)
                            },
                            onToggleFriction = {
                                viewModel.toggleMindfulFriction(contextApp)
                            },
                            onHideApp = {
                                viewModel.setHidden(contextApp, true)
                            },
                            onAppInfo = {
                                SystemLauncherHelper.openAppInfo(context, contextApp.packageName)
                            },
                            onUninstall = {
                                SystemLauncherHelper.requestUninstallApp(
                                    context,
                                    contextApp.packageName
                                )
                            }
                        )
                    }

                    // Swap Pinned App Bottom Sheet
                    val currentSwapTarget = swapTargetApp
                    if (currentSwapTarget != null) {
                        SwapPinnedAppBottomSheet(
                            targetApp = currentSwapTarget,
                            candidateApps = visibleApps,
                            onDismiss = { viewModel.dismissSwapPinnedSheet() },
                            onSelectReplacement = { replacement ->
                                viewModel.confirmSwapPinnedApp(currentSwapTarget, replacement)
                            }
                        )
                    }

                    // Minimalist Toast / Status Pill
                    if (statusMessage != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
                                .clip(RoundedCornerShape(50))
                                .background(palette.surfaceElevated)
                                .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = statusMessage.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primaryText
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.returnToHome()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAllState()
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            unregisterReceiver(dynamicPackageReceiver)
        }
    }
}
