/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import org.prauga.clock.data.City

data class ClockContent(
    val seconds: Boolean,
    val analog: Boolean,
    val clocks: List<City>,
    val selected: List<City>,
)
