/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.data

data class BedtimeSnapshot(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val days: Int,
    val reminderMinutes: Int,
    val introDone: Boolean,
    val modeEnabled: Boolean,
    val hasModeAccess: Boolean,
    val wakeUp: WakeUpAlarm?,
    val sleepSound: Int,
    val sleepSoundMinutes: Int,
    val sleepSoundPlaying: Boolean,
)
