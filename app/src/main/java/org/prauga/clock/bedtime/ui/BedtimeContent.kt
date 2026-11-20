/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import org.prauga.clock.bedtime.data.BedtimeSnapshot
import org.prauga.clock.bedtime.data.WakeUpAlarm

data class BedtimeContent(
    val bedtime: BedtimeSnapshot,
    val wakeUpChoices: List<WakeUpAlarm>?,
    val tomorrow: String?,
) {
    val bedMinutes: Int
        get() = minuteOfDay(bedtime.hour, bedtime.minute)

    val wakeMinutes: Int
        get() =
            bedtime.wakeUp?.let { minuteOfDay(it.hour, it.minute) }
                ?: wrapDay(bedMinutes + bedtime.sleepGoalMinutes)

    val goalStatus: SleepGoalStatus
        get() = sleepGoalStatus(sleepLength(bedMinutes, wakeMinutes), bedtime.sleepGoalMinutes)
}
