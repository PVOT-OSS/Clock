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
)
