/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.prauga.clock.provider.AlarmInstance

/**
 * Receives the alarm instance state changes scheduled with AlarmManager. An exact alarm's broadcast
 * may start a foreground service from the background, which a service PendingIntent may not when
 * Clock is not a system app, so only the change to firing hands over to [AlarmService]; every other
 * change is applied here.
 */
class AlarmStateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(AlarmStateManager.ALARM_STATE_EXTRA, -1)
        if (state == AlarmInstance.FIRED_STATE) {
            ContextCompat.startForegroundService(
                context,
                Intent(intent).setClass(context, AlarmService::class.java),
            )
        } else {
            AlarmStateManager.handleIntent(context, intent)
        }
    }
}
