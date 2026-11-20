/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import android.net.Uri
import org.prauga.clock.data.Weekdays
import org.prauga.clock.provider.Alarm

data class AlarmDraft(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String,
    val days: Int,
    val color: Int,
    val vibrate: Boolean,
    val alert: Uri?,
    val scheduledDate: Long,
    val pauseStart: Long,
    val pauseEnd: Long,
    val groupId: Long,
) {
    val isNew: Boolean
        get() = id == Alarm.INVALID_ID

    constructor(
        alarm: Alarm
    ) : this(
        alarm.id,
        alarm.hour,
        alarm.minutes,
        alarm.label.orEmpty(),
        alarm.daysOfWeek.bits,
        alarm.color,
        alarm.vibrate,
        alarm.alert,
        alarm.scheduledDate,
        alarm.pauseStart,
        alarm.pauseEnd,
        alarm.groupId,
    )

    fun applyTo(alarm: Alarm) {
        alarm.hour = hour
        alarm.minutes = minute
        alarm.label = label
        alarm.daysOfWeek = Weekdays.fromBits(days)
        alarm.color = color
        alarm.vibrate = vibrate
        alarm.scheduledDate = scheduledDate
        alarm.pauseStart = pauseStart
        alarm.pauseEnd = pauseEnd
        alarm.groupId = groupId
        alarm.enabled = true
    }

    fun toAlarm(): Alarm =
        Alarm(hour, minute).also {
            it.id = id
            it.alert = alert
            applyTo(it)
        }
}
