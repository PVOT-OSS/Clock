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

/** Device states, beyond silence, that keep alarms from ringing the way they should. */
object AlarmReliability {

    /** Whether alarms may launch their full-screen ringing activity (revocable from API 34). */
    @JvmStatic
    fun canUseFullScreenIntent(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    @JvmField
    val openFullScreenIntentSettings =
        View.OnClickListener { v ->
            openAppSetting(v.context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
        }

    /** Whether exact alarms are allowed; USE_EXACT_ALARM grants it, but a ROM may still deny. */
    @JvmStatic
    fun canScheduleExactAlarms(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    @JvmField
    val openExactAlarmSettings =
        View.OnClickListener { v ->
            openAppSetting(v.context, Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        }

    /** Whether the user restricted the app's background work in its battery settings. */
    @JvmStatic
    fun isBackgroundRestricted(context: Context): Boolean =
        context.getSystemService(ActivityManager::class.java).isBackgroundRestricted

    @JvmField
    val openAppDetails =
        View.OnClickListener { v ->
            openAppSetting(v.context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }

    /** Whether the alarm stream is audible but below a quarter of its range. */
    @JvmStatic
    fun isAlarmVolumeLow(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java)
        val volume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        return volume > 0 && volume * 4 < audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    }

    /** Raises the alarm stream to 11/16 of its range (a fresh install's level) and shows it. */
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
