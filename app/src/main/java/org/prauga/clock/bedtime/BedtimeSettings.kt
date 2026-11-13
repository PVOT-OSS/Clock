/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import org.prauga.clock.data.Weekdays

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

    var wakeUpAlarmId: Long
        get() = prefs.getLong(KEY_WAKEUP_ALARM, -1)
        set(value) = prefs.edit().putLong(KEY_WAKEUP_ALARM, value).apply()

    var modeEnabled: Boolean
        get() = prefs.getBoolean(KEY_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_MODE, value).apply()

    var zenRuleId: String?
        get() = prefs.getString(KEY_ZEN_RULE, null)
        set(value) = prefs.edit().putString(KEY_ZEN_RULE, value).apply()

    var sleepSound: Int
        get() = prefs.getInt(KEY_SLEEP_SOUND, 2)
        set(value) = prefs.edit().putInt(KEY_SLEEP_SOUND, value).apply()

    var sleepSoundMinutes: Int
        get() = prefs.getInt(KEY_SLEEP_SOUND_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_SLEEP_SOUND_MINUTES, value).apply()

    var introDone: Boolean
        get() = prefs.getBoolean(KEY_INTRO_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_INTRO_DONE, value).apply()

    var reminderMinutes: Int
        get() = prefs.getInt(KEY_REMINDER, 15)
        set(value) = prefs.edit().putInt(KEY_REMINDER, value).apply()

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
        const val KEY_WAKEUP_ALARM = "bedtime_wakeup_alarm"
        const val KEY_MODE = "bedtime_mode"
        const val KEY_INTRO_DONE = "bedtime_intro_done"
        const val KEY_ZEN_RULE = "bedtime_zen_rule"
        const val KEY_SLEEP_SOUND = "bedtime_sleep_sound"
        const val KEY_SLEEP_SOUND_MINUTES = "bedtime_sleep_sound_minutes"
    }
}
