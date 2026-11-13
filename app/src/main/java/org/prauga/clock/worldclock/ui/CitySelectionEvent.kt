/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import org.prauga.clock.data.City

sealed interface CitySelectionEvent {
    data class Search(val query: String) : CitySelectionEvent

    data class Toggle(val city: City) : CitySelectionEvent

    data object ToggleSort : CitySelectionEvent

    data class AddPlace(val name: String, val zoneId: String) : CitySelectionEvent

    data object Save : CitySelectionEvent
}
