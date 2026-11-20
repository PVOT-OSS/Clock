/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

data class AlarmsContent(val wakeUp: AlarmItem?, val others: List<AlarmItem>) {
    val isEmpty: Boolean
        get() = wakeUp == null && others.isEmpty()
}
