package com.productiveos.launcher.data.service

import android.content.ComponentName
import android.content.Context
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImportantNotificationItem(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val category: String
)

data class ImportantNotificationData(
    val hasPermission: Boolean = false,
    val totalCount: Int = 0,
    val messagesCount: Int = 0,
    val emailCount: Int = 0,
    val calendarCount: Int = 0,
    val callCount: Int = 0,
    val recentNotifications: List<ImportantNotificationItem> = emptyList()
)

data class QuickNotification(
    val key: String,
    val title: String,
    val text: String,
    val canReply: Boolean
)

class ProductiveNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        updateNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance === this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    private fun updateNotifications() {
        runCatching {
            val active = activeNotifications ?: emptyArray()
            val importantList = mutableListOf<ImportantNotificationItem>()

            var messages = 0
            var email = 0
            var calendar = 0
            var call = 0

            active.forEach { sbn ->
                if (sbn.isOngoing) return@forEach // Ignore persistent background services/media
                val pkg = sbn.packageName.lowercase()
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
                val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""

                val category = when {
                    pkg.contains("message") || pkg.contains("sms") || pkg.contains("whatsapp") ||
                        pkg.contains("telegram") || pkg.contains("signal") || pkg.contains("slack") ||
                        pkg.contains("discord") -> {
                        messages++
                        "messages"
                    }
                    pkg.contains("gmail") || pkg.contains("mail") || pkg.contains("outlook") -> {
                        email++
                        "email"
                    }
                    pkg.contains("calendar") || pkg.contains("task") || pkg.contains("reminder") -> {
                        calendar++
                        "calendar"
                    }
                    pkg.contains("dialer") || pkg.contains("phone") || pkg.contains("telecom") -> {
                        call++
                        "call"
                    }
                    else -> null
                }

                if (category != null && (title.isNotBlank() || text.isNotBlank())) {
                    importantList.add(
                        ImportantNotificationItem(
                            key = sbn.key,
                            packageName = sbn.packageName,
                            title = title,
                            text = text,
                            timestamp = sbn.postTime,
                            category = category
                        )
                    )
                }
            }

            _notificationsFlow.value = ImportantNotificationData(
                hasPermission = true,
                totalCount = messages + email + calendar + call,
                messagesCount = messages,
                emailCount = email,
                calendarCount = calendar,
                callCount = call,
                recentNotifications = importantList.take(6)
            )
        }
    }

    companion object {
        @Volatile
        private var instance: ProductiveNotificationListenerService? = null

        /** Live notifications for one package (used by the quick action capsule). */
        fun notificationsForPackage(packageName: String): List<QuickNotification> {
            val service = instance ?: return emptyList()
            return runCatching {
                (service.activeNotifications ?: emptyArray())
                    .filter { it.packageName == packageName && !it.isOngoing }
                    .sortedByDescending { it.postTime }
                    .mapNotNull { sbn ->
                        val extras = sbn.notification.extras
                        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
                        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
                        if (title.isBlank() && text.isBlank()) return@mapNotNull null
                        val canReply = sbn.notification.actions?.any { a ->
                            a.remoteInputs?.isNotEmpty() == true
                        } == true
                        QuickNotification(sbn.key, title, text, canReply)
                    }
                    .take(4)
            }.getOrDefault(emptyList())
        }

        /** Sends [message] through the notification's own inline-reply action without opening the app. */
        fun sendInlineReply(context: Context, key: String, message: String): Boolean {
            val service = instance ?: return false
            if (message.isBlank()) return false
            return runCatching {
                val sbn = service.activeNotifications?.firstOrNull { it.key == key } ?: return false
                val action = sbn.notification.actions?.firstOrNull { a ->
                    a.remoteInputs?.isNotEmpty() == true
                } ?: return false
                val remoteInputs = action.remoteInputs ?: return false
                val fillIn = Intent()
                val results = Bundle()
                remoteInputs.forEach { results.putCharSequence(it.resultKey, message) }
                RemoteInput.addResultsToIntent(remoteInputs, fillIn, results)
                action.actionIntent.send(context, 0, fillIn)
                true
            }.getOrDefault(false)
        }
        private val _notificationsFlow = MutableStateFlow(ImportantNotificationData())
        val notificationsFlow: StateFlow<ImportantNotificationData> = _notificationsFlow.asStateFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            return runCatching {
                val flat = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ) ?: ""
                val cn = ComponentName(context, ProductiveNotificationListenerService::class.java)
                flat.contains(cn.flattenToString()) || flat.contains(context.packageName)
            }.getOrDefault(false)
        }

        fun openNotificationAccessSettings(context: Context) {
            runCatching {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}

