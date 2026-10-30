package com.productiveos.launcher.presentation.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.ClipEntity
import com.productiveos.launcher.data.local.MicroLogEntity
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantumClipSheet(
    clips: List<ClipEntity>,
    initialFilter: String = "",
    onDismiss: () -> Unit,
    onCopyClip: (String) -> Unit,
    onShareClip: (String) -> Unit,
    onDeleteClip: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterQuery by remember(initialFilter) { mutableStateOf(initialFilter) }

    val filteredClips = remember(clips, filterQuery) {
        if (filterQuery.isBlank()) clips
        else clips.filter { it.content.contains(filterQuery.trim(), ignoreCase = true) }
    }

    val timeFormat = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceElevated,
        contentColor = palette.primaryText,
        scrimColor = palette.background.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "quantum clip vault",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.primaryText
                    )
                    Text(
                        text = "${filteredClips.size} local snippets · 7-day auto-purge",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mutedText
                    )
                }
                if (clips.isNotEmpty()) {
                    Text(
                        text = "clear all",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onClearAll)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            BasicTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.primaryText),
                cursorBrush = SolidColor(palette.primaryText),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.background)
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        if (filterQuery.isEmpty()) {
                            Text(
                                text = "filter clipboard history...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.mutedText
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.borderSubtle)

            if (filteredClips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "no clipboard items yet · copy any text to index",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(
                        items = filteredClips,
                        key = { it.id }
                    ) { clip ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCopyClip(clip.content) }
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = clip.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.primaryText,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeFormat.format(Date(clip.timestamp)).lowercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.mutedText
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "copy",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.primaryText,
                                        modifier = Modifier.clickable { onCopyClip(clip.content) }
                                    )
                                    Text(
                                        text = "share",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.secondaryText,
                                        modifier = Modifier.clickable { onShareClip(clip.content) }
                                    )
                                    Text(
                                        text = "delete",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.mutedText,
                                        modifier = Modifier.clickable { onDeleteClip(clip.id) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicroLogJournalSheet(
    logs: List<MicroLogEntity>,
    onDismiss: () -> Unit,
    onAddLog: (String) -> Unit,
    onCopyLog: (String) -> Unit,
    onDeleteLog: (Long) -> Unit,
    onExportMarkdown: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftThought by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceElevated,
        contentColor = palette.primaryText,
        scrimColor = palette.background.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "micro-log second brain",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.primaryText
                    )
                    Text(
                        text = "saved to ProductiveOS/Inbox.md · type 'log [thought]' in search",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mutedText
                    )
                }
                if (logs.isNotEmpty()) {
                    Text(
                        text = "share .md →",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onExportMarkdown)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draftThought,
                    onValueChange = { draftThought = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.primaryText),
                    cursorBrush = SolidColor(palette.primaryText),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.background)
                                .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            if (draftThought.isEmpty()) {
                                Text(
                                    text = "capture a quick thought...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.mutedText
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                ActionPillButton(
                    text = "log",
                    isPrimary = true,
                    enabled = draftThought.isNotBlank(),
                    onClick = {
                        onAddLog(draftThought.trim())
                        draftThought = ""
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.borderSubtle)

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "no journal entries yet · type 'log [idea]' in omni-search",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.mutedText
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = logs,
                        key = { it.id }
                    ) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCopyLog(item.thought) }
                                .padding(vertical = 13.dp)
                        ) {
                            Text(
                                text = item.thought,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.primaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = timeFormat.format(Date(item.timestamp)).lowercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.mutedText
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "copy",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.secondaryText,
                                        modifier = Modifier.clickable { onCopyLog(item.thought) }
                                    )
                                    Text(
                                        text = "delete",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.mutedText,
                                        modifier = Modifier.clickable { onDeleteLog(item.id) }
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
fun BurnerNoteOverlay(
    noteText: String,
    updatedAtMillis: Long,
    onNoteChange: (String) -> Unit,
    onCopyNote: (String) -> Unit,
    onClearNote: () -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    val remainingHoursText = remember(updatedAtMillis, noteText) {
        if (noteText.isBlank() || updatedAtMillis <= 0L) {
            "auto-purges 24h after last edit"
        } else {
            val elapsedMs = System.currentTimeMillis() - updatedAtMillis
            val remMs = (24 * 3600_000L - elapsedMs).coerceAtLeast(0L)
            val hours = remMs / 3600_000L
            val mins = (remMs % 3600_000L) / 60_000L
            "self-destructs in ${hours}h ${mins}m"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 28.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "burner scratchpad",
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                    color = palette.primaryText
                )
                Text(
                    text = remainingHoursText,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (noteText.isNotBlank()) {
                    Text(
                        text = "copy",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondaryText,
                        modifier = Modifier.clickable { onCopyNote(noteText) }
                    )
                    Text(
                        text = "burn now",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.mutedText,
                        modifier = Modifier.clickable(onClick = onClearNote)
                    )
                }
                Text(
                    text = "← done",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.primaryText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(50))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = palette.borderSubtle)
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (noteText.isEmpty()) {
                Text(
                    text = "write a temporary number, code, or fleeting thought...\n\nthis page self-destructs 24 hours after your last keystroke.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        lineHeight = 30.sp
                    ),
                    color = palette.mutedText
                )
            }
            BasicTextField(
                value = noteText,
                onValueChange = onNoteChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    lineHeight = 30.sp,
                    color = palette.primaryText
                ),
                cursorBrush = SolidColor(palette.primaryText),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
            )
        }
    }
}
