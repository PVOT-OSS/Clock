/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

sealed interface AlarmListEntry {
    val key: Any

    data class Title(val section: AlarmListSection) : AlarmListEntry {
        override val key: Any
            get() = "title-$section"
    }

    data class WakeUp(val item: AlarmItem) : AlarmListEntry {
        override val key: Any
            get() = item.id
    }

    data class GroupHeader(val section: AlarmGroupSection) : AlarmListEntry {
        override val key: Any
            get() = "group-${section.group.id}"
    }

    data class Cards(val items: List<AlarmItem>) : AlarmListEntry {
        override val key: Any
            get() = "row-${items.first().id}"
    }
}

enum class AlarmListSection {
    WakeUp,
    Others,
}

fun alarmListEntries(content: AlarmsContent, columns: Int): List<AlarmListEntry> = buildList {
    content.wakeUp?.let {
        add(AlarmListEntry.Title(AlarmListSection.WakeUp))
        add(AlarmListEntry.WakeUp(it))
    }
    content.groups.forEach { section ->
        add(AlarmListEntry.GroupHeader(section))
        if (!section.group.collapsed) {
            section.alarms.chunked(columns).forEach { add(AlarmListEntry.Cards(it)) }
        }
    }
    if (content.others.isNotEmpty()) {
        if (content.wakeUp != null || content.groups.isNotEmpty()) {
            add(AlarmListEntry.Title(AlarmListSection.Others))
        }
        content.others.chunked(columns).forEach { add(AlarmListEntry.Cards(it)) }
    }
}

fun List<AlarmListEntry>.indexOfAlarm(id: Long): Int = indexOfFirst { entry ->
    when (entry) {
        is AlarmListEntry.WakeUp -> entry.item.id == id
        is AlarmListEntry.GroupHeader ->
            entry.section.group.collapsed && entry.section.alarms.any { it.id == id }
        is AlarmListEntry.Cards -> entry.items.any { it.id == id }
        is AlarmListEntry.Title -> false
    }
}
