/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock

import android.content.SharedPreferences
import org.prauga.clock.data.City

/**
 * The order of the world clocks. They are sorted by UTC offset until the user moves one; from then
 * on their saved order is kept and newly chosen cities are appended.
 */
object CityOrder {

    private const val KEY_CUSTOM_ORDER = "selected_cities_custom_order"

    @JvmStatic
    fun isCustom(prefs: SharedPreferences): Boolean = prefs.getBoolean(KEY_CUSTOM_ORDER, false)

    @JvmStatic
    fun setCustom(prefs: SharedPreferences) {
        prefs.edit().putBoolean(KEY_CUSTOM_ORDER, true).apply()
    }

    /** The cities in [chosen], keeping the user's order of [current] when there is one. */
    @JvmStatic
    fun merge(prefs: SharedPreferences, current: List<City>, chosen: Collection<City>): List<City> {
        if (!isCustom(prefs)) {
            return chosen.toList()
        }
        val kept = current.filter { it in chosen }
        val added = chosen.filter { it !in kept }.sortedWith(City.UtcOffsetComparator())
        return kept + added
    }

    /** [cities] with [city] moved [delta] places, clamped to the ends of the list. */
    @JvmStatic
    fun move(cities: List<City>, city: City, delta: Int): List<City> {
        val from = cities.indexOf(city)
        if (from < 0) {
            return cities
        }
        val to = (from + delta).coerceIn(0, cities.lastIndex)
        return cities.toMutableList().apply { add(to, removeAt(from)) }
    }
}
