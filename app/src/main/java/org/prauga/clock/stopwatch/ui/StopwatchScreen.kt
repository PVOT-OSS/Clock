/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Lap
import org.prauga.clock.data.StopwatchListener

@Composable
fun StopwatchScreen(padding: PaddingValues, listState: LazyListState) {
    val model = DataModel.getDataModel()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.sw_share_button)
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
        PvotScreen(
            state = listState,
            contentPadding = screenPadding(PaddingValues(), withFab = true),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        ) {
            item(key = "time") {
                Text(
                    text = format(total, hundredths = true),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = PvotSpacing.L),
                )
            }
            if (laps.isNotEmpty() && !stopwatch.isReset) {
                item(key = "current") {
                    val current = total - (laps.firstOrNull()?.accumulatedTime ?: 0L)
                    LapRow(laps.size + 1, current, total, Color.Unspecified)
                }
            }
            itemsIndexed(laps, key = { _, lap -> lap.lapNumber }) { index, lap ->
                Box(Modifier.pvotReveal(index)) {
                    LapRow(lap.lapNumber, lap.lapTime, lap.accumulatedTime, lapColor(lap, laps))
                }
            }
        }
        Row(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = LocalPvotNavBarSizes.current.barHeight + PvotSpacing.L),
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.L),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!stopwatch.isReset) {
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_t_refresh),
                    contentDescription = stringResource(R.string.sw_reset_button),
                    onClick = { model.resetStopwatch() },
                )
            }
            PvotFab(
                icon =
                    painterResource(
                        if (stopwatch.isRunning) R.drawable.ic_t_player_pause
                        else R.drawable.ic_t_player_play
                    ),
                contentDescription =
                    stringResource(
                        if (stopwatch.isRunning) R.string.sw_pause_button
                        else R.string.sw_start_button
                    ),
                onClick = {
                    if (stopwatch.isRunning) model.pauseStopwatch() else model.startStopwatch()
                },
            )
            if (stopwatch.isRunning) {
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_t_flag),
                    contentDescription = stringResource(R.string.sw_lap_button),
                    enabled = model.canAddMoreLaps(),
                    onClick = {
                        model.addLap()
                        laps = model.laps.toList()
                    },
                )
            } else if (stopwatch.isPaused) {
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_t_share),
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
                                shareTitle,
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
