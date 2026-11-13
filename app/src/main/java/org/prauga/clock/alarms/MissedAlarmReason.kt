/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.prauga.clock.R

object MissedAlarmReason {

    @Volatile private var reason = 0

    @JvmStatic
    fun begin(action: String?) {
        reason =
            when (action) {
                Intent.ACTION_LOCKED_BOOT_COMPLETED,
                Intent.ACTION_BOOT_COMPLETED -> R.string.alarm_missed_phone_off
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> R.string.alarm_missed_time_changed
                else -> 0
            }
    }

    @JvmStatic
    fun end() {
        reason = 0
    }

    @JvmStatic
    fun apply(context: Context, builder: NotificationCompat.Builder, text: CharSequence) {
        val current = reason
        if (current == 0) {
            return
        }
        builder.setStyle(
            NotificationCompat.BigTextStyle().bigText("$text\n${context.getString(current)}")
        )
    }
}
