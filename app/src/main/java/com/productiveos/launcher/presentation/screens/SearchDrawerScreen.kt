package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
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
    onOpenSettings: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(60)
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

    val alphabet = remember { ('a'..'z').map { it.toString() } + "#" }
    var activeScrubLetter by remember { mutableStateOf<String?>(null) }
    var scrubFingerY by remember { mutableFloatStateOf(0f) }
    var scrubberHeightPx by remember { mutableFloatStateOf(1f) }

    val searchBarContent = @Composable {
        var searchSwipeOffset by remember { mutableFloatStateOf(0f) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
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
                }
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        .clickable { onQueryChange("") }
                )
            } else {
                Text(
                    text = "close",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable(onClick = onCloseDrawer)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.ime)
            .padding(start = 24.dp, end = 8.dp, top = 18.dp, bottom = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    searchEngines.forEach { (engineKey, engineLabel) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surfaceElevated)
                                .border(1.dp, palette.borderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBrowserEngineSearch(query.trim(), engineKey)
                                }
                                .padding(vertical = 7.dp),
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

            // Omni-Search Math & Bang Command Pill
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp, end = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surfaceElevated)
                            .border(1.dp, palette.primaryText.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { onOmniClick(omniResult) }
                            .padding(horizontal = 16.dp, vertical = 13.dp)
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
                    .padding(end = 24.dp),
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
                    modifier = Modifier.clickable(onClick = onOpenSettings)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = palette.borderSubtle,
                modifier = Modifier.padding(end = 24.dp)
            )

            // App Directory with Right-Edge Alphabet Wave Scrubber
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

                // BLOCK 1, ITEM 3: Right-Edge Alphabet Wave Scrubber Rail
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords ->
                            scrubberHeightPx = coords.size.height.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(filteredApps) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    scrubFingerY = offset.y
                                    val frac = (offset.y / scrubberHeightPx).coerceIn(0f, 0.999f)
                                    val index = (frac * alphabet.size).toInt()
                                    val letter = alphabet[index]
                                    activeScrubLetter = letter
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                    val targetIdx = filteredApps.indexOfFirst {
                                        if (letter == "#") !it.displayName.first().isLetter()
                                        else it.displayName.startsWith(letter, ignoreCase = true)
                                    }
                                    if (targetIdx >= 0) {
                                        coroutineScope.launch { listState.scrollToItem(targetIdx) }
                                    }
                                },
                                onDragEnd = {
                                    activeScrubLetter = null
                                },
                                onDragCancel = {
                                    activeScrubLetter = null
                                },
                                onDrag = { change, _ ->
                                    scrubFingerY = change.position.y
                                    val frac = (change.position.y / scrubberHeightPx).coerceIn(0f, 0.999f)
                                    val index = (frac * alphabet.size).toInt()
                                    val letter = alphabet[index]
                                    if (activeScrubLetter != letter) {
                                        activeScrubLetter = letter
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                        val targetIdx = filteredApps.indexOfFirst {
                                            if (letter == "#") !it.displayName.first().isLetter()
                                            else it.displayName.startsWith(letter, ignoreCase = true)
                                        }
                                        if (targetIdx >= 0) {
                                            coroutineScope.launch { listState.scrollToItem(targetIdx) }
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        alphabet.forEach { letter ->
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                                color = if (activeScrubLetter == letter) palette.primaryText else palette.mutedText
                            )
                        }
                    }
                }
            }
            }

            if (settings.bottomDockedSearch) {
                Spacer(modifier = Modifier.height(10.dp))
                searchBarContent()
            }
        }

        // Active projected letter indicator at horizontal offset from finger
        if (activeScrubLetter != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = -72.dp.roundToPx(),
                            y = (scrubFingerY - 24.dp.roundToPx()).toInt().coerceAtLeast(0)
                        )
                    }
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(palette.surfaceElevated)
                    .border(1.5.dp, palette.primaryText, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeScrubLetter.orEmpty().uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 32.sp
                    ),
                    color = palette.primaryText
                )
            }
        }
    }
}
