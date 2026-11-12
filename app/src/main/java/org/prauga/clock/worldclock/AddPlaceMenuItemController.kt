/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.TimeZone
import org.prauga.clock.R
import org.prauga.clock.actionbarmenu.MenuItemController
import org.prauga.clock.data.DataModel

/** "Add place": a world clock with any name in any time zone, for places not in the list. */
class AddPlaceMenuItemController(
    private val context: Context,
    private val beforeAdd: Runnable,
    private val afterAdd: Runnable,
) : MenuItemController {

    override fun getId(): Int = R.id.menu_item_add_place

    override fun onCreateOptionsItem(menu: Menu) {
        menu
            .add(Menu.NONE, id, Menu.NONE, R.string.add_place)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    }

    override fun onPrepareOptionsItem(item: MenuItem) {}

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val zones =
            TimeZone.getAvailableIDs()
                .filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
                .sortedWith(compareBy({ TimeZone.getTimeZone(it).rawOffset }, { it }))
        val labels = zones.map { "${offset(it)}  ${it.replace('_', ' ')}" }
        val padding = context.resources.getDimensionPixelSize(R.dimen.add_place_padding)
        val name = EditText(context).apply { setHint(R.string.add_place_name) }
        val zone =
            Spinner(context).apply {
                adapter =
                    ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, labels)
                setSelection(zones.indexOf(TimeZone.getDefault().id).coerceAtLeast(0))
            }
        val form =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding / 2, padding, 0)
                addView(name)
                addView(zone)
            }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.add_place)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.add_place_add) { _, _ ->
                val label = name.text.toString().trim()
                if (label.isNotEmpty()) {
                    beforeAdd.run()
                    DataModel.getDataModel().addCustomPlace(label, zones[zone.selectedItemPosition])
                    afterAdd.run()
                }
            }
            .show()
        return true
    }

    private fun offset(zone: String): String {
        val minutes = TimeZone.getTimeZone(zone).rawOffset / 60_000
        val sign = if (minutes < 0) "−" else "+"
        return "GMT$sign%d:%02d".format(Math.abs(minutes) / 60, Math.abs(minutes) % 60)
    }
}
