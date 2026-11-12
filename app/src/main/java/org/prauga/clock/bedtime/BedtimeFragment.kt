/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar
import org.prauga.clock.DeskClockFragment
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.uidata.UiDataModel

/** The Bedtime tab: when to go to bed, and a reminder before it. */
class BedtimeFragment : DeskClockFragment(UiDataModel.Tab.BEDTIME) {

    private lateinit var settings: BedtimeSettings
    private lateinit var enabled: MaterialSwitch
    private lateinit var time: TextView
    private lateinit var dayButtons: List<CompoundButton>
    private lateinit var reminder: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        settings = BedtimeSettings(requireContext())
        val view = inflater.inflate(R.layout.bedtime_fragment, container, false)
        enabled = view.findViewById(R.id.bedtime_enabled)
        time = view.findViewById(R.id.bedtime_time)
        dayButtons = DAY_BUTTONS.map { view.findViewById<CompoundButton>(it) }
        reminder = view.findViewById(R.id.bedtime_reminder)

        enabled.setOnCheckedChangeListener { _, checked ->
            if (checked != settings.enabled) {
                settings.enabled = checked
                onScheduleChanged()
            }
        }
        time.setOnClickListener { pickTime() }
        reminder.setOnClickListener { pickReminder() }
        buildDayChips()
        bind()
        return view
    }

    private fun buildDayChips() {
        val order = DataModel.getDataModel().weekdayOrder.calendarDays
        dayButtons.forEachIndexed { i, button ->
            val calendarDay = order[i]
            button.text = UiDataModel.getUiDataModel().getShortWeekday(calendarDay)
            button.contentDescription = UiDataModel.getUiDataModel().getLongWeekday(calendarDay)
            button.tag = calendarDay
            button.setOnClickListener {
                settings.days = settings.days.setBit(calendarDay, button.isChecked)
                onScheduleChanged()
            }
        }
    }

    private fun bind() {
        enabled.isChecked = settings.enabled
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.hour)
                set(Calendar.MINUTE, settings.minute)
            }
        time.text = DateFormat.getTimeFormat(requireContext()).format(calendar.time)
        val chosen = settings.days
        dayButtons.forEach { it.isChecked = chosen.isBitOn(it.tag as Int) }
        reminder.text =
            if (settings.reminderMinutes == 0) getString(R.string.bedtime_reminder_off)
            else
                resources.getQuantityString(
                    R.plurals.bedtime_reminder_before,
                    settings.reminderMinutes,
                    settings.reminderMinutes,
                )
    }

    private fun pickTime() {
        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(
                    if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H
                    else TimeFormat.CLOCK_12H
                )
                .setHour(settings.hour)
                .setMinute(settings.minute)
                .setTitleText(R.string.bedtime_schedule_title)
                .build()
        picker.addOnPositiveButtonClickListener {
            settings.hour = picker.hour
            settings.minute = picker.minute
            settings.enabled = true
            onScheduleChanged()
        }
        picker.show(childFragmentManager, "bedtime_time")
    }

    private fun pickReminder() {
        val choices = intArrayOf(0, 15, 30, 45, 60)
        val labels =
            choices
                .map {
                    if (it == 0) getString(R.string.bedtime_reminder_off)
                    else resources.getQuantityString(R.plurals.bedtime_reminder_before, it, it)
                }
                .toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.bedtime_reminder_title)
            .setSingleChoiceItems(labels, choices.indexOf(settings.reminderMinutes)) { dialog, i ->
                settings.reminderMinutes = choices[i]
                onScheduleChanged()
                dialog.dismiss()
            }
            .show()
    }

    private fun onScheduleChanged() {
        bind()
    }

    override fun getFabTargetVisibility(): Int = View.INVISIBLE

    private companion object {
        val DAY_BUTTONS =
            intArrayOf(
                R.id.day_button_0,
                R.id.day_button_1,
                R.id.day_button_2,
                R.id.day_button_3,
                R.id.day_button_4,
                R.id.day_button_5,
                R.id.day_button_6,
            )
    }

    override fun onFabClick(fab: ImageView) {}

    override fun onUpdateFab(fab: ImageView) {
        fab.visibility = View.INVISIBLE
    }

    override fun onUpdateFabButtons(left: ImageView, right: ImageView) {
        left.visibility = View.INVISIBLE
        right.visibility = View.INVISIBLE
    }
}
