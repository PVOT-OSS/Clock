/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.view.View
import android.widget.Button
import com.google.android.material.button.MaterialButtonToggleGroup
import org.prauga.clock.R

/** The sleep sounds card: which noise, for how long, and play/stop. */
class SleepSoundCard(root: View, private val settings: BedtimeSettings) {

    private val context = root.context
    private val noise: MaterialButtonToggleGroup = root.findViewById(R.id.sleep_sound_noise)
    private val length: MaterialButtonToggleGroup = root.findViewById(R.id.sleep_sound_length)
    private val play: Button = root.findViewById(R.id.sleep_sound_play)

    init {
        noise.check(NOISE_BUTTONS[settings.sleepSound])
        length.check(LENGTH_BUTTONS[LENGTHS.indexOf(settings.sleepSoundMinutes).coerceAtLeast(1)])
        noise.addOnButtonCheckedListener { _, id, checked ->
            if (checked) settings.sleepSound = NOISE_BUTTONS.indexOf(id)
        }
        length.addOnButtonCheckedListener { _, id, checked ->
            if (checked) settings.sleepSoundMinutes = LENGTHS[LENGTH_BUTTONS.indexOf(id)]
        }
        play.setOnClickListener {
            if (SleepSoundService.isPlaying) {
                SleepSoundService.stop(context)
            } else {
                SleepSoundService.start(
                    context,
                    SleepSoundService.Noise.entries[settings.sleepSound],
                    settings.sleepSoundMinutes,
                )
            }
            play.postDelayed({ bind() }, 300)
        }
        bind()
    }

    fun bind() {
        play.setText(
            if (SleepSoundService.isPlaying) R.string.sleep_sound_stop
            else R.string.sleep_sound_play
        )
    }

    private companion object {
        val NOISE_BUTTONS =
            listOf(R.id.sleep_sound_white, R.id.sleep_sound_pink, R.id.sleep_sound_brown)
        val LENGTH_BUTTONS = listOf(R.id.sleep_sound_15, R.id.sleep_sound_30, R.id.sleep_sound_60)
        val LENGTHS = listOf(15, 30, 60)
    }
}
