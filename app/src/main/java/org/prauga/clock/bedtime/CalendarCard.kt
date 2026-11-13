/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import org.prauga.clock.R

/** Tomorrow's first calendar event, to see how early the morning starts. */
class CalendarCard(root: View, private val requestPermission: ActivityResultLauncher<String>) {

    private val context: Context = root.context
    private val event: TextView = root.findViewById(R.id.calendar_event)
    private val allow: Button = root.findViewById(R.id.calendar_allow)

    init {
        allow.setOnClickListener { requestPermission.launch(Manifest.permission.READ_CALENDAR) }
    }

    fun load() {
        if (!hasPermission()) {
            event.setText(R.string.calendar_permission_hint)
            allow.visibility = View.VISIBLE
            return
        }
        allow.visibility = View.GONE
        Executors.newSingleThreadExecutor().execute {
            val text = TomorrowsEvent.describe(context)
            Handler(Looper.getMainLooper()).post { event.text = text }
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
}
