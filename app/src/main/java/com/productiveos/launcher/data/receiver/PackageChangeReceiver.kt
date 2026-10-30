package com.productiveos.launcher.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.productiveos.launcher.data.repository.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appRepository: AppRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action == Intent.ACTION_PACKAGE_ADDED ||
            action == Intent.ACTION_PACKAGE_REMOVED ||
            action == Intent.ACTION_PACKAGE_CHANGED ||
            action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            val pendingResult = goAsync()
            receiverScope.launch {
                try {
                    appRepository.syncInstalledApps()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

