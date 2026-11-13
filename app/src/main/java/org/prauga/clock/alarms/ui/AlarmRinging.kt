/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.prauga.pvot.designsystem.components.PvotSlideTarget
import com.prauga.pvot.designsystem.components.PvotSlideToAct
import com.prauga.pvot.designsystem.components.PvotSlideToActDefaults
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlinx.coroutines.delay
import org.prauga.clock.R

class RingingState {
    var outcome by mutableStateOf<Outcome?>(null)
        private set

    fun snoozed(minutes: Int) {
        outcome = Outcome.Snoozed(minutes)
    }

    fun dismissed() {
        outcome = Outcome.Dismissed
    }

    sealed interface Outcome {
        data class Snoozed(val minutes: Int) : Outcome

        data object Dismissed : Outcome
    }
}

object AlarmRinging {

    @JvmStatic
    fun show(
        activity: ComponentActivity,
        label: String,
        tapToAct: Boolean,
        state: RingingState,
        onSnooze: Runnable,
        onDismiss: Runnable,
    ) {
        activity.setContent {
            PvotAppTheme(darkTheme = true) {
                // Text outside a Surface is black unless the content color is given.
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground
                ) {
                    AlarmRingingScreen(label, tapToAct, state, onSnooze::run, onDismiss::run)
                }
            }
        }
    }
}

@Composable
private fun AlarmRingingScreen(
    label: String,
    tapToAct: Boolean,
    state: RingingState,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(PvotSpacing.L)
    ) {
        AnimatedContent(
            targetState = state.outcome,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize(),
            label = "ringing",
        ) { outcome ->
            if (outcome == null) {
                Ringing(label, tapToAct, onSnooze, onDismiss)
            } else {
                Handled(outcome)
            }
        }
    }
}

@Composable
private fun Ringing(label: String, tapToAct: Boolean, onSnooze: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE - System.currentTimeMillis() % MINUTE)
            now = System.currentTimeMillis()
        }
    }
    val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                DateFormat.format(DateFormat.getBestDateTimePattern(locale, skeleton), now)
                    .toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
            )
            Text(
                DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEEMMMMd"), now)
                    .toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (tapToAct) {
            Row(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M)) {
                PvotButton(
                    text = stringResource(R.string.alarm_alert_snooze_text),
                    onClick = onSnooze,
                    style = PvotButtonStyle.Tonal,
                )
                PvotButton(
                    text = stringResource(R.string.alarm_alert_dismiss_text),
                    onClick = onDismiss,
                    style = PvotButtonStyle.Filled,
                )
            }
        } else {
            PvotSlideToAct(
                icon = painterResource(R.drawable.ic_t_alarm),
                contentDescription = label,
                up =
                    PvotSlideTarget(
                        label = stringResource(R.string.alarm_alert_dismiss_text),
                        onReached = onDismiss,
                        distance = PvotSlideToActDefaults.UpDistance,
                        hint = stringResource(R.string.alarm_slide_dismiss),
                    ),
                down =
                    PvotSlideTarget(
                        label = stringResource(R.string.alarm_alert_snooze_text),
                        onReached = onSnooze,
                        distance = PvotSlideToActDefaults.DownDistance,
                        hint = stringResource(R.string.alarm_slide_snooze),
                    ),
                puckColor = MaterialTheme.colorScheme.primary,
                puckContentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun Handled(outcome: RingingState.Outcome) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (outcome) {
            is RingingState.Outcome.Snoozed -> {
                Text(
                    stringResource(R.string.alarm_alert_snoozed_text),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    pluralStringResource(
                        R.plurals.alarm_alert_snooze_duration,
                        outcome.minutes,
                        outcome.minutes,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RingingState.Outcome.Dismissed ->
                Text(
                    stringResource(R.string.alarm_alert_off_text),
                    style = MaterialTheme.typography.headlineLarge,
                )
        }
    }
}

private const val MINUTE = 60_000L
