/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import android.net.Uri
import org.prauga.clock.alarms.data.AlarmRecord
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmInstance

// Alarm.equals compares ids only; the copied fields make an edited alarm a different item.
data class AlarmItem(
    val alarm: Alarm,
    val instance: AlarmInstance?,
    val hour: Int,
    val minutes: Int,
    val enabled: Boolean,
    val days: Int,
    val label: String,
    val color: Int,
    val alert: Uri?,
    val scheduledDate: Long,
    val pauseStart: Long,
    val pauseEnd: Long,
    val instanceState: Int,
    val groupId: Long,
) {
    val id: Long
        get() = alarm.id

    constructor(
        record: AlarmRecord
    ) : this(
        record.alarm,
        record.instance,
        record.alarm.hour,
        record.alarm.minutes,
        record.alarm.enabled,
        record.alarm.daysOfWeek.bits,
        record.alarm.label.orEmpty(),
        record.alarm.color,
        record.alarm.alert,
        record.alarm.scheduledDate,
        record.alarm.pauseStart,
        record.alarm.pauseEnd,
        record.alarm.instanceState,
        record.alarm.groupId,
    )
}
