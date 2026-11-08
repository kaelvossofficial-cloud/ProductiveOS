package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.repository.AppUsageItem
import com.productiveos.launcher.data.repository.DayUsagePoint
import com.productiveos.launcher.data.repository.ReportPeriod
import com.productiveos.launcher.data.repository.UsageReportData
import com.productiveos.launcher.presentation.theme.LiquidGlassCard
import com.productiveos.launcher.presentation.theme.LiquidGlassPill
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
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PRODUCTIVE OS · ANALYTICS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.1.sp
                    ),
                    color = palette.accentPrimary
                )
                Text(
                    text = "Mindful Insights",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
            }

            LiquidGlassPill(
                selected = false,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackToHome()
                }
            ) {
                Text(
                    text = "Home →",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Period Tab Selector (Daily / Weekly / Monthly)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportPeriod.entries.forEach { period ->
                val isSelected = period == selectedPeriod
                LiquidGlassPill(
                    selected = isSelected,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPeriodSelected(period)
                    }
                ) {
                    Text(
                        text = period.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) palette.primaryText else palette.secondaryText
                    )
                }
            }
        }

        if (!reportData.hasPermission) {
            Spacer(modifier = Modifier.height(16.dp))
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                elevated = true,
                accentHighlight = true,
                onClick = onRequestUsagePermission
            ) {
                Column {
                    Text(
                        text = "Grant Usage Access for Real-Time Telemetry →",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Android requires Usage Access permission to compute exact screen durations and app breakdowns. Tap to enable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // BLOCK 1: Dual Gradient Radial Ring Graph
        RadialRingCard(reportData = reportData)

        Spacer(modifier = Modifier.height(18.dp))

        // BLOCK 2: 7-Day Screen Time Gradient Bar Chart
        DailyTrendChartCard(
            dayPoints = reportData.dailyPoints,
            dailyTargetMinutes = reportData.dailyTargetMinutes / if (selectedPeriod == ReportPeriod.DAILY) 1 else 7
        )

        Spacer(modifier = Modifier.height(18.dp))

        // BLOCK 3: Most Used Apps Breakdown
        MostUsedAppsCard(
            topApps = reportData.topApps,
            onAppClick = onAppClick
        )

        Spacer(modifier = Modifier.height(18.dp))

        // BLOCK 4: Time of Day Distribution
        TimeOfDayDistributionCard(reportData = reportData)

        Spacer(modifier = Modifier.height(18.dp))

        // BLOCK 5: ProductiveOS Overview & Mindfulness Benchmark
        MindfulnessOverviewCard(reportData = reportData)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RadialRingCard(
    reportData: UsageReportData
) {
    val palette = LocalProductiveOSPalette.current

    val animScreenPercent by animateFloatAsState(
        targetValue = reportData.screenTimePercent,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "animScreenPercent"
    )
    val animUnlocksPercent by animateFloatAsState(
        targetValue = reportData.unlocksPercent,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "animUnlocksPercent"
    )

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        elevated = true,
        accentHighlight = true,
        paddingHorizontal = 20.dp,
        paddingVertical = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CONSCIOUS SCREEN BUDGET",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.5.sp,
                    letterSpacing = 1.sp
                ),
                color = palette.accentPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier.size(204.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val middleStroke = 7.dp.toPx()

                    // Soft radial aura behind the ring
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                palette.accentPrimary.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.48f
                        ),
                        radius = size.minDimension * 0.48f,
                        center = center
                    )

                    val outerDiameter = size.minDimension - strokeWidth
                    val outerTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                    val outerSize = Size(outerDiameter, outerDiameter)

                    // Outer Track
                    drawArc(
                        color = palette.borderSubtle.copy(alpha = 0.55f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = strokeWidth)
                    )
                    // Outer Progress (Gradient Accent Primary -> Secondary)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                palette.accentPrimary,
                                palette.accentSecondary,
                                palette.accentPrimary
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = (animScreenPercent * 360f).coerceIn(4f, 360f),
                        useCenter = false,
                        topLeft = outerTopLeft,
                        size = outerSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Middle Ring (Unlocks vs Target)
                    val gap = 12.dp.toPx()
                    val middleOffset = strokeWidth + gap
                    val middleDiameter = outerDiameter - (2 * gap)
                    val middleTopLeft = Offset(middleOffset / 2f, middleOffset / 2f)
                    val mSize = Size(middleDiameter, middleDiameter)

                    drawArc(
                        color = palette.borderSubtle.copy(alpha = 0.35f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = middleTopLeft,
                        size = mSize,
                        style = Stroke(width = middleStroke)
                    )
                    drawArc(
                        color = palette.accentSecondary,
                        startAngle = -90f,
                        sweepAngle = (animUnlocksPercent * 360f).coerceIn(4f, 360f),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.accentPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Screen Duration",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(palette.accentSecondary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${reportData.unlockCount} Unlocks",
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

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        paddingHorizontal = 20.dp,
        paddingVertical = 20.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "7-Day Screen Rhythm",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primaryText
                    )
                    Text(
                        text = "Daily duration vs 4h mindful target line",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        color = palette.secondaryText
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.accentPrimary.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Target 4h",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                        color = palette.accentPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val chartHeight = size.height
                    val colWidth = 22.dp.toPx()
                    val totalSlots = dayPoints.size.coerceAtLeast(1)
                    val spacing = (size.width - (colWidth * totalSlots)) / (totalSlots + 1)

                    // Target Dashed Line
                    val targetY = chartHeight * (1f - (dailyTargetMinutes.toFloat() / maxMins.toFloat()).coerceIn(0f, 1f))
                    drawLine(
                        color = palette.accentPrimary.copy(alpha = 0.45f),
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )

                    dayPoints.forEachIndexed { index, point ->
                        val x = spacing + (index * (colWidth + spacing))
                        val barHeight = chartHeight * (point.screenTimeMinutes.toFloat() / maxMins.toFloat()).coerceIn(0.06f, 1f)
                        val y = chartHeight - barHeight
                        val corner = CornerRadius(7.dp.toPx(), 7.dp.toPx())

                        // Track slot background
                        drawRoundRect(
                            color = palette.borderSubtle.copy(alpha = 0.25f),
                            topLeft = Offset(x, 0f),
                            size = Size(colWidth, chartHeight),
                            cornerRadius = corner
                        )

                        val barBrush = if (point.isToday) {
                            Brush.verticalGradient(
                                colors = listOf(palette.accentPrimary, palette.accentSecondary),
                                startY = y,
                                endY = chartHeight
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    palette.accentPrimary.copy(alpha = 0.55f),
                                    palette.accentSecondary.copy(alpha = 0.30f)
                                ),
                                startY = y,
                                endY = chartHeight
                            )
                        }

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(x, y),
                            size = Size(colWidth, barHeight),
                            cornerRadius = corner
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayPoints.forEach { point ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = point.dayLabel.lowercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (point.isToday) palette.accentPrimary else palette.secondaryText
                        )
                        val hrs = point.screenTimeMinutes / 60
                        val mins = point.screenTimeMinutes % 60
                        Text(
                            text = if (hrs > 0) "${hrs}h${mins}m" else "${mins}m",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.5.sp),
                            color = palette.mutedText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MostUsedAppsCard(
    topApps: List<AppUsageItem>,
    onAppClick: (String) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        paddingHorizontal = 20.dp,
        paddingVertical = 20.dp
    ) {
        Column {
            Text(
                text = "Attention Allocation",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Most used applications ranked by active foreground time",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = palette.secondaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (topApps.isEmpty()) {
                Text(
                    text = "No usage recorded yet for this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.mutedText
                )
            } else {
                topApps.forEachIndexed { index, app ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
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
                                text = "${index + 1}.  ${app.appLabel}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = palette.primaryText
                            )
                            Text(
                                text = "${app.formattedDuration} · ${(app.percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.accentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(palette.borderSubtle.copy(alpha = 0.45f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = app.percentage.coerceIn(0.04f, 1f))
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(palette.accentPrimary, palette.accentSecondary)
                                        )
                                    )
                            )
                        }
                    }
                    if (index < topApps.size - 1) {
                        HorizontalDivider(
                            color = palette.borderSubtle.copy(alpha = 0.35f),
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

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        paddingHorizontal = 20.dp,
        paddingVertical = 20.dp
    ) {
        Column {
            Text(
                text = "Circadian Usage Split",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Morning vs Afternoon vs Evening screen engagement",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = palette.secondaryText
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.borderSubtle),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(buckets.morningPercent.coerceAtLeast(0.05f))
                        .background(palette.accentPrimary)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(buckets.afternoonPercent.coerceAtLeast(0.05f))
                        .background(palette.accentSecondary)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(buckets.eveningPercent.coerceAtLeast(0.05f))
                        .background(palette.primaryText.copy(alpha = 0.55f))
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
                    color = palette.accentPrimary
                )
                TimeSlotLegend(
                    label = "Afternoon",
                    hours = "12:00 - 18:00",
                    percent = (buckets.afternoonPercent * 100).toInt(),
                    color = palette.accentSecondary
                )
                TimeSlotLegend(
                    label = "Evening",
                    hours = "18:00 - 24:00",
                    percent = (buckets.eveningPercent * 100).toInt(),
                    color = palette.primaryText.copy(alpha = 0.65f)
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
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.primaryText
            )
        }
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.primaryText
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

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = true,
        accentHighlight = true,
        paddingHorizontal = 20.dp,
        paddingVertical = 20.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mindful OS Index",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.primaryText
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.accentPrimary.copy(alpha = 0.20f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Score ${reportData.mindfulScore}/100",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.accentPrimary
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
                        text = "Daily Unlocks",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                    Text(
                        text = "${reportData.unlockCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primaryText
                    )
                }

                Column {
                    Text(
                        text = "Avg Focus Gap",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                    val gapMins = if (reportData.unlockCount > 0) {
                        (720 / reportData.unlockCount).coerceIn(5, 120)
                    } else 45
                    Text(
                        text = "~$gapMins min",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primaryText
                    )
                }

                Column {
                    Text(
                        text = "Offline Rest",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText
                    )
                    val restHours = (24 - (reportData.totalScreenTimeMinutes / 60)).coerceAtLeast(12)
                    Text(
                        text = "${restHours}h",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.primaryText
                    )
                }
            }
        }
    }
}
