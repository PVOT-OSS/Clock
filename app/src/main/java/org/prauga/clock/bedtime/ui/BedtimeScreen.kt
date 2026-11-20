/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.time.LocalTime
import org.prauga.clock.R
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.bedtime.data.BedtimeRepository
import org.prauga.clock.bedtime.data.WakeUpAlarm
import org.prauga.clock.common.TabletopSplit
import org.prauga.clock.common.TimeDialog
import org.prauga.clock.common.TwoPane
import org.prauga.clock.common.formatTime
import org.prauga.clock.common.rememberWindowLayout
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Weekdays

private val REMINDERS = listOf(0, 15, 30, 45, 60)

@Composable
fun BedtimeScreen(
    padding: PaddingValues,
    listState: LazyListState,
    viewModel: BedtimeViewModel = bedtimeViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(BedtimeEvent.Refresh)
        onPauseOrDispose {}
    }
    val content = (state as? UiState.Success)?.data ?: return
    BedtimeBody(content, padding, listState, viewModel::onEvent)
}

@Composable
private fun bedtimeViewModel(): BedtimeViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel { BedtimeViewModel(BedtimeRepository(context, AlarmRepository(context))) }
}

@Composable
private fun BedtimeBody(
    content: BedtimeContent,
    padding: PaddingValues,
    listState: LazyListState,
    onEvent: (BedtimeEvent) -> Unit,
) {
    val bedtime = content.bedtime
    var pickBedtime by rememberSaveable { mutableStateOf(false) }
    var pickReminder by rememberSaveable { mutableStateOf(false) }
    var pickWakeUp by rememberSaveable { mutableStateOf(false) }
    var introPick by rememberSaveable { mutableStateOf(false) }
    var pickGoal by rememberSaveable { mutableStateOf(false) }

    val chooseWakeUp = {
        onEvent(BedtimeEvent.LoadWakeUpChoices)
        pickWakeUp = true
    }
    val actions =
        BedtimeCardActions(
            onStartIntro = {
                introPick = true
                pickBedtime = true
            },
            onPickReminder = { pickReminder = true },
            onChooseWakeUp = chooseWakeUp,
        )

    val onPickGoal = { pickGoal = true }
    val onPickBedtime = { pickBedtime = true }
    val layout = rememberWindowLayout()
    when {
        layout.tabletop ->
            TabletopSplit(
                top = {
                    BedtimeDial(
                        content,
                        onEvent,
                        onPickGoal,
                        Modifier.fillMaxSize()
                            .padding(
                                top = padding.calculateTopPadding() + PvotSpacing.S,
                                start = PvotSpacing.L,
                                end = PvotSpacing.L,
                                bottom = PvotSpacing.S,
                            ),
                        fitHeight = true,
                    )
                },
                bottom = {
                    PvotScreen(state = listState, contentPadding = screenPadding(PaddingValues())) {
                        item(key = "chips") { BedtimeChips(content, onPickBedtime, chooseWakeUp) }
                        bedtimeIntro(content, actions.onStartIntro)
                        bedtimeCards(content, onEvent, actions)
                    }
                },
            )
        layout.twoPane ->
            TwoPane(
                first = {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(screenPadding(padding)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PvotSpacing.L),
                    ) {
                        BedtimeDial(content, onEvent, onPickGoal, Modifier.fillMaxWidth())
                        BedtimeChips(content, onPickBedtime, chooseWakeUp)
                    }
                },
                second = {
                    PvotScreen(state = listState, contentPadding = screenPadding(padding)) {
                        bedtimeIntro(content, actions.onStartIntro)
                        bedtimeCards(content, onEvent, actions)
                    }
                },
            )
        else ->
            PvotScreen(state = listState, contentPadding = screenPadding(padding)) {
                bedtimeIntro(content, actions.onStartIntro)
                item(key = "dial") {
                    BedtimeDial(content, onEvent, onPickGoal, Modifier.fillMaxWidth())
                }
                item(key = "chips") { BedtimeChips(content, onPickBedtime, chooseWakeUp) }
                bedtimeCards(content, onEvent, actions)
            }
    }

    TimeDialog(
        visible = pickBedtime,
        initial = LocalTime.of(bedtime.hour, bedtime.minute),
        onDismiss = {
            pickBedtime = false
            introPick = false
        },
        onConfirm = {
            onEvent(BedtimeEvent.SetTime(it.hour, it.minute))
            pickBedtime = false
            if (introPick) {
                introPick = false
                onEvent(BedtimeEvent.FinishIntro)
                chooseWakeUp()
            }
        },
    )
    PvotDialog(
        visible = pickReminder,
        onDismissRequest = { pickReminder = false },
        title = stringResource(R.string.bedtime_reminder_title),
        actions = {
            REMINDERS.forEach { minutes ->
                PvotDialogAction(
                    label = reminderText(minutes),
                    onClick = {
                        onEvent(BedtimeEvent.SetReminder(minutes))
                        pickReminder = false
                    },
                )
            }
        },
    )
    SleepGoalSheet(
        visible = pickGoal,
        goalMinutes = bedtime.sleepGoalMinutes,
        onDismiss = { pickGoal = false },
        onSelect = {
            onEvent(BedtimeEvent.SetSleepGoal(it))
            pickGoal = false
        },
    )
    WakeUpChooser(
        visible = pickWakeUp,
        choices = content.wakeUpChoices,
        onEvent = onEvent,
        onDone = { pickWakeUp = false },
    )
}

@Composable
private fun WakeUpChooser(
    visible: Boolean,
    choices: List<WakeUpAlarm>?,
    onEvent: (BedtimeEvent) -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val wakeUpTitle = stringResource(R.string.wakeup_title)
    val order = DataModel.getDataModel().weekdayOrder
    PvotDialog(
        visible = visible && choices != null,
        onDismissRequest = onDone,
        title = wakeUpTitle,
        actions = {
            choices.orEmpty().forEach { alarm ->
                val days = Weekdays.fromBits(alarm.days)
                val daysText =
                    if (days.isRepeating) days.toString(context, order)
                    else stringResource(R.string.wakeup_once)
                PvotDialogAction(
                    label = "${formatTime(context, alarm.hour, alarm.minute)} · $daysText",
                    onClick = {
                        onEvent(BedtimeEvent.ChooseWakeUp(alarm.id))
                        onDone()
                    },
                )
            }
            PvotDialogAction(
                label = stringResource(R.string.wakeup_new_alarm),
                onClick = {
                    onEvent(BedtimeEvent.AddWakeUpAlarm(wakeUpTitle))
                    onDone()
                },
            )
        },
    )
}
