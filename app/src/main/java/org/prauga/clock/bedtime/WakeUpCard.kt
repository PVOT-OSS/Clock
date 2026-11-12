/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar
import java.util.concurrent.Executors
import org.prauga.clock.R
import org.prauga.clock.alarms.AlarmUpdateHandler
import org.prauga.clock.alarms.ScrollHandler
import org.prauga.clock.data.DataModel
import org.prauga.clock.provider.Alarm

/** The wake-up card: one of the user's alarms, linked to bedtime to show the sleep duration. */
class WakeUpCard(
    private val fragment: Fragment,
    root: ViewGroup,
    private val settings: BedtimeSettings,
) {
    private val context = root.context
    private val time: TextView = root.findViewById(R.id.wakeup_time)
    private val summary: TextView = root.findViewById(R.id.wakeup_summary)
    private var alarm: Alarm? = null
    private val updater =
        AlarmUpdateHandler(
            context,
            object : ScrollHandler {
                // Reports the id of a newly added alarm, which becomes the wake-up alarm.
                override fun setSmoothScrollStableId(stableId: Long) {
                    settings.wakeUpAlarmId = stableId
                    Handler(Looper.getMainLooper()).post { load() }
                }

                override fun smoothScrollTo(position: Int) {}
            },
            root,
        )

    init {
        time.setOnClickListener { pickTime() }
        root.findViewById<View>(R.id.wakeup_choose).setOnClickListener { chooseAlarm() }
    }

    /** Reads the linked alarm and redraws the card. */
    fun load() {
        val id = settings.wakeUpAlarmId
        Executors.newSingleThreadExecutor().execute {
            val found = if (id < 0) null else Alarm.getAlarm(context.contentResolver, id)
            Handler(Looper.getMainLooper()).post {
                alarm = found
                bind()
            }
        }
    }

    fun bind() {
        val linked = alarm
        if (linked == null) {
            time.setText(R.string.wakeup_not_set)
            summary.setText(R.string.wakeup_choose_hint)
            return
        }
        time.text = format(linked.hour, linked.minutes)
        val order = DataModel.getDataModel().weekdayOrder
        val days =
            if (linked.daysOfWeek.isRepeating) linked.daysOfWeek.toString(context, order)
            else context.getString(R.string.alarm_tomorrow)
        val sleep = sleepMinutes(linked)
        val parts = mutableListOf(days)
        if (!linked.enabled) parts += context.getString(R.string.wakeup_alarm_off)
        else if (settings.enabled)
            parts +=
                context.getString(
                    R.string.wakeup_sleep_duration,
                    context.resources.getQuantityString(R.plurals.hours, sleep / 60, sleep / 60),
                    context.resources.getQuantityString(R.plurals.minutes, sleep % 60, sleep % 60),
                )
        summary.text = parts.joinToString(" · ")
    }

    private fun sleepMinutes(alarm: Alarm): Int {
        val bed = settings.hour * 60 + settings.minute
        val wake = alarm.hour * 60 + alarm.minutes
        return ((wake - bed) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
    }

    private fun pickTime() {
        val linked = alarm ?: return chooseAlarm()
        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(
                    if (DateFormat.is24HourFormat(context)) TimeFormat.CLOCK_24H
                    else TimeFormat.CLOCK_12H
                )
                .setHour(linked.hour)
                .setMinute(linked.minutes)
                .setTitleText(R.string.wakeup_title)
                .build()
        picker.addOnPositiveButtonClickListener {
            linked.hour = picker.hour
            linked.minutes = picker.minute
            linked.enabled = true
            updater.asyncUpdateAlarm(linked, true, false)
            bind()
        }
        picker.show(fragment.childFragmentManager, "wakeup_time")
    }

    private fun chooseAlarm() {
        Executors.newSingleThreadExecutor().execute {
            val alarms = Alarm.getAlarms(context.contentResolver, null)
            Handler(Looper.getMainLooper()).post { showChoices(alarms) }
        }
    }

    private fun showChoices(alarms: List<Alarm>) {
        val order = DataModel.getDataModel().weekdayOrder
        val labels =
            alarms.map { a ->
                val days =
                    if (a.daysOfWeek.isRepeating) a.daysOfWeek.toString(context, order)
                    else context.getString(R.string.wakeup_once)
                "${format(a.hour, a.minutes)} · $days"
            } + context.getString(R.string.wakeup_new_alarm)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.wakeup_title)
            .setItems(labels.toTypedArray()) { _, i ->
                if (i < alarms.size) {
                    settings.wakeUpAlarmId = alarms[i].id
                    load()
                } else {
                    // A new alarm on the bedtime days, eight hours after bedtime.
                    val wake = (settings.hour * 60 + settings.minute + 8 * 60) % MINUTES_PER_DAY
                    val alarm =
                        Alarm(wake / 60, wake % 60).apply {
                            daysOfWeek = settings.days
                            enabled = true
                            label = context.getString(R.string.wakeup_title)
                        }
                    updater.asyncAddAlarm(alarm)
                }
            }
            .show()
    }

    private fun format(hour: Int, minute: Int): String {
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
