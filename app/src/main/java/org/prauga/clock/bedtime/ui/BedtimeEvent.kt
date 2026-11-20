/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

sealed interface BedtimeEvent {
    data object Refresh : BedtimeEvent

    data object FinishIntro : BedtimeEvent

    data class SetEnabled(val enabled: Boolean) : BedtimeEvent

    data class SetDay(val calendarDay: Int, val on: Boolean) : BedtimeEvent

    data class SetTime(val hour: Int, val minute: Int) : BedtimeEvent

    data class SetReminder(val minutes: Int) : BedtimeEvent

    data class SetMode(val enabled: Boolean) : BedtimeEvent

    data class SetWakeUpTime(val hour: Int, val minute: Int) : BedtimeEvent

    data object LoadWakeUpChoices : BedtimeEvent

    data class ChooseWakeUp(val id: Long) : BedtimeEvent

    data class AddWakeUpAlarm(val label: String) : BedtimeEvent

    data class SetSleepSound(val sound: Int) : BedtimeEvent

    data class SetSleepMinutes(val minutes: Int) : BedtimeEvent

    data object ToggleSleepSound : BedtimeEvent

    data object LoadTomorrow : BedtimeEvent

    data class SetSleepGoal(val minutes: Int) : BedtimeEvent
}
