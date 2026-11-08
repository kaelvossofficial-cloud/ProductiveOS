package com.productiveos.launcher.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.IconColorMode
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.presentation.theme.GoogleShape
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.flow.collectLatest

val SnappyEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Hold duration (ms) at which the quick action capsule opens. */
private const val QUICK_ACTION_MS = 620L

/**
 * Wraps an app row or icon with the hold-to-open gesture, radial glow halo, and quick action popup.
 */
@Composable
fun HoldToOpenHost(
    app: AppEntity,
    settings: LauncherSettings,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
    onHoldChange: (String?, Float) -> Unit,
    onLaunch: (AppEntity, Offset) -> Unit,
    onMoreOptions: (AppEntity) -> Unit,
    content: @Composable (progress: () -> Float) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val haptic = LocalHapticFeedback.current
    val rawTarget = remember { mutableFloatStateOf(0f) }
    val progress = remember { Animatable(0f) }
    var showQuickActions by remember { mutableStateOf(false) }
    var centerInWindow by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(Unit) {
        snapshotFlow { rawTarget.floatValue }.collectLatest { t ->
            if (t > 0f) progress.snapTo(t)
            else progress.animateTo(0f, tween(180, easing = SnappyEaseOut))
        }
    }

    val rowAlpha by animateFloatAsState(
        targetValue = if (dimmed) 0.35f else 1f,
        animationSpec = tween(200, easing = SnappyEaseOut),
        label = "PeripheralDim"
    )

    val holdMs = (settings.holdToOpenSeconds * 1000L).coerceAtLeast(1000L)

    Box(
        modifier = modifier
            .graphicsLayer { alpha = rowAlpha }
            .onGloballyPositioned { coords ->
                centerInWindow = coords.positionInWindow() +
                    Offset(coords.size.width / 2f, coords.size.height / 2f)
            }
            .holdGlow(
                progress = { progress.value },
                core = palette.accentPrimary,
                halo = palette.accentSecondary
            )
            .holdToOpenGesture(
                key = app.packageName,
                enabled = settings.enableHoldToOpen,
                holdMs = holdMs,
                quickActionMs = QUICK_ACTION_MS.coerceAtMost(holdMs - 200L),
                onHoldStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onHoldChange(app.packageName, 0.001f)
                },
                onProgress = { p ->
                    rawTarget.floatValue = p.coerceAtLeast(0.001f)
                    onHoldChange(app.packageName, p)
                },
                onQuickActions = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showQuickActions = true
                },
                onComplete = {
                    showQuickActions = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLaunch(app, centerInWindow)
                },
                onRelease = {
                    rawTarget.floatValue = 0f
                    onHoldChange(null, 0f)
                },
                onTap = { onLaunch(app, centerInWindow) }
            )
    ) {
        content { progress.value }

        if (showQuickActions) {
            QuickActionPopup(
                app = app,
                favorites = settings.favoriteContacts,
                onDismiss = { showQuickActions = false },
                onOpenApp = {
                    showQuickActions = false
                    onLaunch(app, centerInWindow)
                },
                onMoreOptions = {
                    showQuickActions = false
                    onMoreOptions(app)
                }
            )
        }
    }
}

/**
 * Responsive 2-column Liquid Glass Floating Home Icons.
 * Automatically scales to fit available viewport height so icons NEVER overlap or clip.
 */
@Composable
fun FloatingHomeIcons(
    apps: List<AppEntity>,
    settings: LauncherSettings,
    tilt: State<Offset>,
    holdingPackage: String?,
    modifier: Modifier = Modifier,
    onHoldChange: (String?, Float) -> Unit,
    onLaunch: (AppEntity, Offset) -> Unit,
    onMoreOptions: (AppEntity) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val scaleMultiplier = settings.iconScale.coerceIn(0.75f, 1.35f)
    val monochromeTint = if (settings.iconColorMode == IconColorMode.THEME_MONOCHROME) {
        palette.primaryText
    } else {
        null
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val visibleApps = apps.take(6)
        val rowCount = ((visibleApps.size + 1) / 2).coerceAtLeast(1)
        val maxAllowedIconDp = ((maxHeight.value / (rowCount + 0.7f)) - 26f).coerceIn(46f, 84f)
        val iconSize = (64f * scaleMultiplier).coerceAtMost(maxAllowedIconDp).dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            visibleApps.chunked(2).forEachIndexed { rowIdx, rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowApps.forEachIndexed { colIdx, app ->
                        val depth = if ((rowIdx + colIdx) % 2 == 0) 0.85f else 1.1f
                        val shape = remember(app.packageName) {
                            GoogleShape.forKey(app.packageName.hashCode())
                        }

                        HoldToOpenHost(
                            app = app,
                            settings = settings,
                            dimmed = holdingPackage != null && holdingPackage != app.packageName,
                            modifier = Modifier.graphicsLayer {
                                val t = tilt.value
                                translationX = t.x * 6.dp.toPx() * depth
                                translationY = (t.y - 0.9f) * 5.dp.toPx() * depth
                            },
                            onHoldChange = onHoldChange,
                            onLaunch = onLaunch,
                            onMoreOptions = onMoreOptions
                        ) { progress ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(iconSize + 36.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(iconSize)
                                        .border(
                                            width = 1.dp,
                                            color = palette.glassBorderTop,
                                            shape = shape.shape
                                        )
                                        .graphicsLayer {
                                            val p = progress()
                                            val s = 1f - 0.05f * p
                                            scaleX = s
                                            scaleY = s
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ShapedAppIcon(
                                        packageName = app.packageName,
                                        size = iconSize,
                                        shape = shape.shape,
                                        containerColor = palette.glassSurfaceElevated,
                                        monochromeTint = monochromeTint
                                    )
                                    Canvas(modifier = Modifier.requiredSize(iconSize + 12.dp)) {
                                        val p = progress()
                                        if (p > 0.01f) {
                                            val stroke = 2.5.dp.toPx()
                                            drawArc(
                                                color = palette.accentPrimary,
                                                startAngle = -90f,
                                                sweepAngle = 360f * p,
                                                useCenter = false,
                                                topLeft = Offset(stroke / 2, stroke / 2),
                                                size = Size(size.width - stroke, size.height - stroke),
                                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = app.displayName.lowercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = palette.primaryText.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
