/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch

import android.widget.TextView
import org.prauga.clock.R
import org.prauga.clock.ThemeUtils
import org.prauga.clock.data.Lap

/** Marks the fastest and the slowest of the recorded laps once there are at least two. */
object LapStats {

    @JvmStatic
    fun decorate(lapTime: TextView, lap: Lap?, laps: List<Lap>) {
        val defaultColors =
            (lapTime.getTag(R.id.lap_time) as? android.content.res.ColorStateList)
                ?: lapTime.textColors.also { lapTime.setTag(R.id.lap_time, it) }
        lapTime.setTextColor(defaultColors)
        if (lap == null || laps.size < 2) {
            return
        }
        val fastest = laps.minOf { it.lapTime }
        val slowest = laps.maxOf { it.lapTime }
        if (fastest == slowest) {
            return
        }
        val context = lapTime.context
        val (color, label) =
            when (lap.lapTime) {
                fastest -> R.attr.colorPrimary to R.string.lap_fastest
                slowest -> R.attr.colorError to R.string.lap_slowest
                else -> return
            }
        lapTime.setTextColor(ThemeUtils.resolveColor(context, color))
        lapTime.contentDescription = "${lapTime.text}, ${context.getString(label)}"
    }
}
