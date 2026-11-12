/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarmclock

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import org.prauga.clock.R

/** The faces of the analog widget, chosen per widget. */
object AnalogFaces {

    enum class Face(val layout: Int, val label: Int) {
        CLASSIC(R.layout.analog_appwidget, R.string.analog_face_classic),
        MATERIAL(R.layout.analog_appwidget_material, R.string.analog_face_material),
        MINIMAL(R.layout.analog_appwidget_minimal, R.string.analog_face_minimal),
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("analog_widget_faces", Context.MODE_PRIVATE)

    @JvmStatic
    fun layoutFor(context: Context, widgetId: Int): Int =
        Face.entries
            .getOrElse(prefs(context).getInt(widgetId.toString(), 0)) { Face.CLASSIC }
            .layout

    fun set(context: Context, widgetId: Int, face: Face) {
        prefs(context).edit().putInt(widgetId.toString(), face.ordinal).apply()
    }

    @JvmStatic
    fun forget(context: Context, widgetIds: IntArray) {
        prefs(context).edit().apply { widgetIds.forEach { remove(it.toString()) } }.apply()
    }
}

/** Lets the user pick a face when adding the analog widget, or later from the launcher. */
class AnalogWidgetConfigActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        setResult(RESULT_CANCELED, result(widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setTitle(R.string.analog_face_title)
        val inflater = LayoutInflater.from(this)
        val size = resources.getDimensionPixelSize(R.dimen.analog_face_preview_size)
        val row =
            LinearLayout(this).apply {
                gravity = Gravity.CENTER
                setBackgroundColor(getColor(android.R.color.system_neutral1_900))
            }
        for (face in AnalogFaces.Face.entries) {
            val item =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(size / 8)
                    isClickable = true
                    isFocusable = true
                    contentDescription = getString(face.label)
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener { choose(widgetId, face) }
                }
            item.addView(inflater.inflate(face.layout, item, false), size, size)
            item.addView(
                TextView(this).apply {
                    setText(face.label)
                    setTextColor(getColor(android.R.color.system_neutral1_100))
                }
            )
            row.addView(item)
        }
        setContentView(row)
    }

    private fun choose(widgetId: Int, face: AnalogFaces.Face) {
        AnalogFaces.set(this, widgetId, face)
        val manager = AppWidgetManager.getInstance(this)
        AnalogAppWidgetProvider().onUpdate(this, manager, intArrayOf(widgetId))
        setResult(RESULT_OK, result(widgetId))
        finish()
    }

    private fun result(widgetId: Int) =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}
