/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.concurrent.Executors
import org.prauga.clock.R
import org.prauga.clock.provider.Alarm

/** "Turn off all alarms": switches every alarm off after a confirmation. */
object AlarmsOff {

    @JvmStatic
    fun confirm(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.turn_off_all_alarms)
            .setMessage(R.string.turn_off_all_alarms_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.turn_off_all_alarms_confirm) { _, _ -> turnOff(context) }
            .show()
    }

    private fun turnOff(context: Context) {
        val app = context.applicationContext
        Executors.newSingleThreadExecutor().execute {
            val alarms = Alarm.getAlarms(app.contentResolver, "enabled=1")
            for (alarm in alarms) {
                alarm.enabled = false
                Alarm.updateAlarm(app.contentResolver, alarm)
                AlarmStateManager.deleteAllInstances(app, alarm.id)
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                        app,
                        app.resources.getQuantityString(
                            R.plurals.alarms_turned_off,
                            alarms.size,
                            alarms.size,
                        ),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
            }
        }
    }
}
