/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Date
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
            val text = firstEventTomorrow()
            Handler(Looper.getMainLooper()).post { event.text = text }
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun firstEventTomorrow(): String {
        val start =
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val uri =
            CalendarContract.Instances.CONTENT_URI.buildUpon()
                .also {
                    ContentUris.appendId(it, start.timeInMillis)
                    ContentUris.appendId(it, end.timeInMillis)
                }
                .build()
        val projection =
            arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
            )
        try {
            context.contentResolver
                .query(
                    uri,
                    projection,
                    "${CalendarContract.Instances.ALL_DAY} = 0",
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC",
                )
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val title =
                            cursor.getString(0)?.takeIf { it.isNotBlank() }
                                ?: context.getString(R.string.calendar_untitled)
                        val time = DateFormat.getTimeFormat(context).format(Date(cursor.getLong(1)))
                        return context.getString(R.string.calendar_first_event, time, title)
                    }
                }
        } catch (_: SecurityException) {
            return context.getString(R.string.calendar_permission_hint)
        }
        return context.getString(R.string.calendar_no_event)
    }
}
