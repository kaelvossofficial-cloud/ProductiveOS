package com.productiveos.launcher.presentation.components

import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.LauncherSettings
import com.productiveos.launcher.presentation.theme.GoogleShape
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val SnappyEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/**
 * Wraps any content with the full hold ritual: progress ring + magical halo, a quick-action capsule
 * at [QUICK_ACTION_MS] and launch at 100%. Content receives a progress provider (read it only in
 * draw/graphicsLayer lambdas so holding never recomposes the screen).
 */
const val QUICK_ACTION_MS = 750L

@Composable
fun HoldToOpenHost(
    app: AppEntity,
    settings: LauncherSettings,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
    onHoldChange: (packageName: String?, progress: Float) -> Unit,
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
                favorites = settings.favoriteContacts,
                onDismiss = { showQuickActions = false },
                onOpenApp = {
                    showQuickActions = false
                    onLaunch(app, centerInWindow)
                },
                onMoreOptions = { onMoreOptions(app) }
            )
        }
    }
}

private data class FloatSlot(
    val xFraction: Float,
    val yFraction: Float,
    val sizeDp: Float,
    val depth: Float,
    val bobMul: Int,
    val phaseShift: Float
)

private val floatSlots = listOf(
    FloatSlot(0.06f, 0.04f, 72f, 1.0f, 1, 0.0f),
    FloatSlot(0.60f, 0.00f, 64f, 1.4f, 2, 1.3f),
    FloatSlot(0.32f, 0.27f, 80f, 0.7f, 1, 2.1f),
    FloatSlot(0.02f, 0.50f, 66f, 1.2f, 2, 3.4f),
    FloatSlot(0.66f, 0.42f, 74f, 0.9f, 1, 4.2f),
    FloatSlot(0.34f, 0.70f, 68f, 1.3f, 2, 5.3f)
)

/**
 * Pinned apps as floating Google-shaped icons. They bob gently on their own and drift with device
 * tilt (gravity), each at its own depth, so flipping the phone visibly shifts the whole constellation.
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
    val density = LocalDensity.current
    val infinite = rememberInfiniteTransition(label = "FloatBob")
    val bobPhase = infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BobPhase"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val areaWidth = maxWidth
        val areaHeight = maxHeight
        apps.take(floatSlots.size).forEachIndexed { index, app ->
            val slot = floatSlots[index]
            val iconSize = slot.sizeDp.dp
            val x = (areaWidth - iconSize) * slot.xFraction
            val y = (areaHeight - iconSize - 18.dp) * slot.yFraction
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
                        translationX = t.x * 16.dp.toPx() * slot.depth +
                            cos(bobPhase.value + slot.phaseShift) * 3.dp.toPx()
                        translationY = (t.y - 0.9f) * 11.dp.toPx() * slot.depth + bob * 6.dp.toPx()
                        rotationZ = t.x * 5f * slot.depth + bob * 1.5f
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
                                val s = 1f - 0.07f * p
                                scaleX = s
                                scaleY = s
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ShapedAppIcon(
                            packageName = app.packageName,
                            size = iconSize,
                            shape = shape.shape,
                            containerColor = palette.surface
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
