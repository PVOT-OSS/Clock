/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import org.prauga.clock.data.City

data class CitySection(val index: String, val cities: List<City>, val selected: Boolean = false)
