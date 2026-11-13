/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

sealed interface TimerEvent {
    data class Create(val lengthMillis: Long) : TimerEvent

    data class Start(val id: Int) : TimerEvent

    data class Pause(val id: Int) : TimerEvent

    data class Reset(val id: Int) : TimerEvent

    data class Stop(val id: Int) : TimerEvent

    data class AddMinute(val id: Int) : TimerEvent

    data class Delete(val id: Int) : TimerEvent
}
