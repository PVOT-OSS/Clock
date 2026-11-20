/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.common.ContentMaxWidth
import org.prauga.clock.common.ScreenAction
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Lap

@Composable
fun StopwatchScreen(
    padding: PaddingValues,
    listState: LazyListState,
    viewModel: StopwatchViewModel = viewModel { StopwatchViewModel(DataModel.getDataModel()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = (state as? UiState.Success)?.data ?: return
    StopwatchBody(content, padding, listState, viewModel::onEvent)
}

@Composable
private fun StopwatchBody(
    content: StopwatchContent,
    padding: PaddingValues,
    listState: LazyListState,
    onEvent: (StopwatchEvent) -> Unit,
) {
    val stopwatch = content.stopwatch
    var total by remember { mutableLongStateOf(stopwatch.totalTime) }
    LaunchedEffect(stopwatch) {
        total = stopwatch.totalTime
        while (stopwatch.isRunning) {
            withFrameMillis { total = stopwatch.totalTime }
        }
    }
    ScreenAction(
        icon =
            if (stopwatch.isRunning) R.drawable.ic_t_player_pause else R.drawable.ic_t_player_play,
        label =
            when {
                stopwatch.isRunning -> R.string.sw_pause_button
                stopwatch.isPaused -> R.string.sw_resume_button
                else -> R.string.sw_start_button
            },
    ) {
        onEvent(if (stopwatch.isRunning) StopwatchEvent.Pause else StopwatchEvent.Start)
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = bottomInset()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StopwatchRing(
            stopwatch = stopwatch,
            total = total,
            modifier = Modifier.padding(PvotSpacing.L).widthIn(max = RingMaxSize).fillMaxWidth(),
        )
        LapList(content, total, listState, Modifier.weight(1f))
        StopwatchControls(content, total, onEvent, Modifier.padding(top = PvotSpacing.L))
    }
}

@Composable
private fun LapList(
    content: StopwatchContent,
    total: Long,
    state: LazyListState,
    modifier: Modifier = Modifier,
) {
    val laps = content.laps
    LazyColumn(
        state = state,
        modifier = modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PvotSpacing.L),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
    ) {
        if (laps.isNotEmpty() && !content.stopwatch.isReset) {
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
}

@Composable
private fun StopwatchControls(
    content: StopwatchContent,
    total: Long,
    onEvent: (StopwatchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val stopwatch = content.stopwatch
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.L),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (stopwatch.isPaused) {
            PvotButton(
                text = stringResource(R.string.sw_reset_button),
                icon = painterResource(R.drawable.ic_t_refresh),
                style = PvotButtonStyle.Tonal,
                onClick = { onEvent(StopwatchEvent.Reset) },
            )
        } else {
            PvotButton(
                text = stringResource(R.string.sw_lap_button),
                icon = painterResource(R.drawable.ic_t_flag),
                style = PvotButtonStyle.Tonal,
                enabled = stopwatch.isRunning && content.canAddLap,
                onClick = { onEvent(StopwatchEvent.Lap) },
            )
        }
        if (stopwatch.isPaused) {
            val shareTitle = stringResource(R.string.sw_share_button)
            PvotIconButton(
                icon = painterResource(R.drawable.ic_t_share),
                contentDescription = shareTitle,
                onClick = { share(context, shareTitle, total, content.laps) },
            )
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
            formatStopwatch(lapTime),
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
        Text(
            formatStopwatch(total),
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

@Composable
private fun bottomInset(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        LocalPvotNavBarSizes.current.barHeight +
        PvotSpacing.L

private fun share(context: Context, title: String, total: Long, laps: List<Lap>) {
    val text = buildString {
        append(formatStopwatch(total))
        laps.reversed().forEach { append("\n#${it.lapNumber}  ${formatStopwatch(it.lapTime)}") }
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
            title,
        )
    )
}

private val RingMaxSize = 320.dp
