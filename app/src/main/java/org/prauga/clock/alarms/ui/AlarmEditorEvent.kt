/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

sealed interface AlarmEditorEvent {
    data class SetTime(val hour: Int, val minute: Int) : AlarmEditorEvent

    data class SetLabel(val label: String) : AlarmEditorEvent

    data class SetDay(val calendarDay: Int, val on: Boolean) : AlarmEditorEvent

    data class SetColor(val color: Int) : AlarmEditorEvent

    data class SetVibrate(val vibrate: Boolean) : AlarmEditorEvent

    data class SetDate(val scheduledDate: Long) : AlarmEditorEvent

    data class SetPause(val start: Long, val end: Long) : AlarmEditorEvent

    data object PickSound : AlarmEditorEvent

    data object Save : AlarmEditorEvent

    data object Delete : AlarmEditorEvent
}
