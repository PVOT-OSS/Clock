/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

sealed interface RingingOutcome {
    data class Snoozed(val minutes: Int) : RingingOutcome

    data object Dismissed : RingingOutcome
}
