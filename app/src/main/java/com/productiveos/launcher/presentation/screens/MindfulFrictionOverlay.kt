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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    onCancelAndReturnHome: () -> Unit,
    onContinueToApp: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    var remainingSeconds by remember(targetApp, totalSeconds) {
        mutableIntStateOf(totalSeconds.coerceAtLeast(1))
    }

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

    val isUnlocked = remainingSeconds <= 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .clickable(enabled = false) {}
            .padding(horizontal = 32.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Take a breath. Is this intentional?",
                style = MaterialTheme.typography.titleMedium,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Pause. Opening ${targetApp.displayName.lowercase()} intentionally?",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Expanding / Contracting Circular Breathing Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(breathingScale)
                        .clip(CircleShape)
                        .background(palette.surface)
                        .border(1.5.dp, palette.secondaryText.copy(alpha = 0.55f), CircleShape)
                )

                Text(
                    text = if (isUnlocked) "ready" else "${remainingSeconds}s",
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.primaryText,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(52.dp))

            // Primary White Pill: Cancel & Stay Focused
            ActionPillButton(
                text = "Cancel & Stay Focused",
                isPrimary = true,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCancelAndReturnHome
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Secondary Pill: Continue to App (Unlocked after countdown)
            ActionPillButton(
                text = if (isUnlocked) {
                    "Continue to ${targetApp.displayName.lowercase()} →"
                } else {
                    "Continue to App (${remainingSeconds}s)"
                },
                isPrimary = false,
                enabled = isUnlocked,
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinueToApp
            )
        }
    }
}

