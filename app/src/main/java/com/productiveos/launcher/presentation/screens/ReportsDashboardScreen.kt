package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.repository.AppUsageItem
import com.productiveos.launcher.data.repository.DayUsagePoint
import com.productiveos.launcher.data.repository.ReportPeriod
import com.productiveos.launcher.data.repository.UsageReportData
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette

@Composable
fun ReportsDashboardScreen(
    reportData: UsageReportData,
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onRequestUsagePermission: () -> Unit,
    onAppClick: (String) -> Unit,
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
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "productiveos",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText
                )
                Text(
                    text = "mindful insights",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.primaryText
                )
            }

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
                    text = "home →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Period Tab Selector (Daily / Weekly / Monthly)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surface)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ReportPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) palette.surfaceElevated else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) palette.borderSubtle else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onPeriodSelected(period)
                        }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.title.lowercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = if (isSelected) palette.primaryText else palette.secondaryText
                    )
                }
            }
        }

        if (!reportData.hasPermission) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.surfaceElevated)
                    .border(1.dp, palette.accentDot.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .clickable { onRequestUsagePermission() }
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Grant Usage Access for Real-Time Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Android requires explicit permission to calculate exact app durations and daily screen time. Tap here to open Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BLOCK 1: Circular / Radial Ring Graph
        RadialRingCard(
            reportData = reportData,
            selectedPeriod = selectedPeriod
        )

        Spacer(modifier = Modifier.height(20.dp))

        // BLOCK 2: 7-Day Screen Time Bar Chart
        DailyTrendChartCard(
            dayPoints = reportData.dailyPoints,
            dailyTargetMinutes = reportData.dailyTargetMinutes / if (selectedPeriod == ReportPeriod.DAILY) 1 else 7
        )

        Spacer(modifier = Modifier.height(20.dp))

        // BLOCK 3: Most Used Apps Breakdown
        MostUsedAppsCard(
            topApps = reportData.topApps,
            totalMinutes = reportData.totalScreenTimeMinutes,
            onAppClick = onAppClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        // BLOCK 4: Time of Day Distribution
        TimeOfDayDistributionCard(reportData = reportData)

        Spacer(modifier = Modifier.height(20.dp))

        // BLOCK 5: ProductiveOS Overview & Mindfulness Benchmark
        MindfulnessOverviewCard(reportData = reportData)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RadialRingCard(
    reportData: UsageReportData,
    selectedPeriod: ReportPeriod
) {
    val palette = LocalProductiveOSPalette.current

    val animScreenPercent by animateFloatAsState(
        targetValue = reportData.screenTimePercent,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "animScreenPercent"
    )
    val animUnlocksPercent by animateFloatAsState(
        targetValue = reportData.unlocksPercent,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "animUnlocksPercent"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "screen time ratio",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    val middleStroke = 6.dp.toPx()

                    // Outer Ring (Screen Time)
                    val outerDiameter = size.minDimension - strokeWidth
                    val outerTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    val outerSize = Size(outerDiameter, outerDiameter)

                    // Track
                    drawArc(
                        color = palette.borderSubtle,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress
                    drawArc(
                        color = palette.primaryText,
                        startAngle = -90f,
                        sweepAngle = (animScreenPercent * 360f).coerceIn(0f, 360f),
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Middle Ring (Unlocks vs Daily Target)
                    val middleOffset = strokeWidth + 8.dp.toPx()
                    val middleDiameter = outerDiameter - (2 * 8.dp.toPx())
                    val middleTopLeft = Offset(middleOffset / 2f, middleOffset / 2f)
                    val mSize = Size(middleDiameter, middleDiameter)

                    drawArc(
                        color = palette.borderSubtle.copy(alpha = 0.5f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = middleTopLeft,
                        size = mSize,
                        style = Stroke(width = middleStroke)
                    )
                    drawArc(
                        color = palette.secondaryText,
                        startAngle = -90f,
                        sweepAngle = (animUnlocksPercent * 360f).coerceIn(0f, 360f),
                        useCenter = false,
                        topLeft = middleTopLeft,
                        size = mSize,
                        style = Stroke(width = middleStroke, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = reportData.formattedTotalTime,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 32.sp, lineHeight = 36.sp),
                        color = palette.primaryText,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        text = "${(reportData.screenTimePercent * 100).toInt()}% of mindful limit",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(palette.primaryText)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "screen duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(palette.secondaryText)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${reportData.unlockCount} unlocks",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTrendChartCard(
    dayPoints: List<DayUsagePoint>,
    dailyTargetMinutes: Int
) {
    val palette = LocalProductiveOSPalette.current
    val maxMins = (dayPoints.maxOfOrNull { it.screenTimeMinutes } ?: 240).coerceAtLeast(180)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "screen time over days",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primaryText
                )
                Text(
                    text = "target 4h/day",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 7-Day Vertical Bar Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val chartHeight = size.height
                    val colWidth = 18.dp.toPx()
                    val totalSlots = dayPoints.size.coerceAtLeast(1)
                    val spacing = (size.width - (colWidth * totalSlots)) / (totalSlots + 1)

                    // Draw Target Dashed Line
                    val targetY = chartHeight * (1f - (dailyTargetMinutes.toFloat() / maxMins.toFloat()).coerceIn(0f, 1f))
                    drawLine(
                        color = palette.borderSubtle,
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )

                    // Draw each day bar
                    dayPoints.forEachIndexed { index, point ->
                        val x = spacing + (index * (colWidth + spacing))
                        val barHeight = chartHeight * (point.screenTimeMinutes.toFloat() / maxMins.toFloat()).coerceIn(0.04f, 1f)
                        val y = chartHeight - barHeight

                        val barColor = if (point.isToday) {
                            palette.primaryText
                        } else {
                            palette.secondaryText.copy(alpha = 0.65f)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(colWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayPoints.forEach { point ->
                    Text(
                        text = point.dayLabel.lowercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (point.isToday) palette.primaryText else palette.mutedText
                    )
                }
            }
        }
    }
}

@Composable
private fun MostUsedAppsCard(
    topApps: List<AppUsageItem>,
    totalMinutes: Int,
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
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "most used apps",
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ranked by conscious attention consumed",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (topApps.isEmpty()) {
                Text(
                    text = "no usage recorded yet today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.mutedText
                )
            } else {
                topApps.forEachIndexed { index, app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAppClick(app.packageName)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.  ${app.appLabel.lowercase()}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.primaryText
                            )
                            Text(
                                text = "${app.formattedDuration} · ${(app.percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Thin custom progress bar (0.75dp aesthetic)
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
                                    .fillMaxWidth(fraction = app.percentage.coerceIn(0.02f, 1f))
                                    .clip(RoundedCornerShape(50))
                                    .background(palette.primaryText)
                            )
                        }
                    }
                    if (index < topApps.size - 1) {
                        HorizontalDivider(
                            color = palette.borderSubtle.copy(alpha = 0.4f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeOfDayDistributionCard(reportData: UsageReportData) {
    val palette = LocalProductiveOSPalette.current
    val buckets = reportData.timeBuckets

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "time of day distribution",
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "usage split: morning vs afternoon vs evening",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented 3-part bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.borderSubtle)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(buckets.morningPercent.coerceAtLeast(0.05f))
                        .background(palette.primaryText)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(buckets.afternoonPercent.coerceAtLeast(0.05f))
                        .background(palette.secondaryText)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(buckets.eveningPercent.coerceAtLeast(0.05f))
                        .background(palette.mutedText)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeSlotLegend(
                    label = "Morning",
                    hours = "06:00 - 12:00",
                    percent = (buckets.morningPercent * 100).toInt(),
                    color = palette.primaryText
                )
                TimeSlotLegend(
                    label = "Afternoon",
                    hours = "12:00 - 18:00",
                    percent = (buckets.afternoonPercent * 100).toInt(),
                    color = palette.secondaryText
                )
                TimeSlotLegend(
                    label = "Evening",
                    hours = "18:00 - 24:00",
                    percent = (buckets.eveningPercent * 100).toInt(),
                    color = palette.mutedText
                )
            }
        }
    }
}

@Composable
private fun TimeSlotLegend(
    label: String,
    hours: String,
    percent: Int,
    color: Color
) {
    val palette = LocalProductiveOSPalette.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label.lowercase(),
                style = MaterialTheme.typography.labelMedium,
                color = palette.primaryText
            )
        }
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleMedium,
            color = palette.primaryText,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = hours,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
            color = palette.mutedText
        )
    }
}

@Composable
private fun MindfulnessOverviewCard(reportData: UsageReportData) {
    val palette = LocalProductiveOSPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surfaceElevated)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(18.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "overview by productiveos",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primaryText
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "score ${reportData.mindfulScore}/100",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryText
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "daily unlocks",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                    Text(
                        text = "${reportData.unlockCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                }

                Column {
                    Text(
                        text = "avg gap between unlocks",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                    val gapMins = if (reportData.unlockCount > 0) {
                        (720 / reportData.unlockCount).coerceIn(5, 120)
                    } else 45
                    Text(
                        text = "~$gapMins min",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                }

                Column {
                    Text(
                        text = "screen-off rest",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                    val restHours = (24 - (reportData.totalScreenTimeMinutes / 60)).coerceAtLeast(12)
                    Text(
                        text = "${restHours}h",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                }
            }
        }
    }
}
