package com.productiveos.launcher.data.admin

import android.app.admin.DeviceAdminReceiver

/** Device-admin receiver used solely for the "double tap to lock screen" feature (force-lock policy). */
class LockScreenAdminReceiver : DeviceAdminReceiver()
