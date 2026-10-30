package com.productiveos.launcher.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productiveos.launcher.presentation.components.AppActionBottomSheet
import com.productiveos.launcher.presentation.components.SwapPinnedAppBottomSheet
import com.productiveos.launcher.presentation.screens.MindfulFrictionOverlay
import com.productiveos.launcher.presentation.screens.SearchDrawerScreen
import com.productiveos.launcher.presentation.screens.SettingsScreen
import com.productiveos.launcher.presentation.screens.ZenHomeScreen
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.ProductiveOSTheme
import com.productiveos.launcher.util.SystemLauncherHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val dynamicPackageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refreshAllState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )

        // Override physical/gesture Back button so pressing Back on the Home Screen never exits the launcher
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.frictionTargetApp.value != null) {
                        viewModel.dismissFrictionOverlay()
                    } else if (viewModel.currentScreen.value != LauncherScreen.ZEN_HOME) {
                        viewModel.returnToHome()
                    }
                    // When already on ZEN_HOME, consume back press intentionally (do nothing)
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
        registerReceiver(dynamicPackageReceiver, pkgFilter)

        setContent {
            val context = LocalContext.current
            val settings by viewModel.settingsState.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val pinnedApps by viewModel.pinnedApps.collectAsStateWithLifecycle()
            val visibleApps by viewModel.visibleApps.collectAsStateWithLifecycle()
            val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
            val filteredApps by viewModel.filteredSearchApps.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val telemetry by viewModel.dailyTelemetry.collectAsStateWithLifecycle()
            val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()
            val frictionTargetApp by viewModel.frictionTargetApp.collectAsStateWithLifecycle()
            val selectedContextApp by viewModel.selectedContextApp.collectAsStateWithLifecycle()
            val swapTargetApp by viewModel.swapTargetApp.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

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
                themeOption = settings.themeOption,
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
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "LauncherScreenTransition"
                    ) { screen ->
                        when (screen) {
                            LauncherScreen.ZEN_HOME -> {
                                ZenHomeScreen(
                                    pinnedApps = pinnedApps,
                                    telemetry = telemetry,
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
                                    onAppClick = { app ->
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.onAppRequested(app) { pkg ->
                                            SystemLauncherHelper.launchPackage(context, pkg)
                                        }
                                    },
                                    onAppLongClick = { app ->
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.openAppContextSheet(app)
                                    },
                                    onSwipeUpSearch = {
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.openSearchDrawer()
                                    },
                                    onSwipeDownNotifications = {
                                        SystemLauncherHelper.expandNotificationShade(context)
                                    },
                                    onDoubleTapEmptySpace = {
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.toggleContrastThemeOnDoubleTap()
                                    },
                                    onLongPressEmptySpace = {
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.openSettingsScreen()
                                    }
                                )
                            }

                            LauncherScreen.SEARCH_DRAWER -> {
                                SearchDrawerScreen(
                                    query = searchQuery,
                                    filteredApps = filteredApps,
                                    settings = settings,
                                    onQueryChange = { newQuery ->
                                        viewModel.updateSearchQuery(newQuery) { matchedApp ->
                                            SystemLauncherHelper.performSubtleHaptic(context)
                                            viewModel.onAppRequested(matchedApp) { pkg ->
                                                SystemLauncherHelper.launchPackage(context, pkg)
                                            }
                                        }
                                    },
                                    onAppClick = { app ->
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.onAppRequested(app) { pkg ->
                                            SystemLauncherHelper.launchPackage(context, pkg)
                                        }
                                    },
                                    onAppLongClick = { app ->
                                        SystemLauncherHelper.performSubtleHaptic(context)
                                        viewModel.openAppContextSheet(app)
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
                                    }
                                )
                            }
                        }
                    }

                    // Screen 3: Mindful Friction Interstitial Overlay
                    val activeFrictionApp = frictionTargetApp
                    if (activeFrictionApp != null) {
                        MindfulFrictionOverlay(
                            targetApp = activeFrictionApp,
                            totalSeconds = settings.frictionSeconds,
                            onCancelAndReturnHome = {
                                SystemLauncherHelper.performSubtleHaptic(context)
                                viewModel.dismissFrictionOverlay()
                            },
                            onContinueToApp = {
                                SystemLauncherHelper.performSubtleHaptic(context)
                                viewModel.confirmAndLaunchApp(activeFrictionApp) { pkg ->
                                    SystemLauncherHelper.launchPackage(context, pkg)
                                }
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

