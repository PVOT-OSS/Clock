/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer

import android.widget.Button
import org.prauga.clock.R

/** One-tap durations above the timer keypad. */
object TimerPresets {

    private val MINUTES = intArrayOf(5, 10, 30, 45)
    private val BUTTONS =
        intArrayOf(
            R.id.timer_preset_0,
            R.id.timer_preset_1,
            R.id.timer_preset_2,
            R.id.timer_preset_3,
        )

    @JvmStatic
    fun bind(setup: TimerSetupView) {
        val res = setup.resources
        BUTTONS.forEachIndexed { i, id ->
            val minutes = MINUTES[i]
            setup.findViewById<Button>(id)?.apply {
                text = res.getString(R.string.timer_preset_minutes, minutes)
                contentDescription =
                    res.getQuantityString(
                        R.plurals.timer_preset_minutes_description,
                        minutes,
                        minutes,
                    )
                setOnClickListener { setup.setMinutes(minutes) }
            }
        }
    }
}
