package com.productiveos.launcher.presentation.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.productiveos.launcher.data.repository.AppUsageItem
import com.productiveos.launcher.data.repository.DailyTelemetry
import com.productiveos.launcher.data.repository.DayUsagePoint
import com.productiveos.launcher.data.repository.UsageReportData
import com.productiveos.launcher.data.service.ImportantNotificationData
import com.productiveos.launcher.presentation.PomodoroMode
import com.productiveos.launcher.presentation.PomodoroState
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.util.FocusSoundMode

@Composable
fun WidgetGalleryScreen(
    telemetry: DailyTelemetry,
    reportData: UsageReportData,
    importantNotifications: ImportantNotificationData,
    pomodoroState: PomodoroState,
    activeFocusSound: FocusSoundMode?,
    installedWidgetIds: List<Int>,
    appWidgetHost: AppWidgetHost?,
    appWidgetManager: AppWidgetManager?,
    onStartPomodoro: () -> Unit,
    onPausePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSwitchPomodoroMode: (PomodoroMode) -> Unit,
    onToggleFocusSound: () -> Unit,
    onAppClick: (String) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onAddCustomWidgetClick: () -> Unit,
    onRemoveCustomWidgetClick: (Int) -> Unit,
    onBackToHome: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackToHome()
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "← home",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "productiveos",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText
                )
                Text(
                    text = "widget gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Screen Time Gauge Widget
        ScreenTimeGaugeWidget(
            telemetry = telemetry,
            onRequestUsageAccess = onRequestUsageAccess
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Interactive Pomodoro Focus Tracker Widget
        PomodoroFocusWidget(
            pomodoroState = pomodoroState,
            activeFocusSound = activeFocusSound,
            onStartPomodoro = onStartPomodoro,
            onPausePomodoro = onPausePomodoro,
            onResetPomodoro = onResetPomodoro,
            onSwitchPomodoroMode = onSwitchPomodoroMode,
            onToggleFocusSound = onToggleFocusSound
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Most Used Apps Widget (Dynamic bars)
        MostUsedAppsWidget(
            topApps = reportData.topApps,
            onAppClick = onAppClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Important Notifications Widget
        ImportantNotificationsWidget(
            notificationData = importantNotifications,
            onRequestNotificationAccess = onRequestNotificationAccess,
            onOpenNotificationApp = onAppClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Focus & Intentionality Graphs Widget
        FocusGraphsWidget(
            dayPoints = reportData.dailyPoints,
            mindfulScore = reportData.mindfulScore
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Third-Party Android App Widgets Section
        ThirdPartyWidgetsSection(
            widgetIds = installedWidgetIds,
            appWidgetHost = appWidgetHost,
            appWidgetManager = appWidgetManager,
            onAddWidgetClick = onAddCustomWidgetClick,
            onRemoveWidgetClick = onRemoveCustomWidgetClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ScreenTimeGaugeWidget(
    telemetry: DailyTelemetry,
    onRequestUsageAccess: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    val totalMins = telemetry.totalScreenTimeMinutes
    val targetMins = 240 // 4 hours limit
    val pct = (totalMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f)

    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "screenGaugeAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (!telemetry.hasPermission) onRequestUsageAccess()
            }
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "screen time ring",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                val hours = totalMins / 60
                val mins = totalMins % 60
                Text(
                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp, lineHeight = 32.sp),
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (telemetry.hasPermission) "${(pct * 100).toInt()}% of 4h mindful budget · ${telemetry.unlockCount} unlocks" else "tap to grant usage access →",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (telemetry.hasPermission) palette.mutedText else palette.primaryText
                )
            }

            Box(
                modifier = Modifier.size(68.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 6.dp.toPx()
                    val diameter = size.minDimension - stroke
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = palette.borderSubtle,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = palette.primaryText,
                        startAngle = -90f,
                        sweepAngle = animatedPct * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(pct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PomodoroFocusWidget(
    pomodoroState: PomodoroState,
    activeFocusSound: FocusSoundMode?,
    onStartPomodoro: () -> Unit,
    onPausePomodoro: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSwitchPomodoroMode: (PomodoroMode) -> Unit,
    onToggleFocusSound: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    val animProgress by animateFloatAsState(
        targetValue = pomodoroState.progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "pomoProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "pomodoro focus engine",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                if (pomodoroState.completedSessions > 0) {
                    Text(
                        text = "${pomodoroState.completedSessions} sessions done",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mutedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timer Display and Progress Arc
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = pomodoroState.formattedTime,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp, lineHeight = 46.sp),
                        color = palette.primaryText,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = if (pomodoroState.isRunning) "in flow state · stay detached" else "paused · ready to engage",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }

                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 5.dp.toPx()
                        val diameter = size.minDimension - stroke
                        val topLeft = Offset(stroke / 2f, stroke / 2f)
                        val arcSize = Size(diameter, diameter)

                        drawArc(
                            color = palette.borderSubtle,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = palette.primaryText,
                            startAngle = -90f,
                            sweepAngle = animProgress * 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${((1f - pomodoroState.progress) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = palette.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play / Pause button
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (pomodoroState.isRunning) palette.surfaceElevated else palette.primaryText)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (pomodoroState.isRunning) onPausePomodoro() else onStartPomodoro()
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (pomodoroState.isRunning) "pause" else "start focus",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (pomodoroState.isRunning) palette.primaryText else palette.background
                    )
                }

                // Reset button
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onResetPomodoro()
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "reset",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                }

                // Ambient Audio Shortcut
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceElevated)
                        .border(
                            1.dp,
                            if (activeFocusSound != null) palette.primaryText else palette.borderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onToggleFocusSound()
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeFocusSound != null) "ılı audio" else "+ audio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Duration Mode Switcher Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PomodoroMode.values().forEach { mode ->
                    val isSelected = mode == pomodoroState.mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) palette.surfaceElevated else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) palette.borderSubtle else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSwitchPomodoroMode(mode)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title.lowercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) palette.primaryText else palette.mutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MostUsedAppsWidget(
    topApps: List<AppUsageItem>,
    onAppClick: (String) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "most used apps",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (topApps.isEmpty()) {
                Text(
                    text = "usage stats gathering...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.mutedText
                )
            } else {
                topApps.take(4).forEach { app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAppClick(app.packageName)
                            }
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = app.appLabel.lowercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.primaryText
                            )
                            Text(
                                text = app.formattedDuration,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(palette.borderSubtle)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = app.percentage.coerceIn(0.04f, 1f))
                                    .background(palette.primaryText)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportantNotificationsWidget(
    notificationData: ImportantNotificationData,
    onRequestNotificationAccess: () -> Unit,
    onOpenNotificationApp: (String) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "important notifications",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Text(
                    text = "${notificationData.totalCount} unread",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!notificationData.hasPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRequestNotificationAccess()
                        }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Enable Notification Access →",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Filter messages, calls, and calendar alerts directly in the OS aesthetic.",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.mutedText
                        )
                    }
                }
            } else if (notificationData.recentNotifications.isEmpty()) {
                Text(
                    text = "all clear · zero urgent distractions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.mutedText
                )
            } else {
                notificationData.recentNotifications.take(3).forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenNotificationApp(item.packageName)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = item.title.ifBlank { "Notification" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = palette.primaryText
                        )
                        Text(
                            text = item.text.take(65),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusGraphsWidget(
    dayPoints: List<DayUsagePoint>,
    mindfulScore: Int
) {
    val palette = LocalProductiveOSPalette.current
    val maxMins = (dayPoints.maxOfOrNull { it.screenTimeMinutes } ?: 240).coerceAtLeast(180)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "focus graphs & rhythm",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Text(
                    text = "zen index $mindfulScore/100",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini sparkline bar row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dayPoints.forEach { point ->
                    val fraction = (point.screenTimeMinutes.toFloat() / maxMins.toFloat()).coerceIn(0.08f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(fraction = fraction)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(if (point.isToday) palette.primaryText else palette.borderSubtle)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = point.dayLabel.take(1).lowercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                            color = if (point.isToday) palette.primaryText else palette.mutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThirdPartyWidgetsSection(
    widgetIds: List<Int>,
    appWidgetHost: AppWidgetHost?,
    appWidgetManager: AppWidgetManager?,
    onAddWidgetClick: () -> Unit,
    onRemoveWidgetClick: (Int) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "custom app widgets",
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryText
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddWidgetClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+ add widget",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (widgetIds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "no android widgets added yet · tap '+ add widget' above to place any weather, notes, or music widget",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryText,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            widgetIds.forEach { id ->
                val info = appWidgetManager?.getAppWidgetInfo(id)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = info?.label ?: "Android Widget",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                            Text(
                                text = "✕ remove",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.mutedText,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onRemoveWidgetClick(id)
                                    }
                                    .padding(4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (appWidgetHost != null && info != null) {
                            AndroidView(
                                factory = { ctx ->
                                    runCatching {
                                        appWidgetHost.createView(ctx, id, info).apply {
                                            setAppWidget(id, info)
                                        }
                                    }.getOrElse { View(ctx) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 90.dp, max = 220.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

