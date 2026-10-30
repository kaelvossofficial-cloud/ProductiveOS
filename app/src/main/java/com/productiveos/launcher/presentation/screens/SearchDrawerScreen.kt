package com.productiveos.launcher.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.data.preferences.TextAlignmentOption
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchDrawerScreen(
    query: String,
    filteredApps: List<AppEntity>,
    settings: LauncherSettings,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppEntity) -> Unit,
    onAppLongClick: (AppEntity) -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        // Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface)
                .border(1.dp, palette.borderSubtle, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = if (settings.enableT9Search) {
                            "type app name or t9 digits..."
                        } else {
                            "type app name..."
                        },
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
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (filteredApps.isNotEmpty()) {
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

        Spacer(modifier = Modifier.height(12.dp))

        // Quick App Count & Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
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
        HorizontalDivider(color = palette.borderSubtle)

        // Alphabetical App Directory LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = filteredApps,
                key = { it.packageName }
            ) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onAppClick(app) },
                            onLongClick = { onAppLongClick(app) }
                        )
                        .padding(vertical = 15.dp, horizontal = 4.dp),
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
    }
}

