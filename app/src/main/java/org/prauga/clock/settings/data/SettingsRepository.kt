/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.data

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import org.prauga.clock.RingtonePreviewKlaxon
import org.prauga.clock.alarms.AlarmsOff
import org.prauga.clock.alarms.Sunrise

class SettingsRepository(context: Context) {

    private val app = context.applicationContext
    private val prefs = settingsPrefs(app)
    private val audio = app.getSystemService(AudioManager::class.java)

    val preferences: Flow<Map<String, Any?>> =
        callbackFlow {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> trySend(p.all) }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                trySend(prefs.all)
                awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            .conflate()

    val alarmVolume: Flow<AlarmVolume> =
        callbackFlow {
                val observer =
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            trySend(currentVolume())
                        }
                    }
                app.contentResolver.registerContentObserver(
                    Settings.System.CONTENT_URI,
                    true,
                    observer,
                )
                trySend(currentVolume())
                awaitClose { app.contentResolver.unregisterContentObserver(observer) }
            }
            .conflate()

    val hasMotionSensor: Boolean
        get() =
            app.getSystemService(SensorManager::class.java)
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    val hasVibrator: Boolean
        get() = app.getSystemService(Vibrator::class.java)?.hasVibrator() == true

    fun putString(key: String, value: String) {
        prefs.edit { putString(key, value) }
        if (key == Sunrise.KEY_MINUTES) Sunrise.onSettingChanged(app, value)
    }

    fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }

    fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }

    fun setAlarmVolume(volume: Int) = audio.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)

    fun startAlarmPreview(uri: android.net.Uri) = RingtonePreviewKlaxon.start(app, uri)

    fun stopAlarmPreview() = RingtonePreviewKlaxon.stop(app)

    fun turnOffAllAlarms() = AlarmsOff.turnOff(app)

    private fun currentVolume() =
        AlarmVolume(
            current = audio.getStreamVolume(AudioManager.STREAM_ALARM),
            min = audio.getStreamMinVolume(AudioManager.STREAM_ALARM),
            max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            adjustable =
                app.getSystemService(NotificationManager::class.java).currentInterruptionFilter !=
                    NotificationManager.INTERRUPTION_FILTER_NONE,
        )

    companion object {
        // Device-protected so alarms can read the settings before the first unlock.
        fun settingsPrefs(context: Context): SharedPreferences {
            val storage = context.createDeviceProtectedStorageContext()
            return storage.getSharedPreferences(
                "${storage.packageName}_preferences",
                Context.MODE_PRIVATE,
            )
        }
    }
}
