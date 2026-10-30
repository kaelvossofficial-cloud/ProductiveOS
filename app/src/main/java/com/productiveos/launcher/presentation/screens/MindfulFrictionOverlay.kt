package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.presentation.components.ActionPillButton
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import kotlinx.coroutines.delay

@Composable
fun MindfulFrictionOverlay(
    targetApp: AppEntity,
    totalSeconds: Int,
    onCancelAndReturnHome: (String) -> Unit,
    onContinueToApp: (String) -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    var remainingSeconds by remember(targetApp, totalSeconds) {
        mutableIntStateOf(totalSeconds.coerceAtLeast(1))
    }
    var selectedReason by remember(targetApp) { mutableStateOf<String?>(null) }

    LaunchedEffect(targetApp, totalSeconds) {
        remainingSeconds = totalSeconds.coerceAtLeast(1)
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds--
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "breathingCircle")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val isTimerDone = remainingSeconds <= 0
    val canProceed = isTimerDone && selectedReason != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(enabled = false) {}
            .padding(horizontal = 28.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "pathcheck · mindful intent gate",
                style = MaterialTheme.typography.labelMedium,
                color = palette.mutedText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pause. Why are you opening ${targetApp.displayName.lowercase()}?",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Expanding / Contracting Circular Breathing Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(156.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .scale(breathingScale)
                        .clip(CircleShape)
                        .background(palette.surface)
                        .border(1.5.dp, palette.secondaryText.copy(alpha = 0.55f), CircleShape)
                )

                Text(
                    text = if (isTimerDone) "ready" else "${remainingSeconds}s",
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.primaryText,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "select your current trigger:",
                style = MaterialTheme.typography.labelMedium,
                color = palette.secondaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Quick Intent Pill Buttons: [Boredom], [Need Info], [Habit]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Boredom", "Need Info", "Habit").forEach { reason ->
                    val isSelected = selectedReason == reason
                    val bgColor = if (isSelected) palette.primaryText else palette.surface
                    val txtColor = if (isSelected) palette.background else palette.primaryText

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(bgColor)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) palette.primaryText else palette.borderSubtle,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedReason = reason }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = reason.lowercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = txtColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Primary White Pill: Cancel & Stay Focused
            ActionPillButton(
                text = "Cancel & Stay Focused",
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onCancelAndReturnHome(selectedReason ?: "Habit")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Pill: Continue to App (Unlocked after countdown + intent selection)
            val buttonLabel = when {
                !isTimerDone -> "Breathe (${remainingSeconds}s)..."
                selectedReason == null -> "Select intent above to unlock"
                else -> "Continue to ${targetApp.displayName.lowercase()} →"
            }

            ActionPillButton(
                text = buttonLabel,
                isPrimary = false,
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val chosen = selectedReason ?: "Need Info"
                    onContinueToApp(chosen)
                }
            )
        }
    }
}
