package com.productiveos.launcher.presentation.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.productiveos.launcher.data.local.AppEntity
import com.productiveos.launcher.data.preferences.FavoriteContact
import com.productiveos.launcher.data.service.ProductiveNotificationListenerService
import com.productiveos.launcher.presentation.theme.LocalProductiveOSPalette
import com.productiveos.launcher.presentation.theme.jellyPress
import com.productiveos.launcher.util.MediaSessionHelper
import com.productiveos.launcher.util.SystemLauncherHelper

data class QuickActionSpec(val label: String, val run: (Context) -> Unit)

/** Builds the per-app quick actions shown in the capsule. */
object QuickActionsHelper {

    private fun digits(number: String) = number.filter { it.isDigit() }

    private fun start(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun actionsFor(app: AppEntity, favorites: List<FavoriteContact>): List<QuickActionSpec> {
        val pkg = app.packageName.lowercase()
        val name = app.label.lowercase()
        val actions = mutableListOf<QuickActionSpec>()

        when {
            pkg.contains("whatsapp") -> favorites.forEach { fav ->
                actions += QuickActionSpec("chat ${fav.name.lowercase()}") { ctx ->
                    start(
                        ctx,
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${digits(fav.number)}"))
                            .setPackage(app.packageName)
                    )
                }
            }
            pkg.contains("telegram") -> favorites.forEach { fav ->
                actions += QuickActionSpec("chat ${fav.name.lowercase()}") { ctx ->
                    start(
                        ctx,
                        Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?phone=${digits(fav.number)}"))
                            .setPackage(app.packageName)
                    )
                }
            }
            pkg.contains("messag") || pkg.contains("sms") || name.contains("message") ->
                favorites.forEach { fav ->
                    actions += QuickActionSpec("text ${fav.name.lowercase()}") { ctx ->
                        start(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${fav.number}")))
                    }
                }
            pkg.contains("dialer") || pkg.contains("phone") || name.contains("phone") -> {
                favorites.forEach { fav ->
                    actions += QuickActionSpec("call ${fav.name.lowercase()}") { ctx ->
                        start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${fav.number}")))
                    }
                }
                actions += QuickActionSpec("keypad") { ctx ->
                    start(ctx, Intent(Intent.ACTION_DIAL))
                }
            }
            pkg.contains("camera") || name.contains("camera") -> {
                actions += QuickActionSpec("photo") { ctx ->
                    start(ctx, Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                }
                actions += QuickActionSpec("video") { ctx ->
                    start(ctx, Intent(MediaStore.ACTION_VIDEO_CAPTURE))
                }
            }
            pkg.contains("clock") || name.contains("clock") -> {
                actions += QuickActionSpec("timer") { ctx ->
                    start(ctx, Intent(AlarmClock.ACTION_SHOW_TIMERS))
                }
                actions += QuickActionSpec("alarms") { ctx ->
                    start(ctx, Intent(AlarmClock.ACTION_SHOW_ALARMS))
                }
            }
            pkg.contains("calendar") || name.contains("calendar") -> {
                actions += QuickActionSpec("new event") { ctx ->
                    start(ctx, Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI))
                }
            }
            pkg.contains("gmail") || pkg.contains("mail") -> {
                actions += QuickActionSpec("compose") { ctx ->
                    start(ctx, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")))
                }
            }
            pkg.contains("chrome") || pkg.contains("browser") || pkg.contains("firefox") -> {
                actions += QuickActionSpec("search") { ctx ->
                    start(ctx, Intent(Intent.ACTION_WEB_SEARCH))
                }
            }
            pkg.contains("maps") || name.contains("maps") -> {
                actions += QuickActionSpec("nearby") { ctx ->
                    start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=nearby")))
                }
            }
            pkg.contains("spotify") || pkg.contains("music") || pkg.contains("youtube.music") -> {
                actions += QuickActionSpec("play / pause") { ctx -> MediaSessionHelper.playPause(ctx) }
                actions += QuickActionSpec("next") { ctx -> MediaSessionHelper.next(ctx) }
                actions += QuickActionSpec("previous") { ctx -> MediaSessionHelper.previous(ctx) }
            }
        }

        actions += QuickActionSpec("app info") { ctx ->
            SystemLauncherHelper.openAppInfo(ctx, app.packageName)
        }
        return actions
    }
}

private val AboveAnchorPositioner = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = 24
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
        val above = anchorBounds.top - popupContentSize.height - 16
        val y = if (above >= margin) above
        else (anchorBounds.bottom + 16)
            .coerceAtMost((windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin))
        return IntOffset(x, y)
    }
}

/**
 * Small plain capsule of quick actions for one app: contextual shortcuts, favorite-contact chats,
 * live notifications and inline replies — all without opening the app.
 */
@Composable
fun QuickActionPopup(
    app: AppEntity,
    favorites: List<FavoriteContact>,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val palette = LocalProductiveOSPalette.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val notificationTick by ProductiveNotificationListenerService.notificationsFlow.collectAsState()
    val notifications = remember(notificationTick, app.packageName) {
        ProductiveNotificationListenerService.notificationsForPackage(app.packageName)
    }
    val actions = remember(app.packageName, favorites) {
        QuickActionsHelper.actionsFor(app, favorites)
    }
    val hasAccess = remember(notificationTick) {
        ProductiveNotificationListenerService.isNotificationAccessGranted(context)
    }

    var replyKey by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var sentNotice by remember { mutableStateOf<String?>(null) }

    Popup(
        popupPositionProvider = AboveAnchorPositioner,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .width(270.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(palette.surfaceElevated)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.displayName.lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText
                )
                Text(
                    text = "open →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primaryText,
                    modifier = Modifier
                        .jellyPress(0.94f)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenApp()
                        }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions.forEach { action ->
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryText,
                        modifier = Modifier
                            .jellyPress(0.94f)
                            .clip(RoundedCornerShape(50))
                            .background(palette.surface)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                action.run(context)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                Text(
                    text = "more",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    modifier = Modifier
                        .jellyPress(0.94f)
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .clickable {
                            onMoreOptions()
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            val isMessagingApp = actions.any { it.label.startsWith("chat ") || it.label.startsWith("text ") } ||
                notifications.any { it.canReply }
            if (notifications.isNotEmpty() || isMessagingApp || !hasAccess) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!hasAccess) {
                Text(
                    text = "allow notification access to see & reply here →",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText,
                    modifier = Modifier.clickable {
                        ProductiveNotificationListenerService.openNotificationAccessSettings(context)
                        onDismiss()
                    }
                )
            } else if (notifications.isEmpty() && isMessagingApp) {
                Text(
                    text = "no new messages",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.mutedText
                )
            }

            notifications.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                        .clickable(enabled = item.canReply) {
                            replyKey = if (replyKey == item.key) null else item.key
                            replyText = ""
                            sentNotice = null
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = item.title.lowercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primaryText,
                        maxLines = 1
                    )
                    if (item.text.isNotBlank()) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText,
                            maxLines = 2
                        )
                    }
                    if (item.canReply && replyKey != item.key) {
                        Text(
                            text = "tap to reply",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.mutedText
                        )
                    }
                    if (replyKey == item.key) {
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.primaryText),
                            cursorBrush = SolidColor(palette.primaryText),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (replyText.isEmpty()) {
                                    Text(
                                        text = "type a reply…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.mutedText
                                    )
                                }
                                inner()
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "send",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.primaryText,
                            modifier = Modifier
                                .jellyPress(0.94f)
                                .clip(RoundedCornerShape(50))
                                .background(palette.surfaceElevated)
                                .clickable {
                                    val ok = ProductiveNotificationListenerService
                                        .sendInlineReply(context, item.key, replyText)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    sentNotice = if (ok) "sent" else "could not send"
                                    if (ok) {
                                        replyText = ""
                                        replyKey = null
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            sentNotice?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondaryText
                )
            }
        }
    }
}
