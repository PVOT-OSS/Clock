/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui.ringing

import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlin.math.abs
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Timer
import org.prauga.clock.data.TimerListener

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
private fun ExpiredTimersScreen() {
    val model = DataModel.getDataModel()
    val activity = LocalActivity.current
    // Timer.equals compares ids only: keep every update, and redraw on each version.
    var timers by remember { mutableStateOf(model.expiredTimers.toList(), neverEqualPolicy()) }
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val listener =
            object : TimerListener {
                override fun timerAdded(timer: Timer) = reload()

                override fun timerUpdated(before: Timer, after: Timer) = reload()

                override fun timerRemoved(timer: Timer) = reload()

                private fun reload() {
                    timers = model.expiredTimers.toList()
                    version++
                }
            }
        model.addTimerListener(listener)
        onDispose { model.removeTimerListener(listener) }
    }
    LaunchedEffect(timers) { if (timers.isEmpty()) activity?.finish() }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { now = it }
        }
    }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(PvotSpacing.L),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.timer_times_up),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = PvotSpacing.L),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(PvotSpacing.M)) {
            items(timers, key = { it.id }) { timer ->
                // Read the clock and version so the card redraws as the overtime grows.
                now.let {}
                version.let {}
                ExpiredTimerCard(model.getTimer(timer.id) ?: timer)
            }
        }
    }
}

@Composable
private fun ExpiredTimerCard(timer: Timer) {
    val model = DataModel.getDataModel()
    PvotCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(PvotSpacing.L)) {
            Text(
                timer.label.ifEmpty { stringResource(R.string.timer_notification_label) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                overtime(timer.remainingTime),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.error,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = PvotSpacing.S),
            ) {
                PvotButton(
                    text = stringResource(R.string.timer_plus_1_min),
                    onClick = { model.addTimerMinute(timer) },
                    style = PvotButtonStyle.Tonal,
                )
                PvotButton(
                    text = stringResource(R.string.timer_stop),
                    onClick = { model.resetOrDeleteTimer(timer, R.string.label_deskclock) },
                    style = PvotButtonStyle.Filled,
                )
            }
        }
    }
}

private fun overtime(remaining: Long): String {
    val total = abs(remaining) / DateUtils.SECOND_IN_MILLIS
    val text =
        if (total >= 3600) "%d:%02d:%02d".format(total / 3600, total / 60 % 60, total % 60)
        else "%d:%02d".format(total / 60, total % 60)
    return if (remaining < 0) "−$text" else text
}
