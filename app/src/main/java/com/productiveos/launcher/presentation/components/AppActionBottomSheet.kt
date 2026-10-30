package com.productiveos.launcher.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionBottomSheet(
    app: AppEntity,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onSwapPinnedSlot: (() -> Unit)? = null,
    onRenameApp: (String?) -> Unit,
    onToggleFriction: () -> Unit,
    onHideApp: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isRenaming by remember { mutableStateOf(false) }
    var renameInput by remember(app) { mutableStateOf(app.displayName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceElevated,
        contentColor = palette.primaryText,
        scrimColor = palette.background.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = app.displayName.lowercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.primaryText
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = palette.mutedText,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            HorizontalDivider(color = palette.borderSubtle)

            if (isRenaming) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "custom label (leave empty to reset)",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.primaryText),
                    cursorBrush = SolidColor(palette.primaryText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.background)
                        .border(1.dp, palette.borderSubtle, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionPillButton(
                        text = "save label",
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onRenameApp(renameInput) }
                    )
                    ActionPillButton(
                        text = "reset default",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = { onRenameApp(null) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                SheetOptionRow(
                    title = if (app.isPinned) "unpin from home" else "pin to home (max 6)",
                    subtitle = if (app.isPinned) "remove from zen home essentials" else "add to zen home screen",
                    onClick = onTogglePin
                )

                if (app.isPinned && onSwapPinnedSlot != null) {
                    SheetOptionRow(
                        title = "swap pinned app",
                        subtitle = "replace this home slot with another app",
                        onClick = onSwapPinnedSlot
                    )
                }

                SheetOptionRow(
                    title = "rename app label",
                    subtitle = "e.g., rename to 'time sink' or minimalist alias",
                    onClick = { isRenaming = true }
                )

                SheetOptionRow(
                    title = if (app.isFrictionEnabled) {
                        "disable mindful friction"
                    } else {
                        "enable 5s mindful friction"
                    },
                    subtitle = if (app.isFrictionEnabled) {
                        "launch immediately without breathing pause"
                    } else {
                        "require intentional breathing countdown before opening"
                    },
                    onClick = onToggleFriction
                )

                SheetOptionRow(
                    title = "hide app to vault",
                    subtitle = "remove from home and search drawer",
                    onClick = onHideApp
                )

                SheetOptionRow(
                    title = "app info & permissions",
                    subtitle = "open android system package settings",
                    onClick = {
                        onDismiss()
                        onAppInfo()
                    }
                )

                SheetOptionRow(
                    title = "uninstall app",
                    subtitle = "remove package from device",
                    onClick = {
                        onDismiss()
                        onUninstall()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapPinnedAppBottomSheet(
    targetApp: AppEntity,
    candidateApps: List<AppEntity>,
    onDismiss: () -> Unit,
    onSelectReplacement: (AppEntity) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceElevated,
        contentColor = palette.primaryText,
        scrimColor = palette.background.copy(alpha = 0.75f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "swap '${targetApp.displayName.lowercase()}' with...",
                style = MaterialTheme.typography.titleMedium,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = palette.borderSubtle)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                items(
                    items = candidateApps.filter { !it.isPinned && !it.isHidden },
                    key = { it.packageName }
                ) { candidate ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectReplacement(candidate) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = candidate.displayName.lowercase(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.primaryText
                        )
                        Text(
                            text = "select →",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryText
                        )
                    }
                    HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun SheetOptionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.primaryText
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = palette.secondaryText,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
    HorizontalDivider(color = palette.borderSubtle.copy(alpha = 0.5f))
}

@Composable
fun ActionPillButton(
    text: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val bgColor = when {
        !enabled -> palette.surface
        isPrimary -> palette.primaryText
        else -> palette.surface
    }
    val textColor = when {
        !enabled -> palette.mutedText
        isPrimary -> palette.background
        else -> palette.primaryText
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (isPrimary && enabled) palette.primaryText else palette.borderSubtle,
                shape = RoundedCornerShape(50)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

