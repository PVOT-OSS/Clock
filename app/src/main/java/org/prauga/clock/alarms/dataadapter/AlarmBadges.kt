/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.dataadapter

import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import org.prauga.clock.R
import org.prauga.clock.Utils
import org.prauga.clock.provider.Alarm

/** Marks a collapsed alarm that will only vibrate, or make no sound at all. */
object AlarmBadges {

    @JvmStatic
    fun bind(summary: TextView, alarm: Alarm) {
        val silent = alarm.alert == Utils.RINGTONE_SILENT
        val (icon, description) =
            when {
                !silent -> null to null
                alarm.vibrate -> R.drawable.ic_vibrate to R.string.alarm_badge_vibrate_only
                else -> R.drawable.ic_ringtone_silent to R.string.alarm_badge_silent
            }
        val drawable =
            icon
                ?.let { ContextCompat.getDrawable(summary.context, it)?.mutate() }
                ?.apply {
                    val size = summary.lineHeight
                    setBounds(0, 0, size, size)
                }
        summary.setCompoundDrawablesRelative(null, null, drawable, null)
        summary.compoundDrawablePadding = summary.lineHeight / 3
        TextViewCompat.setCompoundDrawableTintList(summary, summary.textColors)
        if (description != null) {
            summary.contentDescription =
                "${summary.contentDescription ?: summary.text}, ${summary.context.getString(description)}"
        }
    }
}
