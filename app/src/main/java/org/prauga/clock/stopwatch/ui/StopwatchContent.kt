/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import org.prauga.clock.data.Lap
import org.prauga.clock.data.Stopwatch

data class StopwatchContent(val stopwatch: Stopwatch, val laps: List<Lap>, val canAddLap: Boolean)
