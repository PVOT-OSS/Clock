/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.provider

import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone

/**
 * Calendar dates on top of the weekday schedule: a one-time alarm may be set for a given date, and
 * a repeating alarm may be paused for a range of dates. Dates are stored as epoch days.
 */
object AlarmDates {

    /** Stored when no date is set. */
    const val NONE = -1L

    /** Next time [alarm] should ring after [now], honouring its date and pause range. */
    @JvmStatic
    fun nextAlarmTime(alarm: Alarm, now: Calendar): Calendar {
        if (!alarm.daysOfWeek.isRepeating && alarm.scheduledDate != NONE) {
            val scheduled = at(alarm.scheduledDate, alarm.hour, alarm.minutes, now.timeZone)
            if (scheduled.after(now)) {
                return scheduled
            }
        }
        var next = alarm.getNextWeekdayTime(now)
        if (alarm.daysOfWeek.isRepeating && alarm.pauseStart != NONE) {
            var guard = 0
            while (epochDay(next) in alarm.pauseStart..alarm.pauseEnd && guard++ < 1000) {
                next = alarm.getNextWeekdayTime(next)
            }
        }
        return next
    }

    /** Whether [alarm] has a pause range that has not ended yet. */
    @JvmStatic
    fun isPaused(alarm: Alarm, now: Calendar): Boolean =
        alarm.daysOfWeek.isRepeating && alarm.pauseStart != NONE && epochDay(now) <= alarm.pauseEnd

    @JvmStatic
    fun epochDay(time: Calendar): Long =
        LocalDate.of(
                time.get(Calendar.YEAR),
                time.get(Calendar.MONTH) + 1,
                time.get(Calendar.DAY_OF_MONTH),
            )
            .toEpochDay()

    @JvmStatic
    fun at(epochDay: Long, hour: Int, minute: Int, zone: TimeZone): Calendar {
        val date = LocalDate.ofEpochDay(epochDay)
        return Calendar.getInstance(zone).apply {
            clear()
            set(date.year, date.monthValue - 1, date.dayOfMonth, hour, minute, 0)
        }
    }
}
