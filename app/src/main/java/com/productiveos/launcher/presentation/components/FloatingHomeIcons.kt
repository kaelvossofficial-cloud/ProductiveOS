package com.productiveos.launcher.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.IconColorMode
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.presentation.theme.GoogleShape
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Hold duration (ms) at which the quick action capsule opens. */
private const val QUICK_ACTION_MS = 620L

/**
 * Wraps an app row or icon with the hold-to-open gesture, radial glow halo, and quick action popup.
 * Reports progress to the background canvas so the dot grid reacts.
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
        targetValue = if (dimmed) 0.3f else 1f,
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
                core = palette.accentDot,
                halo = palette.primaryText
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
                favoriteContacts = settings.favoriteContacts,
                onDismiss = { showQuickActions = false },
                onLaunchApp = {
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
 * Normalized 2-column centered constellation slots for home screen icons.
 * Confined strictly within the safe middle canvas (xFraction 0.14..0.66, yFraction 0.08..0.72)
 * so floating icons sit clean in the center and NEVER collide with top telemetry/clock or bottom dock.
 */
private data class FloatSlot(
    val xFraction: Float,
    val yFraction: Float,
    val baseSizeDp: Float,
    val depth: Float,
    val bobMul: Int,
    val phaseShift: Float
)

private val floatSlots = listOf(
    FloatSlot(0.16f, 0.08f, 68f, 0.8f, 1, 0.0f),
    FloatSlot(0.62f, 0.08f, 68f, 1.1f, 2, 1.4f),
    FloatSlot(0.16f, 0.38f, 68f, 0.9f, 1, 2.2f),
    FloatSlot(0.62f, 0.38f, 68f, 1.2f, 2, 3.5f),
    FloatSlot(0.16f, 0.68f, 68f, 0.8f, 1, 4.3f),
    FloatSlot(0.62f, 0.68f, 68f, 1.0f, 2, 5.4f)
)

/**
 * Pinned apps as floating Google-shaped icons.
 * Confined in a clean central viewport, respecting [iconScale] and [iconColorMode].
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
    val infinite = rememberInfiniteTransition(label = "FloatBob")
    val bobPhase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BobPhase"
    )

    val scaleMultiplier = settings.iconScale.coerceIn(0.70f, 1.40f)
    val monochromeTint = if (settings.iconColorMode == IconColorMode.THEME_MONOCHROME) {
        palette.primaryText
    } else {
        null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val areaWidth = maxWidth
        val areaHeight = maxHeight

        apps.take(floatSlots.size).forEachIndexed { index, app ->
            val slot = floatSlots[index]
            val iconSize = (slot.baseSizeDp * scaleMultiplier).dp
            val x = (areaWidth - iconSize) * slot.xFraction
            val y = (areaHeight - iconSize - 20.dp) * slot.yFraction
            val shape = remember(app.packageName) { GoogleShape.forKey(app.packageName.hashCode()) }

            HoldToOpenHost(
                app = app,
                settings = settings,
                dimmed = holdingPackage != null && holdingPackage != app.packageName,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .graphicsLayer {
                        val t = tilt.value
                        val bob = sin(bobPhase.value * slot.bobMul + slot.phaseShift)
                        // Restrained parallax range (max 9dp) so icons stay centered
                        translationX = t.x * 9.dp.toPx() * slot.depth +
                            cos(bobPhase.value + slot.phaseShift) * 2.dp.toPx()
                        translationY = (t.y - 0.9f) * 7.dp.toPx() * slot.depth + bob * 4.dp.toPx()
                        rotationZ = t.x * 3f * slot.depth + bob * 1f
                    },
                onHoldChange = onHoldChange,
                onLaunch = onLaunch,
                onMoreOptions = onMoreOptions
            ) { progress ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.size(width = iconSize + 12.dp, height = iconSize + 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .graphicsLayer {
                                val p = progress()
                                val s = 1f - 0.06f * p
                                scaleX = s
                                scaleY = s
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ShapedAppIcon(
                            packageName = app.packageName,
                            size = iconSize,
                            shape = shape.shape,
                            containerColor = palette.surface,
                            monochromeTint = monochromeTint
                        )
                        Canvas(modifier = Modifier.requiredSize(iconSize + 14.dp)) {
                            val p = progress()
                            if (p > 0.01f) {
                                val stroke = 2.5.dp.toPx()
                                drawArc(
                                    color = palette.primaryText.copy(alpha = 0.9f),
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
                    Text(
                        text = app.displayName.lowercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                        color = palette.secondaryText,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
