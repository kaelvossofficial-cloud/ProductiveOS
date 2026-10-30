package com.productiveos.launcher.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.data.repository.DailyTelemetry
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZenHomeScreen(
    pinnedApps: List<AppEntity>,
    telemetry: DailyTelemetry,
    settings: LauncherSettings,
    isDefaultLauncher: Boolean,
    onSetDefaultLauncherClick: () -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onTelemetryPillClick: () -> Unit,
    onAppClick: (AppEntity) -> Unit,
    onAppLongClick: (AppEntity) -> Unit,
    onSwipeUpSearch: () -> Unit,
    onSwipeDownNotifications: () -> Unit,
    onDoubleTapEmptySpace: () -> Unit,
    onLongPressEmptySpace: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            val delayMs = 60_000L - (nowMillis % 60_000L)
            delay(delayMs.coerceIn(1_000L, 60_000L))
        }
    }

    val timeFormatted = remember(nowMillis) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
    }
    val dateFormatted = remember(nowMillis) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
    }

    val horizontalAlignment = when (settings.textAlignment) {
        TextAlignmentOption.LEFT -> Alignment.Start
        TextAlignmentOption.CENTER -> Alignment.CenterHorizontally
        TextAlignmentOption.RIGHT -> Alignment.End
    }

    val textAlign = when (settings.textAlignment) {
        TextAlignmentOption.LEFT -> TextAlign.Start
        TextAlignmentOption.CENTER -> TextAlign.Center
        TextAlignmentOption.RIGHT -> TextAlign.End
    }

    var accumulatedDragY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTapEmptySpace() },
                    onLongPress = { onLongPressEmptySpace() }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDragY = 0f },
                    onDragEnd = {
                        if (accumulatedDragY < -90f) {
                            onSwipeUpSearch()
                        } else if (accumulatedDragY > 90f) {
                            onSwipeDownNotifications()
                        }
                        accumulatedDragY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        accumulatedDragY += dragAmount
                    }
                )
            }
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = horizontalAlignment
        ) {
            // Top Glance Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                if (!isDefaultLauncher) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceElevated)
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                            .clickable(onClick = onSetDefaultLauncherClick)
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = "Set ProductiveOS as Default Home →",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.primaryText,
                    textAlign = textAlign,
                    modifier = Modifier.clickable(onClick = onClockClick)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateFormatted.lowercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.secondaryText,
                    textAlign = textAlign,
                    modifier = Modifier.clickable(onClick = onDateClick)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Screen-Time & Focus Telemetry Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                        .clickable(onClick = onTelemetryPillClick)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(palette.accentDot)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = telemetry.formattedText,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }
            }

            // Center / Lower Third Pinned Essentials (4 to 6 Text Items)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = horizontalAlignment
            ) {
                if (pinnedApps.isEmpty()) {
                    Text(
                        text = "no pinned apps · swipe up to pin essentials",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedText,
                        textAlign = textAlign,
                        modifier = Modifier.clickable(onClick = onSwipeUpSearch)
                    )
                } else {
                    pinnedApps.take(6).forEach { app ->
                        Row(
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = app.displayName.lowercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = palette.primaryText,
                                textAlign = textAlign
                            )
                            if (app.isFrictionEnabled) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(palette.mutedText)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation / Search Trigger
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSwipeUpSearch)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "↑ swipe up or tap to search",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mutedText
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mutedText
                    )
                    Text(
                        text = "settings",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier.clickable(onClick = onLongPressEmptySpace)
                    )
                }
            }
        }
    }
}

