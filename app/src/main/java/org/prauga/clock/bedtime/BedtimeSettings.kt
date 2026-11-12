/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import org.prauga.clock.data.Weekdays

/** The bedtime schedule, kept in device-protected preferences next to the other settings. */
class BedtimeSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.createDeviceProtectedStorageContext().let {
            it.getSharedPreferences("${it.packageName}_preferences", Context.MODE_PRIVATE)
        }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var hour: Int
        get() = prefs.getInt(KEY_HOUR, 22)
        set(value) = prefs.edit().putInt(KEY_HOUR, value).apply()

    var minute: Int
        get() = prefs.getInt(KEY_MINUTE, 30)
        set(value) = prefs.edit().putInt(KEY_MINUTE, value).apply()

    var days: Weekdays
        get() = Weekdays.fromBits(prefs.getInt(KEY_DAYS, ALL_DAYS))
        set(value) = prefs.edit().putInt(KEY_DAYS, value.bits).apply()

    /** Minutes before bedtime to remind; 0 is off. */
    var reminderMinutes: Int
        get() = prefs.getInt(KEY_REMINDER, 15)
        set(value) = prefs.edit().putInt(KEY_REMINDER, value).apply()

    /** The next bedtime after [now] on one of the chosen days, or null when off. */
    fun nextBedtime(now: Calendar = Calendar.getInstance()): Calendar? {
        if (!enabled || !days.isRepeating) {
            return null
        }
        val next =
            (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
        next.add(Calendar.DAY_OF_YEAR, days.getDistanceToNextDay(next))
        next.set(Calendar.HOUR_OF_DAY, hour)
        next.set(Calendar.MINUTE, minute)
        return next
    }

    private companion object {
        const val KEY_ENABLED = "bedtime_enabled"
        const val KEY_HOUR = "bedtime_hour"
        const val KEY_MINUTE = "bedtime_minute"
        const val KEY_DAYS = "bedtime_days"
        const val KEY_REMINDER = "bedtime_reminder_minutes"
        const val ALL_DAYS = 0x7F
    }
}
