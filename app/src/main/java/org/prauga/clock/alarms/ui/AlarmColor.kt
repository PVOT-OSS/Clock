/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.prauga.pvot.designsystem.theme.PvotTheme
import org.prauga.clock.R

// Stored by ordinal in the alarms table: only append, never reorder or remove.
enum class AlarmColor(@StringRes val label: Int) {
    Violet(R.string.alarm_color_violet),
    Blue(R.string.alarm_color_blue),
    Pink(R.string.alarm_color_pink),
    Green(R.string.alarm_color_green),
    Amber(R.string.alarm_color_amber),
    Red(R.string.alarm_color_red);

    companion object {
        fun of(stored: Int): AlarmColor? = entries.getOrNull(stored)
    }
}

class AlarmAccent(val color: Color, val onColor: Color)

@Composable
fun AlarmColor.accent(): AlarmAccent {
    val scheme = MaterialTheme.colorScheme
    val extended = PvotTheme.extendedColors
    return when (this) {
        AlarmColor.Violet -> AlarmAccent(scheme.primary, scheme.onPrimary)
        AlarmColor.Blue -> AlarmAccent(scheme.secondary, scheme.onSecondary)
        AlarmColor.Pink -> AlarmAccent(scheme.tertiary, scheme.onTertiary)
        AlarmColor.Green -> AlarmAccent(extended.success, extended.onSuccess)
        AlarmColor.Amber -> AlarmAccent(extended.warning, extended.onWarning)
        AlarmColor.Red -> AlarmAccent(scheme.error, scheme.onError)
    }
}
