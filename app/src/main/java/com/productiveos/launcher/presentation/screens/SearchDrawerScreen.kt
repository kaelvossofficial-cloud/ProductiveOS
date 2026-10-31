package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.presentation.components.InteractiveGridCanvas
import com.productiveos.launcher.presentation.components.WaveSidebarScrubber
import com.productiveos.launcher.presentation.theme.DoubleBezelSquircle
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.SquircleShape
import com.productiveos.launcher.presentation.theme.jellyPress
import com.productiveos.launcher.presentation.theme.tactilePress
import com.productiveos.launcher.util.OmniSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val StaggerEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun SearchDrawerScreen(
    query: String,
    filteredApps: List<AppEntity>,
    omniResult: OmniSearchResult?,
    settings: LauncherSettings,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppEntity) -> Unit,
    onAppLongClick: (AppEntity) -> Unit,
    onOmniClick: (OmniSearchResult) -> Unit,
    onBrowserEngineSearch: (query: String, engine: String) -> Unit = { _, _ -> },
    onCloseDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var isEntered by remember { mutableStateOf(false) }
    var activeTouchPoint by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        isEntered = true
        delay(50)
        runCatching {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val textAlign = when (settings.textAlignment) {
        TextAlignmentOption.LEFT -> TextAlign.Start
        TextAlignmentOption.CENTER -> TextAlign.Center
        TextAlignmentOption.RIGHT -> TextAlign.End
    }

    val searchBarContent = @Composable {
        var searchSwipeOffset by remember { mutableFloatStateOf(0f) }
        DoubleBezelSquircle(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (searchSwipeOffset < -90f) {
                                onOmniClick(OmniSearchResult.QuantumClipCommand(filterQuery = "", label = "quantum clip"))
                            }
                            searchSwipeOffset = 0f
                        },
                        onDragCancel = { searchSwipeOffset = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            searchSwipeOffset += dragAmount
                        }
                    )
                },
            outerRadius = 18.dp,
            bezelThickness = 1.5.dp,
            innerPaddingHorizontal = 16.dp,
            innerPaddingVertical = 13.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "type app, c/ clips, log, focus...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.mutedText
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.primaryText),
                        cursorBrush = SolidColor(palette.primaryText),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (omniResult != null) {
                                    onOmniClick(omniResult)
                                } else if (query.isNotBlank()) {
                                    onBrowserEngineSearch(query.trim(), "google")
                                } else if (filteredApps.isNotEmpty()) {
                                    onAppClick(filteredApps.first())
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (query.isNotEmpty()) {
                    Text(
                        text = "clear",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .jellyPress(0.95f)
                            .clickable { onQueryChange("") }
                    )
                } else {
                    Text(
                        text = "close",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .jellyPress(0.95f)
                            .clickable(onClick = onCloseDrawer)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activeChange = event.changes.firstOrNull { it.pressed }
                        activeTouchPoint = activeChange?.position
                        if (event.changes.none { it.pressed }) {
                            break
                        }
                    }
                    activeTouchPoint = null
                }
            }
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        if (settings.showInteractiveGrid) {
            InteractiveGridCanvas(
                touchPoint = activeTouchPoint
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 6.dp, top = 18.dp, bottom = 14.dp)
        ) {
            if (!settings.bottomDockedSearch) {
                searchBarContent()
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Browser Search Recommendations (Google, YouTube, Wikipedia, DuckDuckGo)
            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val searchEngines = listOf(
                        "google" to "Google",
                        "youtube" to "YouTube",
                        "wikipedia" to "Wiki",
                        "duckduckgo" to "DuckDuckGo"
                    )
                    val engineSquircle = remember { SquircleShape(10.dp) }
                    searchEngines.forEach { (engineKey, engineLabel) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .jellyPress(0.95f)
                                .clip(engineSquircle)
                                .background(palette.surfaceElevated)
                                .border(0.75.dp, palette.borderSubtle, engineSquircle)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBrowserEngineSearch(query.trim(), engineKey)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = engineLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = palette.primaryText
                            )
                        }
                    }
                }
            }

            // Omni-Search Math & Bang Command Pill (Double-Bezel)
            AnimatedVisibility(
                visible = omniResult != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (omniResult != null) {
                    val label = when (omniResult) {
                        is OmniSearchResult.MathResult -> omniResult.resultFormatted
                        is OmniSearchResult.WebSearch -> omniResult.label
                        is OmniSearchResult.MapsSearch -> omniResult.label
                        is OmniSearchResult.TimerCommand -> omniResult.label
                        is OmniSearchResult.QuantumClipCommand -> omniResult.label
                        is OmniSearchResult.FocusAudioCommand -> omniResult.label
                        is OmniSearchResult.MicroLogCapture -> omniResult.label
                        is OmniSearchResult.OpenMicroLogVault -> omniResult.label
                        is OmniSearchResult.OpenBurnerNote -> omniResult.label
                    }

                    DoubleBezelSquircle(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp, end = 16.dp),
                        outerRadius = 16.dp,
                        bezelThickness = 1.5.dp,
                        innerPaddingHorizontal = 16.dp,
                        innerPaddingVertical = 12.dp,
                        elevated = true,
                        onClick = { onOmniClick(omniResult) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.primaryText
                        )
                    }
                }
            }

            // Quick App Count & Status Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredApps.size} apps" +
                        if (settings.autoLaunchSingleResult) " · instant auto-launch" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText
                )

                Text(
                    text = "vault & settings →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    modifier = Modifier
                        .jellyPress(0.95f)
                        .clickable(onClick = onOpenSettings)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = palette.borderSubtle,
                modifier = Modifier.padding(end = 22.dp)
            )

            // App Directory with Right-Edge Gaussian Sine-Wave Scrubber
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isEmpty()) "loading apps..." else "no matching apps found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.mutedText
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = 6.dp)
                    ) {
                        itemsIndexed(
                            items = filteredApps,
                            key = { _, app -> app.packageName }
                        ) { index, app ->
                            // Staggered waterfall entrance for first 12 visible rows
                            val staggerDelay = (index.coerceAtMost(12)) * 28
                            val rowEntrance by animateFloatAsState(
                                targetValue = if (isEntered) 1f else 0f,
                                animationSpec = tween(
                                    durationMillis = 240,
                                    delayMillis = staggerDelay,
                                    easing = StaggerEaseOut
                                ),
                                label = "StaggerAppRow"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = rowEntrance
                                        translationY = (1f - rowEntrance) * 18.dp.toPx()
                                    }
                                    .tactilePress(
                                        minTouchHeight = true,
                                        enableGlow = true,
                                        onClick = { onAppClick(app) },
                                        onLongClick = { onAppLongClick(app) }
                                    )
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = app.displayName.lowercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = palette.primaryText,
                                    textAlign = textAlign,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (app.isPinned) {
                                        Text(
                                            text = "pinned",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.mutedText
                                        )
                                    }
                                    if (app.isFrictionEnabled) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(palette.secondaryText)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.35f))
                        }
                    }

                    // Right-Edge Gaussian Sine-Wave Sidebar Scrubber
                    WaveSidebarScrubber(
                        compactIdleMode = false,
                        onLetterScrubbed = { letter ->
                            val targetIdx = filteredApps.indexOfFirst {
                                if (letter == "#") !it.displayName.first().isLetter()
                                else it.displayName.startsWith(letter, ignoreCase = true)
                            }
                            if (targetIdx >= 0) {
                                coroutineScope.launch { listState.scrollToItem(targetIdx) }
                            }
                        }
                    )
                }
            }

            if (settings.bottomDockedSearch) {
                Spacer(modifier = Modifier.height(10.dp))
                searchBarContent()
            }
        }
    }
}
