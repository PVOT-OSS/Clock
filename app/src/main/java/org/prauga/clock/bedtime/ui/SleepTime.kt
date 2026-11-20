/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import kotlin.math.roundToInt

internal const val MINUTES_PER_HOUR = 60
internal const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
internal const val DIAL_STEP_MINUTES = 5

internal fun minuteOfDay(hour: Int, minute: Int): Int = hour * MINUTES_PER_HOUR + minute

internal fun wrapDay(minutes: Int): Int = Math.floorMod(minutes, MINUTES_PER_DAY)

internal fun sleepLength(bed: Int, wake: Int): Int = wrapDay(wake - bed)

internal fun snapToStep(minutes: Float): Int =
    wrapDay((minutes / DIAL_STEP_MINUTES).roundToInt() * DIAL_STEP_MINUTES)
