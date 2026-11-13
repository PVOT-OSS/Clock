/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui.stopwatch

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Lap
import org.prauga.clock.data.StopwatchListener

/** The Stopwatch tab: running time, laps, and reset / start-pause / lap-or-share controls. */
@Composable
fun StopwatchScreen(padding: PaddingValues) {
    val model = DataModel.getDataModel()
    val context = LocalContext.current
    var stopwatch by remember { mutableStateOf(model.stopwatch) }
    var laps by remember { mutableStateOf(model.laps.toList()) }
    DisposableEffect(Unit) {
        val listener = StopwatchListener { _, after ->
            stopwatch = after
            laps = model.laps.toList()
        }
        model.addStopwatchListener(listener)
        onDispose { model.removeStopwatchListener(listener) }
    }
    var total by remember { mutableLongStateOf(stopwatch.totalTime) }
    LaunchedEffect(stopwatch) {
        total = stopwatch.totalTime
        while (stopwatch.isRunning) {
            withFrameMillis { total = stopwatch.totalTime }
        }
    }

    Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = format(total, hundredths = true),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (laps.isNotEmpty() && !stopwatch.isReset) {
                    item(key = "current") {
                        val current = total - (laps.firstOrNull()?.accumulatedTime ?: 0L)
                        LapRow(laps.size + 1, current, total, Color.Unspecified)
                    }
                }
                items(laps, key = { it.lapNumber }) { lap ->
                    LapRow(lap.lapNumber, lap.lapTime, lap.accumulatedTime, lapColor(lap, laps))
                }
            }
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!stopwatch.isReset) {
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_reset),
                    contentDescription = stringResource(R.string.sw_reset_button),
                    onClick = { model.resetStopwatch() },
                )
            }
            PvotFab(
                icon =
                    painterResource(
                        if (stopwatch.isRunning) R.drawable.ic_widget_pause
                        else R.drawable.ic_widget_play
                    ),
                contentDescription =
                    stringResource(
                        if (stopwatch.isRunning) R.string.sw_pause_button
                        else R.string.sw_start_button
                    ),
                onClick = {
                    if (stopwatch.isRunning) model.pauseStopwatch() else model.startStopwatch()
                },
                modifier = Modifier.size(72.dp),
            )
            if (stopwatch.isRunning) {
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_widget_lap),
                    contentDescription = stringResource(R.string.sw_lap_button),
                    enabled = model.canAddMoreLaps(),
                    onClick = {
                        model.addLap()
                        laps = model.laps.toList()
                    },
                )
            } else if (stopwatch.isPaused) {
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.sw_share_button),
                    onClick = {
                        val text = buildString {
                            append(format(total, hundredths = true))
                            laps.reversed().forEach {
                                append("\n#${it.lapNumber}  ${format(it.lapTime, true)}")
                            }
                        }
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, text),
                                context.getString(R.string.sw_share_button),
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LapRow(number: Int, lapTime: Long, total: Long, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "# $number",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            format(lapTime, hundredths = true),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
        Text(
            format(total, hundredths = true),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

/** Fastest lap in the primary colour, slowest in the error colour, once there are two. */
@Composable
private fun lapColor(lap: Lap, laps: List<Lap>): Color {
    if (laps.size < 2) return Color.Unspecified
    val fastest = laps.minOf { it.lapTime }
    val slowest = laps.maxOf { it.lapTime }
    return when {
        fastest == slowest -> Color.Unspecified
        lap.lapTime == fastest -> MaterialTheme.colorScheme.primary
        lap.lapTime == slowest -> MaterialTheme.colorScheme.error
        else -> Color.Unspecified
    }
}

private fun format(millis: Long, hundredths: Boolean): String {
    val hours = millis / 3_600_000
    val minutes = millis / 60_000 % 60
    val seconds = millis / 1000 % 60
    val centis = millis / 10 % 100
    val base =
        if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    return if (hundredths) "%s.%02d".format(base, centis) else base
}
