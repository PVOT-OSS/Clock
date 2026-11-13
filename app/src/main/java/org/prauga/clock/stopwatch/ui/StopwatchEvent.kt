/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

sealed interface StopwatchEvent {
    data object Start : StopwatchEvent

    data object Pause : StopwatchEvent

    data object Reset : StopwatchEvent

    data object Lap : StopwatchEvent
}
