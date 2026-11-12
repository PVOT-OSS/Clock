/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var wakeUp: WakeUpCard
    private lateinit var mode: MaterialSwitch
    private lateinit var modeSummary: TextView
    private lateinit var sleepSound: SleepSoundCard
    private lateinit var calendar: CalendarCard
    private val calendarPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { calendar.load() }

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
        val intro = view.findViewById<View>(R.id.bedtime_intro)
        intro.visibility = if (settings.introDone) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.bedtime_intro_start).setOnClickListener {
            // Bedtime first, then the alarm that wakes the user.
            pickTime {
                settings.introDone = true
                intro.visibility = View.GONE
                wakeUp.chooseAlarm()
            }
        }
        reminder.setOnClickListener { pickReminder() }
        wakeUp = WakeUpCard(this, view as ViewGroup, settings)
        sleepSound = SleepSoundCard(view, settings)
        calendar = CalendarCard(view, calendarPermission)
        mode = view.findViewById(R.id.bedtime_mode)
        modeSummary = view.findViewById(R.id.bedtime_mode_summary)
        mode.setOnCheckedChangeListener { _, checked -> setModeEnabled(checked) }
        // The settings are the source of truth; restored view state would write stale values.
        enabled.isSaveEnabled = false
        mode.isSaveEnabled = false
        dayButtons.forEach { it.isSaveEnabled = false }
        view.findViewById<View>(R.id.bedtime_mode_row).setOnClickListener {
            if (settings.modeEnabled && !BedtimeMode.hasAccess(requireContext())) {
                requestAccess()
            } else {
                mode.toggle()
            }
        }
        buildDayChips()
        bind()
        return view
    }

    override fun onResume() {
        super.onResume()
        bind()
        wakeUp.load()
        bindMode()
        sleepSound.bind()
        calendar.load()
        BedtimeScheduler.schedule(requireContext())
    }

    private fun setModeEnabled(enabled: Boolean) {
        if (enabled == settings.modeEnabled) {
            return
        }
        settings.modeEnabled = enabled
        if (enabled && !BedtimeMode.hasAccess(requireContext())) {
            requestAccess()
        }
        if (!enabled) {
            BedtimeMode.remove(requireContext())
        }
        bindMode()
        BedtimeScheduler.schedule(requireContext())
    }

    private fun requestAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun bindMode() {
        mode.isChecked = settings.modeEnabled
        modeSummary.setText(
            if (settings.modeEnabled && !BedtimeMode.hasAccess(requireContext()))
                R.string.bedtime_mode_needs_access
            else R.string.bedtime_mode_summary
        )
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

    private fun pickTime(then: () -> Unit = {}) {
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
            then()
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
        wakeUp.bind()
        BedtimeScheduler.schedule(requireContext())
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
