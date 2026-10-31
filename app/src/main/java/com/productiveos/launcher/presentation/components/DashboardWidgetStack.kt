package com.productiveos.launcher.presentation.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.productiveos.launcher.presentation.theme.DoubleBezelSquircle
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.jellyPress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Editorial Squircle Card Component using Double-Bezel ("Doppelrand") nested hardware enclosure.
 */
@Composable
fun SquircleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    DoubleBezelSquircle(
        modifier = modifier.fillMaxWidth(),
        outerRadius = 24.dp,
        bezelThickness = 2.dp,
        innerPaddingHorizontal = 18.dp,
        innerPaddingVertical = 18.dp,
        onClick = onClick,
        content = content
    )
}

data class RealCalendarEvent(
    val title: String,
    val timeLabel: String
)

/**
 * Calendar Glance Widget: Current month + 7-day mini calendar strip + Real-time Upcoming Events from Google Calendar.
 */
@Composable
fun DashboardCalendarCard(
    modifier: Modifier = Modifier,
    onOpenCalendar: () -> Unit
) {
    val context = LocalContext.current
    val palette = LocalProductiveOSPalette.current
    val calendar = remember { Calendar.getInstance() }
    val monthTitle = remember {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time).lowercase()
    }
    val todayDayOfMonth = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    val daysOfWeek = remember { listOf("m", "t", "w", "t", "f", "s", "s") }

    // Calculate the start of the current week (Monday)
    val weekDays = remember {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        (0 until 7).map {
            val d = cal.get(Calendar.DAY_OF_MONTH)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            d
        }
    }

    var upcomingEvents by remember { mutableStateOf<List<RealCalendarEvent>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                val now = System.currentTimeMillis()
                val endOfDay = now + 24 * 3600_000L
                val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(uriBuilder, now)
                ContentUris.appendId(uriBuilder, endOfDay)

                val projection = arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN
                )

                runCatching {
                    context.contentResolver.query(
                        uriBuilder.build(),
                        projection,
                        null,
                        null,
                        "${CalendarContract.Instances.BEGIN} ASC"
                    )?.use { cursor ->
                        val list = mutableListOf<RealCalendarEvent>()
                        val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                        val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

                        while (cursor.moveToNext() && list.size < 3) {
                            val title = if (titleIdx >= 0) cursor.getString(titleIdx) else "Event"
                            val begin = if (beginIdx >= 0) cursor.getLong(beginIdx) else now
                            list.add(RealCalendarEvent(title, timeFmt.format(Date(begin)).lowercase()))
                        }
                        upcomingEvents = list
                    }
                }
            }
        }
    }

    SquircleCard(
        modifier = modifier,
        onClick = onOpenCalendar
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                    color = palette.primaryText
                )
                Text(
                    text = "google calendar →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { i, dayName ->
                    val dayNum = weekDays.getOrElse(i) { i + 1 }
                    val isToday = dayNum == todayDayOfMonth

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = palette.mutedText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (isToday) palette.primaryText else Color.Transparent)
                                .border(
                                    width = if (isToday) 0.dp else 1.dp,
                                    color = if (isToday) Color.Transparent else palette.borderSubtle.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = if (isToday) palette.background else palette.primaryText
                            )
                        }
                    }
                }
            }

            if (upcomingEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "upcoming today",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    color = palette.mutedText
                )
                Spacer(modifier = Modifier.height(6.dp))
                upcomingEvents.forEach { ev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ev.title.lowercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = ev.timeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}

data class QuickTaskItem(val id: Int, val text: String, var isDone: Boolean = false)

/**
 * Editorial Tasks & Google Keep Notes Card with system sync integration.
 */
@Composable
fun DashboardTaskListCard(
    modifier: Modifier = Modifier,
    onOpenGoogleTasks: () -> Unit,
    onOpenGoogleKeepNotes: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current

    val tasks = remember {
        mutableStateListOf(
            QuickTaskItem(1, "deep work focus session", true),
            QuickTaskItem(2, "review product metrics", false),
            QuickTaskItem(3, "evening offline walk", false)
        )
    }

    SquircleCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "tasks & keep notes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                    color = palette.primaryText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "keep →",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceElevated)
                            .clickable(onClick = onOpenGoogleKeepNotes)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "tasks →",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surfaceElevated)
                            .clickable(onClick = onOpenGoogleTasks)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            tasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .jellyPress(0.96f)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tasks[index] = task.copy(isDone = !task.isDone)
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (task.isDone) palette.primaryText else Color.Transparent)
                            .border(1.5.dp, palette.primaryText, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.isDone) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(palette.background)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = task.text.lowercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (task.isDone) palette.mutedText else palette.primaryText
                    )
                }
            }
        }
    }
}

/**
 * Organic Breathing Widget: Circular element continuously breathing with rememberInfiniteTransition.
 */
@Composable
fun DashboardBreathingCard(
    modifier: Modifier = Modifier
) {
    val palette = LocalProductiveOSPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "BreatheTransition")

    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheScale"
    )

    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheAlpha"
    )

    SquircleCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "intentional pause",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 18.sp),
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (breatheScale > 1f) "exhale gently..." else "inhale slowly...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryText
                )
            }

            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = (size.minDimension / 2f) * breatheScale * 0.78f
                    drawCircle(
                        color = palette.accentDot.copy(alpha = breatheAlpha * 0.35f),
                        radius = radius
                    )
                    drawCircle(
                        color = palette.primaryText.copy(alpha = breatheAlpha),
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(palette.primaryText)
                )
            }
        }
    }
}
