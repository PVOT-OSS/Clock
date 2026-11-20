/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

sealed interface AlarmEvent {
    data class SetEnabled(val id: Long, val enabled: Boolean) : AlarmEvent

    data class Predismiss(val id: Long) : AlarmEvent
}
