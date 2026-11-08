package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.IconColorMode
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.presentation.components.InteractiveGridCanvas
import com.productiveos.launcher.presentation.components.ShapedAppIcon
import com.productiveos.launcher.presentation.components.SnappyEaseOut
import com.productiveos.launcher.presentation.components.WaveSidebarScrubber
import com.productiveos.launcher.presentation.components.rememberTilt
import com.productiveos.launcher.presentation.theme.DoubleBezelSquircle
import com.productiveos.launcher.presentation.theme.GoogleShape
import com.productiveos.launcher.presentation.theme.LiquidGlassCard
import com.productiveos.launcher.presentation.theme.LiquidGlassPill
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.SquircleShape
import com.productiveos.launcher.presentation.theme.jellyPress
import com.productiveos.launcher.presentation.theme.tactilePress
import com.productiveos.launcher.util.OmniSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onOpenSettings: () -> Unit,
    onHomeToggleChanged: (String, Boolean) -> Unit = { _, _ -> }
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val tilt = rememberTilt(enabled = settings.showInteractiveGrid)

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

    val entrance by animateFloatAsState(
        targetValue = if (isEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = SnappyEaseOut),
        label = "DrawerEntrance"
    )

    val textAlign = when (settings.textAlignment) {
        TextAlignmentOption.LEFT -> TextAlign.Start
        TextAlignmentOption.CENTER -> TextAlign.Center
        TextAlignmentOption.RIGHT -> TextAlign.End
    }

    val searchBarContent = @Composable {
        var searchSwipeOffset by remember { mutableFloatStateOf(0f) }
        LiquidGlassCard(
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
                        onHorizontalDrag = { _, dragAmount -> searchSwipeOffset += dragAmount }
                    )
                },
            cornerRadius = 22.dp,
            elevated = true,
            accentHighlight = query.isNotEmpty(),
            paddingHorizontal = 18.dp,
            paddingVertical = 14.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "⌕",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accentPrimary,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "search apps, math, c/ clips, log, focus...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.mutedText
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.primaryText),
                        cursorBrush = SolidColor(palette.accentPrimary),
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

                Text(
                    text = if (query.isNotEmpty()) "clear" else "close",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.accentPrimary,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .jellyPress(0.95f)
                        .clickable {
                            if (query.isNotEmpty()) onQueryChange("") else onCloseDrawer()
                        }
                )
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
                        val active = event.changes.firstOrNull { it.pressed }
                        activeTouchPoint = active?.position
                        if (active == null) break
                    }
                    activeTouchPoint = null
                }
            }
            .windowInsetsPadding(WindowInsets.ime)
    ) {
        if (settings.showInteractiveGrid) {
            InteractiveGridCanvas(
                touchProvider = { activeTouchPoint },
                tilt = tilt
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 6.dp, top = 16.dp, bottom = 14.dp)
        ) {
            if (!settings.bottomDockedSearch) {
                searchBarContent()
                Spacer(modifier = Modifier.height(10.dp))
            }

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
                    val engineShape = remember { SquircleShape(12.dp) }
                    searchEngines.forEach { (engineKey, engineLabel) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .jellyPress(0.95f)
                                .clip(engineShape)
                                .background(palette.glassSurfaceElevated)
                                .border(0.8.dp, palette.glassBorderTop, engineShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBrowserEngineSearch(query.trim(), engineKey)
                                }
                                .padding(vertical = 9.dp),
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

                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp, end = 16.dp),
                        cornerRadius = 18.dp,
                        elevated = true,
                        accentHighlight = true,
                        paddingHorizontal = 16.dp,
                        paddingVertical = 12.dp,
                        onClick = { onOmniClick(omniResult) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = palette.primaryText
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredApps.size} apps" +
                        if (settings.autoLaunchSingleResult) " · instant match" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassPill(
                        selected = settings.drawerGridMode,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onHomeToggleChanged("drawerGridMode", !settings.drawerGridMode)
                        }
                    ) {
                        Text(
                            text = if (settings.drawerGridMode) "≡ List" else "⊞ Grid",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = palette.primaryText
                        )
                    }
                    LiquidGlassPill(
                        selected = false,
                        onClick = onOpenSettings
                    ) {
                        Text(
                            text = "⚙ Control Center",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = palette.accentPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = palette.borderSubtle.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 18.dp)
            )

            val monochromeTint = if (settings.iconColorMode == IconColorMode.THEME_MONOCHROME) {
                palette.primaryText
            } else {
                null
            }

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
                        .graphicsLayer {
                            alpha = entrance
                            translationY = (1f - entrance) * 16.dp.toPx()
                        }
                ) {
                    if (settings.drawerGridMode) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            state = gridState,
                            contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp, end = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            items(items = filteredApps, key = { it.packageName }) { app ->
                                val shape = remember(app.packageName) {
                                    GoogleShape.forKey(app.packageName.hashCode())
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tactilePress(
                                            minTouchHeight = false,
                                            enableGlow = false,
                                            onClick = { onAppClick(app) },
                                            onLongClick = { onAppLongClick(app) }
                                        )
                                        .padding(vertical = 2.dp)
                                ) {
                                    ShapedAppIcon(
                                        packageName = app.packageName,
                                        size = 56.dp,
                                        shape = shape.shape,
                                        containerColor = palette.glassSurfaceElevated,
                                        monochromeTint = monochromeTint
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = app.displayName.lowercase(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                        color = palette.primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
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
                            ) { _, app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .tactilePress(
                                            minTouchHeight = true,
                                            enableGlow = true,
                                            onClick = { onAppClick(app) },
                                            onLongClick = { onAppLongClick(app) }
                                        )
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val rowShape = remember(app.packageName) {
                                        GoogleShape.forKey(app.packageName.hashCode())
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        ShapedAppIcon(
                                            packageName = app.packageName,
                                            size = 38.dp,
                                            shape = rowShape.shape,
                                            containerColor = palette.glassSurfaceElevated,
                                            monochromeTint = monochromeTint
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = app.displayName.lowercase(),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                                            color = palette.primaryText,
                                            textAlign = textAlign
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (app.isPinned) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(palette.accentPrimary.copy(alpha = 0.16f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "pinned",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                                                    color = palette.accentPrimary
                                                )
                                            }
                                        }
                                        if (app.isFrictionEnabled) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accentSecondary)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.3f))
                            }
                        }
                    }

                    WaveSidebarScrubber(
                        compactIdleMode = false,
                        onLetterScrubbed = { letter ->
                            val targetIdx = filteredApps.indexOfFirst {
                                if (letter == "#") !it.displayName.first().isLetter()
                                else it.displayName.startsWith(letter, ignoreCase = true)
                            }
                            if (targetIdx >= 0) {
                                coroutineScope.launch {
                                    if (settings.drawerGridMode) gridState.scrollToItem(targetIdx)
                                    else listState.scrollToItem(targetIdx)
                                }
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
