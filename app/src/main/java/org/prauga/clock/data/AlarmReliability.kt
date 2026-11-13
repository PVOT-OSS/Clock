/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.data

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View

object AlarmReliability {

    @JvmStatic
    fun canUseFullScreenIntent(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    @JvmField
    val openFullScreenIntentSettings =
        View.OnClickListener { v ->
            openAppSetting(v.context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
        }

    // USE_EXACT_ALARM grants this, but some ROMs still deny it.
    @JvmStatic
    fun canScheduleExactAlarms(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    @JvmField
    val openExactAlarmSettings =
        View.OnClickListener { v ->
            openAppSetting(v.context, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }

    @JvmStatic
    fun isBackgroundRestricted(context: Context): Boolean =
        context.getSystemService(ActivityManager::class.java).isBackgroundRestricted

    @JvmField
    val openAppDetails =
        View.OnClickListener { v ->
            openAppSetting(v.context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }

    @JvmStatic
    fun isAlarmVolumeLow(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        val volume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        return volume > 0 && volume * 4 < audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    }

    @JvmField
    val increaseAlarmVolume =
        View.OnClickListener { v ->
            val audio = v.context.getSystemService(AudioManager::class.java)
            val index = Math.round(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM) * 11f / 16f)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, index, AudioManager.FLAG_SHOW_UI)
        }

    private fun openAppSetting(context: Context, action: String) {
        val intent =
            Intent(action)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(intent.data)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
