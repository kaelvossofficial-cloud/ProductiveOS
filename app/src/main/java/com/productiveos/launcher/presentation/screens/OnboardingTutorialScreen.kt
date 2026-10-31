package com.productiveos.launcher.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.util.SystemLauncherHelper

@Composable
fun OnboardingTutorialScreen(
    onCompleteOnboarding: () -> Unit,
    onRequestDefaultHome: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestCalendarAccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalProductiveOSPalette.current
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) }

    // Live permission state checks that refresh when app resumes
    var isDefaultHome by remember { mutableStateOf(false) }
    var hasUsageAccess by remember { mutableStateOf(false) }
    var hasNotificationAccess by remember { mutableStateOf(false) }
    var hasCalendarAccess by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        isDefaultHome = SystemLauncherHelper.isDefaultHomeLauncher(context)
        hasUsageAccess = SystemLauncherHelper.hasUsageAccessPermission(context)
        hasNotificationAccess = SystemLauncherHelper.hasNotificationListenerPermission(context)
        hasCalendarAccess = SystemLauncherHelper.hasCalendarPermission(context)
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Bar: Step Indicator & Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stepper Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = currentStep == index
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (isSelected) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isSelected) palette.primaryText else palette.borderSubtle
                                )
                        )
                    }
                }

                // Skip Button
                Text(
                    text = "skip to home →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCompleteOnboarding() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Step Content Animated
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "OnboardingStepContent"
                ) { step ->
                    when (step) {
                        0 -> OnboardingWelcomeStep()
                        1 -> OnboardingPermissionsStep(
                            isDefaultHome = isDefaultHome,
                            hasUsageAccess = hasUsageAccess,
                            hasNotificationAccess = hasNotificationAccess,
                            hasCalendarAccess = hasCalendarAccess,
                            onRequestDefaultHome = onRequestDefaultHome,
                            onRequestUsageAccess = onRequestUsageAccess,
                            onRequestNotificationAccess = onRequestNotificationAccess,
                            onRequestCalendarAccess = onRequestCalendarAccess
                        )
                        else -> OnboardingGesturesStep()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, palette.borderSubtle, RoundedCornerShape(24.dp))
                            .clickable { currentStep-- }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "← back",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.primaryText
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(palette.primaryText)
                        .clickable {
                            if (currentStep < 2) {
                                currentStep++
                            } else {
                                onCompleteOnboarding()
                            }
                        }
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (currentStep < 2) "continue →" else "enter productiveos →",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.background
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomeStep() {
    val palette = LocalProductiveOSPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "welcome to\nproductiveos",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light,
                lineHeight = 42.sp
            ),
            color = palette.primaryText
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "an intentional, calm operating system engineered to reclaim your focus, clarity, and time.",
            style = MaterialTheme.typography.bodyLarge,
            color = palette.secondaryText
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Key Principles List
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PillarRow(
                title = "3-second mindful hold",
                description = "apps pause deliberately before launch to break muscle memory and eliminate reflexive phone checking."
            )
            PillarRow(
                title = "google geometric squircle shapes",
                description = "organic, playful shapes with dynamic monochrome harmonized palette matching your active theme."
            )
            PillarRow(
                title = "real-time google sync",
                description = "live agenda, google tasks, and keep notes seamlessly embedded into your daily dashboard."
            )
            PillarRow(
                title = "circular telemetry & reports",
                description = "daily, weekly, and monthly screen-time reports showing exactly where your attention went."
            )
        }
    }
}

@Composable
private fun OnboardingPermissionsStep(
    isDefaultHome: Boolean,
    hasUsageAccess: Boolean,
    hasNotificationAccess: Boolean,
    hasCalendarAccess: Boolean,
    onRequestDefaultHome: () -> Unit,
    onRequestUsageAccess: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestCalendarAccess: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "permissions at once",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
            color = palette.primaryText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "grant required capabilities together for full screen-time telemetry, calm notifications, and agenda sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PermissionCard(
                title = "default home launcher",
                subtitle = "replace standard addictive launcher with productiveos",
                isGranted = isDefaultHome,
                onClick = onRequestDefaultHome
            )

            PermissionCard(
                title = "usage stats & screen time",
                subtitle = "measures daily unlock count and per-app usage",
                isGranted = hasUsageAccess,
                onClick = onRequestUsageAccess
            )

            PermissionCard(
                title = "notification listener",
                subtitle = "displays filtered calm notifications in your widget gallery",
                isGranted = hasNotificationAccess,
                onClick = onRequestNotificationAccess
            )

            PermissionCard(
                title = "calendar & agenda access",
                subtitle = "queries today's google calendar events in real time",
                isGranted = hasCalendarAccess,
                onClick = onRequestCalendarAccess
            )
        }
    }
}

@Composable
private fun OnboardingGesturesStep() {
    val palette = LocalProductiveOSPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "gesture navigation",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
            color = palette.primaryText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "fluid gestures engineered for immediate access without visual clutter.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GestureCard(
                action = "swipe up ↑",
                title = "app drawer & global search",
                description = "instant access to your full catalog with t9 numerical & keyword search."
            )
            GestureCard(
                action = "swipe right →",
                title = "wellbeing & usage reports",
                description = "circular donut charts, screen-time trends, and OS-level focus scores."
            )
            GestureCard(
                action = "swipe left ←",
                title = "widget gallery",
                description = "pomodoro focus timer, upcoming agenda, google tasks, and keep notes."
            )
            GestureCard(
                action = "hold icon (3s) ⏳",
                title = "mindful launch & quick actions",
                description = "breathe through the glow before opening, or send instant WhatsApp messages."
            )
            GestureCard(
                action = "double tap home ••",
                title = "instant lock screen",
                description = "turn display off immediately without reaching for the physical power button."
            )
        }
    }
}

@Composable
private fun PillarRow(
    title: String,
    description: String
) {
    val palette = LocalProductiveOSPalette.current

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.surface)
            .border(
                1.dp,
                if (isGranted) palette.accent else palette.borderSubtle,
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGranted) palette.accent.copy(alpha = 0.2f) else palette.primaryText)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isGranted) "granted ✓" else "grant →",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isGranted) palette.accent else palette.background
                )
            }
        }
    }
}

@Composable
private fun GestureCard(
    action: String,
    title: String,
    description: String
) {
    val palette = LocalProductiveOSPalette.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface)
            .border(1.dp, palette.borderSubtle, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.primaryText
                )
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accent
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondaryText
            )
        }
    }
}
