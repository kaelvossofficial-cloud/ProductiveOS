package com.productiveos.launcher.data.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

class ProductiveNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
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

