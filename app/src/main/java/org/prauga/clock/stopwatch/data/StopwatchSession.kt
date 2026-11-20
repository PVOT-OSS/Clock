/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.data

data class StopwatchSession(val savedAt: Long, val total: Long, val laps: List<Long>)
