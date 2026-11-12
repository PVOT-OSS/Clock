/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.Context
import android.text.format.DateUtils
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.prauga.clock.R
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmDates

/**
 * The date row of an expanded alarm: a one-time alarm picks the date it rings on, a repeating alarm
 * picks a range of dates to stay quiet.
 */
object AlarmDatePicker {

    private const val TAG = "alarm_date_picker"
    private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)

    /** Text of the row in the expanded alarm. */
    @JvmStatic
    fun rowText(context: Context, alarm: Alarm): String =
        summary(context, alarm, Calendar.getInstance())
            ?: context.getString(
                if (alarm.daysOfWeek.isRepeating) R.string.alarm_pause
                else R.string.alarm_schedule_date
            )

    /** Date-based description of when the alarm rings, or null to fall back to the weekdays. */
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

    /** Shows the picker for [alarm]; [onChanged] runs after the alarm's dates were changed. */
    @JvmStatic
    fun show(fragment: Fragment, alarm: Alarm, onChanged: Runnable) {
        val today = AlarmDates.epochDay(Calendar.getInstance())
        val constraints =
            CalendarConstraints.Builder()
                .setStart(today * DAY_MILLIS)
                .setValidator(DateValidatorPointForward.from(today * DAY_MILLIS))
                .build()
        if (alarm.daysOfWeek.isRepeating) {
            val paused = alarm.pauseStart != AlarmDates.NONE
            val picker =
                MaterialDatePicker.Builder.dateRangePicker()
                    // A dialog, not the full-screen default, so "Resume" has a button.
                    .setTheme(
                        com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar
                    )
                    .setTitleText(R.string.alarm_pause_title)
                    .setCalendarConstraints(constraints)
                    .apply {
                        if (paused) {
                            setSelection(
                                Pair(alarm.pauseStart * DAY_MILLIS, alarm.pauseEnd * DAY_MILLIS)
                            )
                            setNegativeButtonText(R.string.alarm_pause_resume)
                        }
                    }
                    .build()
            picker.addOnPositiveButtonClickListener { range ->
                alarm.pauseStart = range.first / DAY_MILLIS
                alarm.pauseEnd = range.second / DAY_MILLIS
                alarm.enabled = true
                onChanged.run()
            }
            if (paused) {
                picker.addOnNegativeButtonClickListener {
                    alarm.pauseStart = AlarmDates.NONE
                    alarm.pauseEnd = AlarmDates.NONE
                    onChanged.run()
                }
            }
            picker.show(fragment.childFragmentManager, TAG)
        } else {
            val dated = alarm.scheduledDate != AlarmDates.NONE
            val picker =
                MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.alarm_schedule_date_title)
                    .setCalendarConstraints(constraints)
                    .setSelection(
                        (if (dated) maxOf(alarm.scheduledDate, today) else today) * DAY_MILLIS
                    )
                    .apply { if (dated) setNegativeButtonText(R.string.alarm_date_remove) }
                    .build()
            picker.addOnPositiveButtonClickListener { millis ->
                alarm.scheduledDate = millis / DAY_MILLIS
                alarm.enabled = true
                onChanged.run()
            }
            if (dated) {
                picker.addOnNegativeButtonClickListener {
                    alarm.scheduledDate = AlarmDates.NONE
                    onChanged.run()
                }
            }
            picker.show(fragment.childFragmentManager, TAG)
        }
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
