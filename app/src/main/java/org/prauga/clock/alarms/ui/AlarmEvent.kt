/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

sealed interface AlarmEvent {
    data class SetEnabled(val id: Long, val enabled: Boolean) : AlarmEvent

    data class Predismiss(val id: Long) : AlarmEvent

    data class SetGroupEnabled(val groupId: Long, val enabled: Boolean) : AlarmEvent

    data class SetGroupCollapsed(val groupId: Long, val collapsed: Boolean) : AlarmEvent

    data class RenameGroup(val groupId: Long, val name: String) : AlarmEvent

    data class DeleteGroup(val groupId: Long, val withAlarms: Boolean) : AlarmEvent
}
