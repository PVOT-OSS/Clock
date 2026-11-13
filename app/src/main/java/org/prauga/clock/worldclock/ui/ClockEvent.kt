/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import org.prauga.clock.data.City

sealed interface ClockEvent {
    data object Refresh : ClockEvent

    data class Move(val city: City, val delta: Int) : ClockEvent

    data class Remove(val city: City) : ClockEvent
}
