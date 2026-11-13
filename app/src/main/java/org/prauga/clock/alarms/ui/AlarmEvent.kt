/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

sealed interface AlarmEvent {
    data class Add(val hour: Int, val minute: Int) : AlarmEvent

    data class SetTime(val id: Long, val hour: Int, val minute: Int) : AlarmEvent

    data class SetEnabled(val id: Long, val enabled: Boolean) : AlarmEvent

    data class SetDay(val id: Long, val calendarDay: Int, val on: Boolean) : AlarmEvent

    data class SetVibrate(val id: Long, val vibrate: Boolean) : AlarmEvent

    data class SetLabel(val id: Long, val label: String) : AlarmEvent

    data class SetDate(val id: Long, val scheduledDate: Long) : AlarmEvent

    data class SetPause(val id: Long, val start: Long, val end: Long) : AlarmEvent

    data class Delete(val id: Long) : AlarmEvent

    data class Predismiss(val id: Long) : AlarmEvent
}
