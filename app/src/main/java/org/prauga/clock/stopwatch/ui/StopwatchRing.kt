/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.prauga.pvot.designsystem.theme.PvotSpacing
import com.prauga.pvot.designsystem.theme.PvotTheme
import org.prauga.clock.data.Stopwatch

@Composable
fun StopwatchRing(stopwatch: Stopwatch, total: Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val target =
        when {
            stopwatch.isRunning -> colors.primary
            stopwatch.isPaused -> PvotTheme.extendedColors.warning
            else -> colors.surfaceVariant
        }
    val color by animateColorAsState(target, label = "stopwatchRing")
    val sweep = if (stopwatch.isReset) FULL_CIRCLE else total % MINUTE * FULL_CIRCLE / MINUTE
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * STROKE_FRACTION
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val style = Stroke(width = stroke)
            drawArc(
                color = color.copy(alpha = TRACK_ALPHA),
                startAngle = 0f,
                sweepAngle = FULL_CIRCLE,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = style,
            )
            drawArc(
                color = color,
                startAngle = START_ANGLE,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = style,
            )
        }
        Text(
            text = formatStopwatch(total),
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
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
private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val STROKE_FRACTION = 0.07f
private const val TRACK_ALPHA = 0.24f
