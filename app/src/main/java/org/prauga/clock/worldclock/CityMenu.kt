/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock

import android.view.View
import android.widget.PopupMenu
import org.prauga.clock.R
import org.prauga.clock.data.City
import org.prauga.clock.data.DataModel

/** Long-press menu of a world clock: move it up or down the list, or remove it. */
object CityMenu {

    private const val MOVE_UP = 1
    private const val MOVE_DOWN = 2
    private const val REMOVE = 3

    /** Shows the menu for [city] if it is one of the selected world clocks. */
    @JvmStatic
    fun show(anchor: View, city: City): Boolean {
        val model = DataModel.getDataModel()
        val cities = model.selectedCities
        val index = cities.indexOf(city)
        if (index < 0) {
            return false
        }
        PopupMenu(anchor.context, anchor).apply {
            menu.add(0, MOVE_UP, 0, R.string.city_move_up).isEnabled = index > 0
            menu.add(0, MOVE_DOWN, 1, R.string.city_move_down).isEnabled = index < cities.lastIndex
            menu.add(0, REMOVE, 2, R.string.city_remove)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MOVE_UP -> model.moveSelectedCity(city, -1)
                    MOVE_DOWN -> model.moveSelectedCity(city, 1)
                    REMOVE -> model.removeSelectedCity(city)
                }
                true
            }
            show()
        }
        return true
    }
}
