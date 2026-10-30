package com.productiveos.launcher.util

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings

object SystemLauncherHelper {

    fun isDefaultHomeLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    return true
                }
            }
        }
        return runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName == context.packageName
        }.getOrDefault(false)
    }

    fun createRequestDefaultLauncherIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (
                roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun openHomeSettingsPicker(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    fun launchPackage(context: Context, packageName: String): Boolean {
        return runCatching {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                context.startActivity(launchIntent)
                return@runCatching true
            }

            val launcherApps =
                context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val activityList =
                launcherApps?.getActivityList(packageName, Process.myUserHandle()).orEmpty()
            val firstActivity = activityList.firstOrNull()
            if (firstActivity != null && launcherApps != null) {
                launcherApps.startMainActivity(
                    firstActivity.componentName,
                    Process.myUserHandle(),
                    null,
                    null
                )
                return@runCatching true
            }
            false
        }.getOrDefault(false)
    }

    fun openClockApp(context: Context) {
        runCatching {
            val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(alarmIntent)
        }.onFailure {
            val fallbackClockPackages = listOf(
                "com.google.android.deskclock",
                "com.android.deskclock",
                "com.sec.android.app.clockpackage",
                "com.oneplus.deskclock"
            )
            for (pkg in fallbackClockPackages) {
                if (launchPackage(context, pkg)) return
            }
        }
    }

    fun openCalendarApp(context: Context) {
        runCatching {
            val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            ContentUris.appendId(builder, System.currentTimeMillis())
            val calendarIntent = Intent(Intent.ACTION_VIEW)
                .setData(builder.build())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(calendarIntent)
        }.onFailure {
            runCatching {
                val categoryIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(categoryIntent)
            }
        }
    }

    fun openAppInfo(context: Context, packageName: String) {
        runCatching {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun requestUninstallApp(context: Context, packageName: String) {
        runCatching {
            val intent = Intent(
                Intent.ACTION_DELETE,
                Uri.fromParts("package", packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            openAppInfo(context, packageName)
        }
    }

    fun openUsageAccessSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }.onFailure {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandNotificationShade(context: Context) {
        runCatching {
            val statusBarService = context.getSystemService("statusbar") ?: return
            val statusBarManagerClass = Class.forName("android.app.StatusBarManager")
            val expandMethod = statusBarManagerClass.getMethod("expandNotificationsPanel")
            expandMethod.isAccessible = true
            expandMethod.invoke(statusBarService)
        }
    }

    fun performSubtleHaptic(context: Context) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    )
                } else {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(12L, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                }
            }
        }
    }
}

