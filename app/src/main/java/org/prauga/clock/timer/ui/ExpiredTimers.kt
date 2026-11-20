/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import android.os.SystemClock
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.adaptive.PvotTabletopSplit
import com.prauga.pvot.designsystem.adaptive.rememberPvotWindowLayout
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlin.math.abs
import org.prauga.clock.R

object ExpiredTimers {

    @JvmStatic
    fun show(activity: ComponentActivity) {
        activity.setContent {
            PvotAppTheme(darkTheme = true) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground
                ) {
                    ExpiredTimersScreen()
                }
            }
        }
    }
}

@Composable
private fun ExpiredTimersScreen(viewModel: TimerViewModel = timerViewModel(expiredOnly = true)) {
    val activity = LocalActivity.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timers = (state as? UiState.Success)?.data?.timers ?: return
    LaunchedEffect(timers.isEmpty()) { if (timers.isEmpty()) activity?.finish() }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { now = SystemClock.elapsedRealtime() }
        }
    }

    val background = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    if (rememberPvotWindowLayout().tabletop) {
        PvotTabletopSplit(
            modifier = background.safeDrawingPadding(),
            top = { ExpiredOvertimes(timers, now) },
            bottom = { ExpiredControls(timers, viewModel::onEvent) },
        )
        return
    }
    Column(
        background.safeDrawingPadding().padding(PvotSpacing.L),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.timer_times_up),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = PvotSpacing.L),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(PvotSpacing.M)) {
            items(timers, key = { it.id }) { item ->
                ExpiredTimerCard(item, item.remainingAt(now), viewModel::onEvent)
            }
        }
    }
}

@Composable
private fun ExpiredOvertimes(timers: List<TimerItem>, now: Long) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PvotSpacing.L),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.S, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = KEY_TITLE) {
            Text(
                stringResource(R.string.timer_times_up),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        items(timers, key = { it.id }) { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(timerLabel(item), style = MaterialTheme.typography.titleMedium)
                Text(
                    overtime(item.remainingAt(now)),
                    style =
                        if (timers.size == 1) MaterialTheme.typography.displayLarge
                        else MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ExpiredControls(timers: List<TimerItem>, onEvent: (TimerEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PvotSpacing.L),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(timers, key = { it.id }) { item ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (timers.size > 1) {
                    Text(
                        timerLabel(item),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ExpiredButtons(item, onEvent)
            }
        }
    }
}

@Composable
private fun ExpiredTimerCard(item: TimerItem, remaining: Long, onEvent: (TimerEvent) -> Unit) {
    PvotCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(PvotSpacing.L)) {
            Text(timerLabel(item), style = MaterialTheme.typography.titleMedium)
            Text(
                overtime(remaining),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.error,
            )
            ExpiredButtons(item, onEvent, Modifier.padding(top = PvotSpacing.S))
        }
    }
}

@Composable
private fun ExpiredButtons(
    item: TimerItem,
    onEvent: (TimerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        PvotButton(
            text = stringResource(R.string.timer_plus_1_min),
            onClick = { onEvent(TimerEvent.AddMinute(item.id)) },
            style = PvotButtonStyle.Tonal,
        )
        PvotButton(
            text = stringResource(R.string.timer_stop),
            onClick = { onEvent(TimerEvent.Stop(item.id)) },
            style = PvotButtonStyle.Filled,
        )
    }
}

private fun overtime(remaining: Long): String {
    val total = abs(remaining) / DateUtils.SECOND_IN_MILLIS
    val text =
        if (total >= 3600) "%d:%02d:%02d".format(total / 3600, total / 60 % 60, total % 60)
        else "%d:%02d".format(total / 60, total % 60)
    return if (remaining < 0) "−$text" else text
}

private const val KEY_TITLE = "title"
