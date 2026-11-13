/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui.timer

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.picker.PvotDurationPicker
import com.prauga.pvot.designsystem.components.progress.PvotLinearProgress
import com.prauga.pvot.designsystem.motion.pvotReveal
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Timer
import org.prauga.clock.data.TimerListener
import org.prauga.clock.ui.ScreenFab
import org.prauga.clock.ui.screenPadding

private val PRESETS = listOf(5, 10, 30, 45)

/** The Timer tab: the running timers, or a duration picker to start a new one. */
@Composable
fun TimerScreen(padding: PaddingValues, listState: LazyListState, setupRequests: Int) {
    val model = DataModel.getDataModel()
    // Timer.equals compares ids only: an updated list compares equal to the old one, so the state
    // must store it regardless, and the version tells the cards to redraw.
    var timers by remember { mutableStateOf(model.timers.toList(), neverEqualPolicy()) }
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val listener =
            object : TimerListener {
                override fun timerAdded(timer: Timer) {
                    timers = model.timers.toList()
                    version++
                }

                override fun timerUpdated(before: Timer, after: Timer) {
                    timers = model.timers.toList()
                    version++
                }

                override fun timerRemoved(timer: Timer) {
                    timers = model.timers.toList()
                    version++
                }
            }
        model.addTimerListener(listener)
        onDispose { model.removeTimerListener(listener) }
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(setupRequests) { if (setupRequests > 0) adding = true }

    Box(Modifier.fillMaxSize()) {
        if (timers.isEmpty() || adding) {
            TimerSetup(
                modifier = Modifier.padding(top = padding.calculateTopPadding()),
                canCancel = timers.isNotEmpty(),
                onCancel = { adding = false },
                onStart = { length ->
                    model.startTimer(model.addTimer(length, "", false))
                    adding = false
                },
            )
        } else {
            TimerList(timers, version, listState, padding, onAdd = { adding = true })
        }
    }
}

@Composable
private fun TimerSetup(
    canCancel: Boolean,
    onCancel: () -> Unit,
    onStart: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var duration by rememberSaveable(stateSaver = DurationSaver) { mutableStateOf(Duration.ZERO) }
    if (duration > Duration.ZERO) {
        ScreenFab(R.drawable.ic_t_player_play, R.string.timer_start) {
            onStart(duration.inWholeMilliseconds)
        }
    }
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PvotDurationPicker(
                duration = duration,
                onDurationChange = { duration = it },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                PRESETS.forEach { minutes ->
                    PvotButton(
                        text = stringResource(R.string.timer_preset_minutes, minutes),
                        onClick = { duration = minutes.minutes },
                        style = PvotButtonStyle.Tonal,
                    )
                }
            }
            if (canCancel) {
                PvotButton(
                    text = stringResource(android.R.string.cancel),
                    onClick = onCancel,
                    style = PvotButtonStyle.Text,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TimerList(
    timers: List<Timer>,
    version: Int,
    listState: LazyListState,
    padding: PaddingValues,
    onAdd: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val ticking = timers.any { it.isRunning || it.isExpired }
    LaunchedEffect(ticking) {
        while (ticking) {
            withFrameMillis { now = System.currentTimeMillis() }
        }
    }
    ScreenFab(R.drawable.ic_t_plus, R.string.new_timer, onAdd)
    Box(Modifier.fillMaxSize()) {
        PvotScreen(state = listState, contentPadding = screenPadding(padding, withFab = true)) {
            itemsIndexed(timers, key = { _, timer -> timer.id }) { index, timer ->
                TimerCard(
                    timer,
                    timer.state,
                    timer.remainingTime,
                    now,
                    version,
                    Modifier.pvotReveal(index),
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: Timer,
    state: Timer.State,
    remainingAtBind: Long,
    now: Long,
    @Suppress("UNUSED_PARAMETER") version: Int,
    modifier: Modifier = Modifier,
) {
    val model = DataModel.getDataModel()
    // Timer.equals compares ids only, so the state and the clock are passed in to redraw the
    // card whenever either changes.
    val remaining =
        if (state == Timer.State.RUNNING || state == Timer.State.EXPIRED) {
            timer.remainingTime
        } else {
            remainingAtBind
        }
    PvotCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = timer.label.ifEmpty { stringResource(R.string.timer_notification_label) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = format(remaining),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                color =
                    if (timer.isExpired) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
            )
            PvotLinearProgress(
                progress = {
                    (1f - remaining.toFloat() / timer.totalLength.coerceAtLeast(1)).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PvotButton(
                    text = stringResource(R.string.timer_plus_1_min),
                    onClick = { model.addTimerMinute(timer) },
                    style = PvotButtonStyle.Tonal,
                )
                when {
                    timer.isRunning ->
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_t_player_pause),
                            contentDescription = stringResource(R.string.timer_pause),
                            onClick = { model.pauseTimer(timer) },
                        )
                    timer.isExpired ->
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_t_x),
                            contentDescription = stringResource(R.string.timer_stop),
                            onClick = { model.resetOrDeleteTimer(timer, R.string.label_deskclock) },
                        )
                    else ->
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_t_player_play),
                            contentDescription = stringResource(R.string.timer_start),
                            onClick = { model.startTimer(timer) },
                        )
                }
                if (timer.isPaused) {
                    PvotIconButton(
                        icon = painterResource(R.drawable.ic_t_refresh),
                        contentDescription = stringResource(R.string.timer_reset),
                        onClick = { model.resetTimer(timer) },
                    )
                }
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_t_trash),
                    contentDescription = stringResource(R.string.timer_delete),
                    onClick = { model.removeTimer(timer) },
                )
            }
        }
    }
}

/** Rounds up like the classic timer while positive; overtime counts up after a minus sign. */
private fun format(remaining: Long): String {
    val seconds =
        if (remaining >= 0) (remaining + DateUtils.SECOND_IN_MILLIS - 1) / 1000
        else remaining / 1000
    val total = abs(seconds)
    val text =
        if (total >= 3600) "%d:%02d:%02d".format(total / 3600, total / 60 % 60, total % 60)
        else "%d:%02d".format(total / 60, total % 60)
    return if (seconds < 0) "−$text" else text
}

private val DurationSaver =
    androidx.compose.runtime.saveable.Saver<Duration, Long>(
        save = { it.inWholeSeconds },
        restore = { it.seconds },
    )
