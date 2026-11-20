/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.prauga.pvot.designsystem.components.progress.PvotProgressRing
import com.prauga.pvot.designsystem.theme.PvotSpacing
import com.prauga.pvot.designsystem.theme.PvotTheme
import org.prauga.clock.data.Stopwatch

@Composable
fun StopwatchRing(stopwatch: Stopwatch, total: Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val color =
        when {
            stopwatch.isRunning -> colors.primary
            stopwatch.isPaused -> PvotTheme.extendedColors.warning
            else -> colors.surfaceVariant
        }
    PvotProgressRing(
        progress = { if (stopwatch.isReset) 1f else total % MINUTE / MINUTE },
        color = color,
        modifier = modifier,
    ) {
        val style = MaterialTheme.typography.displayMedium
        Text(
            text = formatStopwatch(total),
            style = style.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize),
            modifier = Modifier.padding(horizontal = PvotSpacing.L),
        )
    }
}

fun formatStopwatch(millis: Long): String {
    val hours = millis / 3_600_000
    val minutes = millis / 60_000 % 60
    val seconds = millis / 1000 % 60
    val centis = millis / 10 % 100
    return if (hours > 0) "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
    else "%02d:%02d.%02d".format(minutes, seconds, centis)
}

private const val MINUTE = 60_000f
