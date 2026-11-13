/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.Context
import android.text.format.DateUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.prauga.clock.R
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmDates

object AlarmDatePicker {

    private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)

    @JvmStatic
    fun rowText(context: Context, alarm: Alarm): String =
        summary(context, alarm, Calendar.getInstance())
            ?: context.getString(
                if (alarm.daysOfWeek.isRepeating) R.string.alarm_pause
                else R.string.alarm_schedule_date
            )

    @JvmStatic
    fun summary(context: Context, alarm: Alarm, now: Calendar): String? =
        when {
            AlarmDates.isPaused(alarm, now) ->
                context.getString(
                    R.string.alarm_paused_range,
                    format(context, alarm.pauseStart),
                    format(context, alarm.pauseEnd),
                )
            !alarm.daysOfWeek.isRepeating &&
                alarm.scheduledDate != AlarmDates.NONE &&
                alarm.scheduledDate >= AlarmDates.epochDay(now) ->
                format(context, alarm.scheduledDate)
            else -> null
        }

    private fun format(context: Context, epochDay: Long): String =
        DateUtils.formatDateTime(
            context,
            epochDay * DAY_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_ABBREV_ALL or
                DateUtils.FORMAT_NO_YEAR or
                DateUtils.FORMAT_UTC,
        )
}
