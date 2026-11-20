/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import org.prauga.clock.provider.AlarmGroup

data class AlarmGroupSection(val group: AlarmGroup, val alarms: List<AlarmItem>) {
    val enabledCount: Int
        get() = alarms.count { it.enabled }

    val anyOn: Boolean
        get() = enabledCount > 0
}

data class AlarmsContent(
    val wakeUp: AlarmItem?,
    val groups: List<AlarmGroupSection>,
    val others: List<AlarmItem>,
) {
    val isEmpty: Boolean
        get() = wakeUp == null && groups.isEmpty() && others.isEmpty()

    val all: List<AlarmItem>
        get() = listOfNotNull(wakeUp) + groups.flatMap { it.alarms } + others
}

fun alarmsContent(items: List<AlarmItem>, groups: List<AlarmGroup>, wakeUpId: Long): AlarmsContent {
    val groupIds = groups.mapTo(HashSet()) { it.id }
    val listed = items.filter { it.id != wakeUpId }
    return AlarmsContent(
        wakeUp = items.firstOrNull { it.id == wakeUpId },
        groups =
            groups.map { group ->
                AlarmGroupSection(group, listed.filter { it.groupId == group.id })
            },
        others = listed.filter { it.groupId !in groupIds },
    )
}
