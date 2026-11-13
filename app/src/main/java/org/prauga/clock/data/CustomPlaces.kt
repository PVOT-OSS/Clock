/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.data

import android.content.SharedPreferences
import java.util.TimeZone

object CustomPlaces {

    private const val KEY = "custom_places"
    private const val ID_PREFIX = "custom_"

    @JvmStatic
    fun load(prefs: SharedPreferences): Map<String, City> =
        entries(prefs)
            .mapIndexedNotNull { i, (name, zone) ->
                val tz = TimeZone.getTimeZone(zone)
                if (tz.id != zone) null
                else {
                    val id = ID_PREFIX + i
                    id to City(id, -1, name.take(1).uppercase(), name, name, tz)
                }
            }
            .toMap()

    @JvmStatic
    fun add(prefs: SharedPreferences, name: String, zone: String): String {
        val entries = entries(prefs) + (name.replace("\n", " ") to zone)
        prefs.edit().putString(KEY, entries.joinToString("\n") { (n, z) -> "$n\t$z" }).apply()
        return ID_PREFIX + entries.lastIndex
    }

    private fun entries(prefs: SharedPreferences): List<Pair<String, String>> =
        prefs.getString(KEY, null)?.lines()?.mapNotNull { line ->
            line.split('\t').takeIf { it.size == 2 }?.let { it[0] to it[1] }
        } ?: emptyList()
}
