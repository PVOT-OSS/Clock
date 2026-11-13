/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotSwitch
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.time.LocalTime
import org.prauga.clock.R
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.bedtime.data.BedtimeRepository
import org.prauga.clock.bedtime.data.BedtimeSnapshot
import org.prauga.clock.bedtime.data.WakeUpAlarm
import org.prauga.clock.common.TimeDialog
import org.prauga.clock.common.WeekdayButtons
import org.prauga.clock.common.formatTime
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Weekdays

private val REMINDERS = listOf(0, 15, 30, 45, 60)
private val SLEEP_LENGTHS = listOf(15, 30, 60)

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
    val context = LocalContext.current
    val bedtime = content.bedtime
    var pickBedtime by remember { mutableStateOf(false) }
    var pickReminder by remember { mutableStateOf(false) }
    var pickWakeUp by remember { mutableStateOf(false) }
    var introPick by remember { mutableStateOf(false) }

    PvotScreen(
        state = listState,
        contentPadding = screenPadding(padding),
        horizontalAlignment = Alignment.Start,
    ) {
        if (!bedtime.introDone) {
            item(key = "intro") {
                Card(stringResource(R.string.bedtime_intro_title)) {
                    Text(stringResource(R.string.bedtime_intro_text))
                    PvotButton(
                        text = stringResource(R.string.bedtime_intro_start),
                        onClick = {
                            introPick = true
                            pickBedtime = true
                        },
                        modifier = Modifier.padding(top = PvotSpacing.S),
                    )
                }
            }
        }
        item(key = "schedule") {
            Card(
                title = stringResource(R.string.bedtime_schedule_title),
                trailing = {
                    PvotSwitch(
                        checked = bedtime.enabled,
                        onCheckedChange = { onEvent(BedtimeEvent.SetEnabled(it)) },
                    )
                },
            ) {
                Text(
                    text = formatTime(context, bedtime.hour, bedtime.minute),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.clickable { pickBedtime = true },
                )
                WeekdayButtons(Weekdays.fromBits(bedtime.days)) { day, on ->
                    onEvent(BedtimeEvent.SetDay(day, on))
                }
                Text(
                    text = reminderText(bedtime.reminderMinutes),
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { pickReminder = true }
                            .padding(vertical = PvotSpacing.S),
                )
            }
        }
        item(key = "wakeup") {
            WakeUpCard(bedtime) {
                onEvent(BedtimeEvent.LoadWakeUpChoices)
                pickWakeUp = true
            }
        }
        item(key = "mode") { ModeCard(bedtime, onEvent) }
        item(key = "sounds") { SleepSoundsCard(bedtime, onEvent) }
        item(key = "tomorrow") { TomorrowCard(content.tomorrow, onEvent) }
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
                onEvent(BedtimeEvent.LoadWakeUpChoices)
                pickWakeUp = true
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
    WakeUpChooser(
        visible = pickWakeUp,
        choices = content.wakeUpChoices,
        onEvent = onEvent,
        onDone = { pickWakeUp = false },
    )
}

@Composable
private fun Card(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    PvotCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(PvotSpacing.L)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                trailing?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun reminderText(minutes: Int): String =
    if (minutes == 0) stringResource(R.string.bedtime_reminder_off)
    else pluralStringResource(R.plurals.bedtime_reminder_before, minutes, minutes)

@Composable
private fun WakeUpCard(bedtime: BedtimeSnapshot, onChoose: () -> Unit) {
    val context = LocalContext.current
    val linked = bedtime.wakeUp
    Card(stringResource(R.string.wakeup_title)) {
        Text(
            text =
                if (linked == null) stringResource(R.string.wakeup_not_set)
                else formatTime(context, linked.hour, linked.minute),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text =
                when {
                    linked == null -> stringResource(R.string.wakeup_choose_hint)
                    !linked.enabled -> stringResource(R.string.wakeup_alarm_off)
                    else -> {
                        val bed = bedtime.hour * 60 + bedtime.minute
                        val sleep = ((linked.hour * 60 + linked.minute - bed) % 1440 + 1440) % 1440
                        stringResource(
                            R.string.wakeup_sleep_duration,
                            pluralStringResource(R.plurals.hours, sleep / 60, sleep / 60),
                            pluralStringResource(R.plurals.minutes, sleep % 60, sleep % 60),
                        )
                    }
                }
        )
        PvotButton(
            text = stringResource(R.string.wakeup_choose),
            onClick = onChoose,
            style = PvotButtonStyle.Text,
        )
    }
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

@Composable
private fun ModeCard(bedtime: BedtimeSnapshot, onEvent: (BedtimeEvent) -> Unit) {
    val context = LocalContext.current
    val needsAccess = bedtime.modeEnabled && !bedtime.hasModeAccess
    Card(
        title = stringResource(R.string.bedtime_mode_title),
        trailing = {
            PvotSwitch(
                checked = bedtime.modeEnabled,
                onCheckedChange = {
                    if (it && !bedtime.hasModeAccess) requestAccess(context)
                    onEvent(BedtimeEvent.SetMode(it))
                },
            )
        },
    ) {
        Text(
            text =
                stringResource(
                    if (needsAccess) R.string.bedtime_mode_needs_access
                    else R.string.bedtime_mode_summary
                ),
            modifier = if (needsAccess) Modifier.clickable { requestAccess(context) } else Modifier,
        )
    }
}

@Composable
private fun SleepSoundsCard(bedtime: BedtimeSnapshot, onEvent: (BedtimeEvent) -> Unit) {
    val noises =
        listOf(
            R.string.sleep_sound_white_short,
            R.string.sleep_sound_pink_short,
            R.string.sleep_sound_brown_short,
        )
    Card(stringResource(R.string.sleep_sounds_title)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            modifier = Modifier.padding(top = PvotSpacing.S),
        ) {
            noises.forEachIndexed { i, label ->
                PvotButton(
                    text = stringResource(label),
                    onClick = { onEvent(BedtimeEvent.SetSleepSound(i)) },
                    style =
                        if (bedtime.sleepSound == i) PvotButtonStyle.Filled
                        else PvotButtonStyle.Tonal,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            modifier = Modifier.padding(top = PvotSpacing.S),
        ) {
            SLEEP_LENGTHS.forEach { minutes ->
                PvotButton(
                    text = stringResource(R.string.timer_preset_minutes, minutes),
                    onClick = { onEvent(BedtimeEvent.SetSleepMinutes(minutes)) },
                    style =
                        if (bedtime.sleepSoundMinutes == minutes) PvotButtonStyle.Filled
                        else PvotButtonStyle.Tonal,
                )
            }
        }
        PvotButton(
            text =
                stringResource(
                    if (bedtime.sleepSoundPlaying) R.string.sleep_sound_stop
                    else R.string.sleep_sound_play
                ),
            onClick = { onEvent(BedtimeEvent.ToggleSleepSound) },
            modifier = Modifier.padding(top = PvotSpacing.S),
        )
    }
}

@Composable
private fun TomorrowCard(tomorrow: String?, onEvent: (BedtimeEvent) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
        }
    LaunchedEffect(granted) { if (granted) onEvent(BedtimeEvent.LoadTomorrow) }
    Card(stringResource(R.string.calendar_title)) {
        if (granted) {
            Text(tomorrow.orEmpty(), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(R.string.calendar_permission_hint))
            PvotButton(
                text = stringResource(R.string.calendar_allow),
                onClick = { launcher.launch(Manifest.permission.READ_CALENDAR) },
                style = PvotButtonStyle.Text,
            )
        }
    }
}

private fun requestAccess(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
}
