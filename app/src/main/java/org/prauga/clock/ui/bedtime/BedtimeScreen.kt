/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui.bedtime

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
import androidx.compose.runtime.mutableIntStateOf
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
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotSwitch
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.clock.R
import org.prauga.clock.alarms.AlarmUpdateHandler
import org.prauga.clock.alarms.ScrollHandler
import org.prauga.clock.bedtime.BedtimeMode
import org.prauga.clock.bedtime.BedtimeScheduler
import org.prauga.clock.bedtime.BedtimeSettings
import org.prauga.clock.bedtime.SleepSoundService
import org.prauga.clock.bedtime.TomorrowsEvent
import org.prauga.clock.data.DataModel
import org.prauga.clock.provider.Alarm
import org.prauga.clock.ui.WeekdayButtons
import org.prauga.clock.ui.alarms.TimeDialog
import org.prauga.clock.ui.alarms.formatTime
import org.prauga.clock.ui.screenPadding

private val REMINDERS = listOf(0, 15, 30, 45, 60)
private val SLEEP_LENGTHS = listOf(15, 30, 60)

@Composable
fun BedtimeScreen(padding: PaddingValues, listState: LazyListState) {
    val context = LocalContext.current
    val settings = remember { BedtimeSettings(context) }
    var version by remember { mutableIntStateOf(0) }
    val changed: () -> Unit = {
        BedtimeScheduler.schedule(context)
        version += 1
    }
    LifecycleResumeEffect(Unit) {
        changed()
        onPauseOrDispose {}
    }
    var pickBedtime by remember { mutableStateOf(false) }
    var pickReminder by remember { mutableStateOf(false) }
    var pickWakeUp by remember { mutableStateOf(false) }
    var afterBedtime by remember { mutableStateOf<(() -> Unit)?>(null) }

    PvotScreen(
        state = listState,
        contentPadding = screenPadding(padding),
        horizontalAlignment = Alignment.Start,
    ) {
        if (!settings.introDone) {
            item(key = "intro") {
                Card(stringResource(R.string.bedtime_intro_title)) {
                    Text(stringResource(R.string.bedtime_intro_text))
                    PvotButton(
                        text = stringResource(R.string.bedtime_intro_start),
                        onClick = {
                            afterBedtime = {
                                settings.introDone = true
                                pickWakeUp = true
                            }
                            pickBedtime = true
                        },
                        modifier = Modifier.padding(top = PvotSpacing.S),
                    )
                }
            }
        }
        item(key = "schedule") {
            version.let {}
            Card(
                title = stringResource(R.string.bedtime_schedule_title),
                trailing = {
                    PvotSwitch(
                        checked = settings.enabled,
                        onCheckedChange = {
                            settings.enabled = it
                            changed()
                        },
                    )
                },
            ) {
                Text(
                    text = formatTime(context, settings.hour, settings.minute),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.clickable { pickBedtime = true },
                )
                WeekdayButtons(settings.days) { day, on ->
                    settings.days = settings.days.setBit(day, on)
                    changed()
                }
                Text(
                    text = reminderText(settings.reminderMinutes),
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { pickReminder = true }
                            .padding(vertical = PvotSpacing.S),
                )
            }
        }
        item(key = "wakeup") {
            version.let {}
            WakeUpCard(settings, version) { pickWakeUp = true }
        }
        item(key = "mode") {
            version.let {}
            val access = BedtimeMode.hasAccess(context)
            Card(
                title = stringResource(R.string.bedtime_mode_title),
                trailing = {
                    PvotSwitch(
                        checked = settings.modeEnabled,
                        onCheckedChange = {
                            settings.modeEnabled = it
                            if (it && !BedtimeMode.hasAccess(context)) requestAccess(context)
                            if (!it) BedtimeMode.remove(context)
                            changed()
                        },
                    )
                },
            ) {
                Text(
                    text =
                        stringResource(
                            if (settings.modeEnabled && !access) R.string.bedtime_mode_needs_access
                            else R.string.bedtime_mode_summary
                        ),
                    modifier =
                        if (settings.modeEnabled && !access) {
                            Modifier.clickable { requestAccess(context) }
                        } else Modifier,
                )
            }
        }
        item(key = "sounds") {
            version.let {}
            SleepSoundsCard(settings, changed)
        }
        item(key = "tomorrow") { TomorrowCard() }
    }

    TimeDialog(
        visible = pickBedtime,
        initial = LocalTime.of(settings.hour, settings.minute),
        onDismiss = {
            pickBedtime = false
            afterBedtime = null
        },
        onConfirm = {
            settings.hour = it.hour
            settings.minute = it.minute
            settings.enabled = true
            pickBedtime = false
            changed()
            afterBedtime?.invoke()
            afterBedtime = null
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
                        settings.reminderMinutes = minutes
                        pickReminder = false
                        changed()
                    },
                )
            }
        },
    )
    WakeUpChooser(
        visible = pickWakeUp,
        settings = settings,
        onDone = {
            pickWakeUp = false
            changed()
        },
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
private fun WakeUpCard(settings: BedtimeSettings, version: Int, onChoose: () -> Unit) {
    val context = LocalContext.current
    var alarm by remember { mutableStateOf<Alarm?>(null) }
    LaunchedEffect(version, settings.wakeUpAlarmId) {
        val id = settings.wakeUpAlarmId
        alarm =
            if (id < 0) null
            else withContext(Dispatchers.IO) { Alarm.getAlarm(context.contentResolver, id) }
    }
    Card(stringResource(R.string.wakeup_title)) {
        val linked = alarm
        Text(
            text =
                if (linked == null) stringResource(R.string.wakeup_not_set)
                else formatTime(context, linked.hour, linked.minutes),
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text =
                when {
                    linked == null -> stringResource(R.string.wakeup_choose_hint)
                    !linked.enabled -> stringResource(R.string.wakeup_alarm_off)
                    else -> {
                        val bed = settings.hour * 60 + settings.minute
                        val sleep = ((linked.hour * 60 + linked.minutes - bed) % 1440 + 1440) % 1440
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
private fun WakeUpChooser(visible: Boolean, settings: BedtimeSettings, onDone: () -> Unit) {
    val context = LocalContext.current
    val wakeUpTitle = stringResource(R.string.wakeup_title)
    var alarms by remember { mutableStateOf<List<Alarm>?>(null) }
    LaunchedEffect(visible) {
        if (visible) {
            alarms =
                withContext(Dispatchers.IO) {
                    Alarm.getAlarms(context.contentResolver, null)
                        .sortedWith(compareBy({ it.hour }, { it.minutes }))
                }
        }
    }
    val order = DataModel.getDataModel().weekdayOrder
    PvotDialog(
        visible = visible && alarms != null,
        onDismissRequest = onDone,
        title = stringResource(R.string.wakeup_title),
        actions = {
            alarms.orEmpty().forEach { a ->
                val days =
                    if (a.daysOfWeek.isRepeating) a.daysOfWeek.toString(context, order)
                    else stringResource(R.string.wakeup_once)
                PvotDialogAction(
                    label = "${formatTime(context, a.hour, a.minutes)} · $days",
                    onClick = {
                        settings.wakeUpAlarmId = a.id
                        onDone()
                    },
                )
            }
            PvotDialogAction(
                label = stringResource(R.string.wakeup_new_alarm),
                onClick = {
                    val wake = (settings.hour * 60 + settings.minute + 8 * 60) % 1440
                    AlarmUpdateHandler(
                            context,
                            object : ScrollHandler {
                                override fun setSmoothScrollStableId(stableId: Long) {
                                    settings.wakeUpAlarmId = stableId
                                }

                                override fun smoothScrollTo(position: Int) {}
                            },
                        )
                        .asyncAddAlarm(
                            Alarm(wake / 60, wake % 60).apply {
                                daysOfWeek = settings.days
                                enabled = true
                                label = wakeUpTitle
                            }
                        )
                    onDone()
                },
            )
        },
    )
}

@Composable
private fun SleepSoundsCard(settings: BedtimeSettings, changed: () -> Unit) {
    val context = LocalContext.current
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
                    onClick = {
                        settings.sleepSound = i
                        changed()
                    },
                    style =
                        if (settings.sleepSound == i) PvotButtonStyle.Filled
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
                    onClick = {
                        settings.sleepSoundMinutes = minutes
                        changed()
                    },
                    style =
                        if (settings.sleepSoundMinutes == minutes) PvotButtonStyle.Filled
                        else PvotButtonStyle.Tonal,
                )
            }
        }
        PvotButton(
            text =
                stringResource(
                    if (SleepSoundService.isPlaying) R.string.sleep_sound_stop
                    else R.string.sleep_sound_play
                ),
            onClick = {
                if (SleepSoundService.isPlaying) {
                    SleepSoundService.stop(context)
                } else {
                    SleepSoundService.start(
                        context,
                        SleepSoundService.Noise.entries[settings.sleepSound],
                        settings.sleepSoundMinutes,
                    )
                }
                changed()
            },
            modifier = Modifier.padding(top = PvotSpacing.S),
        )
    }
}

@Composable
private fun TomorrowCard() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var text by remember { mutableStateOf<String?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = it
        }
    LaunchedEffect(granted) {
        if (granted) {
            text = withContext(Dispatchers.IO) { TomorrowsEvent.describe(context) }
        }
    }
    Card(stringResource(R.string.calendar_title)) {
        if (granted) {
            Text(text.orEmpty(), style = MaterialTheme.typography.bodyLarge)
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
