/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

enum class SleepGoalStatus {
    Below,
    Meets,
    Above,
}

private const val GOAL_TOLERANCE_MINUTES = 15

internal fun sleepGoalStatus(sleepMinutes: Int, goalMinutes: Int): SleepGoalStatus =
    when {
        sleepMinutes < goalMinutes - GOAL_TOLERANCE_MINUTES -> SleepGoalStatus.Below
        sleepMinutes > goalMinutes + GOAL_TOLERANCE_MINUTES -> SleepGoalStatus.Above
        else -> SleepGoalStatus.Meets
    }
