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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotSegmentedControl
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotPreferenceRow
import com.prauga.pvot.designsystem.components.list.PvotRowIconStyle
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.bedtime.data.BedtimeSnapshot
import org.prauga.clock.common.WeekdayButtons
import org.prauga.clock.common.formatTime
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Weekdays

private val SLEEP_LENGTHS = listOf(15, 30, 60)
private val NOISES =
    listOf(
        R.string.sleep_sound_white_short,
        R.string.sleep_sound_pink_short,
        R.string.sleep_sound_brown_short,
    )

class BedtimeCardActions(
    val onStartIntro: () -> Unit,
    val onPickReminder: () -> Unit,
    val onChooseWakeUp: () -> Unit,
)

fun LazyListScope.bedtimeCards(
    content: BedtimeContent,
    onEvent: (BedtimeEvent) -> Unit,
    actions: BedtimeCardActions,
) {
    val bedtime = content.bedtime
    item(key = "schedule") { ScheduleGroup(bedtime, onEvent, actions.onPickReminder) }
    item(key = "wakeup") { WakeUpGroup(bedtime, onEvent, actions.onChooseWakeUp) }
    item(key = "mode") { ModeGroup(bedtime, onEvent) }
    item(key = "sounds") { SleepSoundsGroup(bedtime, onEvent) }
    item(key = "tomorrow") { TomorrowGroup(content.tomorrow, onEvent) }
}

fun LazyListScope.bedtimeIntro(content: BedtimeContent, onStart: () -> Unit) {
    if (!content.bedtime.introDone) {
        item(key = "intro") { IntroCard(onStart) }
    }
}

@Composable
private fun IntroCard(onStart: () -> Unit) {
    PvotCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(PvotSpacing.L)) {
            Text(
                stringResource(R.string.bedtime_intro_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.bedtime_intro_text),
                modifier = Modifier.padding(top = PvotSpacing.S),
            )
            PvotButton(
                text = stringResource(R.string.bedtime_intro_start),
                onClick = onStart,
                modifier = Modifier.padding(top = PvotSpacing.M),
            )
        }
    }
}

@Composable
private fun ScheduleGroup(
    bedtime: BedtimeSnapshot,
    onEvent: (BedtimeEvent) -> Unit,
    onPickReminder: () -> Unit,
) {
    PvotListGroup(title = stringResource(R.string.bedtime_schedule_title)) {
        item {
            PvotSwitchRow(
                title = stringResource(R.string.bedtime_schedule_title),
                checked = bedtime.enabled,
                onCheckedChange = { onEvent(BedtimeEvent.SetEnabled(it)) },
                icon = { RowIcon(R.drawable.ic_t_moon) },
                iconStyle = PvotRowIconStyle.Plain,
            )
        }
        item {
            Column(Modifier.padding(horizontal = PvotSpacing.L)) {
                WeekdayButtons(Weekdays.fromBits(bedtime.days)) { day, on ->
                    onEvent(BedtimeEvent.SetDay(day, on))
                }
            }
        }
        item {
            PvotPreferenceRow(
                title = stringResource(R.string.bedtime_reminder_title),
                onClick = onPickReminder,
                value = reminderValue(bedtime.reminderMinutes),
                icon = { RowIcon(R.drawable.ic_t_bell) },
                iconStyle = PvotRowIconStyle.Plain,
                showChevron = false,
            )
        }
    }
}

@Composable
private fun WakeUpGroup(
    bedtime: BedtimeSnapshot,
    onEvent: (BedtimeEvent) -> Unit,
    onChoose: () -> Unit,
) {
    val context = LocalContext.current
    val linked = bedtime.wakeUp
    PvotListGroup(title = stringResource(R.string.wakeup_title)) {
        if (linked != null) {
            item {
                val days = Weekdays.fromBits(linked.days)
                PvotSwitchRow(
                    title = formatTime(context, linked.hour, linked.minute),
                    checked = linked.enabled,
                    onCheckedChange = { onEvent(BedtimeEvent.SetWakeUpEnabled(it)) },
                    summary =
                        if (days.isRepeating) {
                            days.toString(context, DataModel.getDataModel().weekdayOrder)
                        } else {
                            stringResource(R.string.wakeup_once)
                        },
                    icon = { RowIcon(R.drawable.ic_t_alarm) },
                    iconStyle = PvotRowIconStyle.Plain,
                )
            }
        }
        item {
            PvotPreferenceRow(
                title = stringResource(R.string.wakeup_choose),
                onClick = onChoose,
                summary = if (linked == null) stringResource(R.string.wakeup_choose_hint) else null,
                icon =
                    if (linked == null) {
                        { RowIcon(R.drawable.ic_t_alarm) }
                    } else {
                        null
                    },
                iconStyle = PvotRowIconStyle.Plain,
            )
        }
    }
}

@Composable
private fun ModeGroup(bedtime: BedtimeSnapshot, onEvent: (BedtimeEvent) -> Unit) {
    val context = LocalContext.current
    val needsAccess = bedtime.modeEnabled && !bedtime.hasModeAccess
    PvotListGroup {
        item {
            PvotSwitchRow(
                title = stringResource(R.string.bedtime_mode_title),
                checked = bedtime.modeEnabled,
                onCheckedChange = {
                    if (it && !bedtime.hasModeAccess) requestAccess(context)
                    onEvent(BedtimeEvent.SetMode(it))
                },
                summary = stringResource(R.string.bedtime_mode_summary),
                icon = { RowIcon(R.drawable.ic_t_moon) },
                iconStyle = PvotRowIconStyle.Plain,
            )
        }
        if (needsAccess) {
            item {
                PvotPreferenceRow(
                    title = stringResource(R.string.bedtime_mode_needs_access),
                    onClick = { requestAccess(context) },
                )
            }
        }
    }
}

@Composable
private fun SleepSoundsGroup(bedtime: BedtimeSnapshot, onEvent: (BedtimeEvent) -> Unit) {
    PvotListGroup(title = stringResource(R.string.sleep_sounds_title)) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
                modifier = Modifier.padding(PvotSpacing.L),
            ) {
                PvotSegmentedControl(
                    options = NOISES.map { stringResource(it) },
                    selectedIndex = bedtime.sleepSound,
                    onSelect = { onEvent(BedtimeEvent.SetSleepSound(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                PvotSegmentedControl(
                    options =
                        SLEEP_LENGTHS.map { stringResource(R.string.timer_preset_minutes, it) },
                    selectedIndex =
                        SLEEP_LENGTHS.indexOf(bedtime.sleepSoundMinutes).coerceAtLeast(0),
                    onSelect = { onEvent(BedtimeEvent.SetSleepMinutes(SLEEP_LENGTHS[it])) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row {
                    PvotButton(
                        text =
                            stringResource(
                                if (bedtime.sleepSoundPlaying) R.string.sleep_sound_stop
                                else R.string.sleep_sound_play
                            ),
                        onClick = { onEvent(BedtimeEvent.ToggleSleepSound) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TomorrowGroup(tomorrow: String?, onEvent: (BedtimeEvent) -> Unit) {
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
    PvotListGroup(title = stringResource(R.string.calendar_title)) {
        item {
            if (granted) {
                Text(
                    tomorrow.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(PvotSpacing.L),
                )
            } else {
                PvotPreferenceRow(
                    title = stringResource(R.string.calendar_allow),
                    onClick = { launcher.launch(Manifest.permission.READ_CALENDAR) },
                    summary = stringResource(R.string.calendar_permission_hint),
                    icon = { RowIcon(R.drawable.ic_t_calendar) },
                    iconStyle = PvotRowIconStyle.Plain,
                )
            }
        }
    }
}

@Composable
private fun RowIcon(@DrawableRes icon: Int) {
    Icon(painterResource(icon), contentDescription = null)
}

@Composable
internal fun reminderText(minutes: Int): String =
    if (minutes == 0) stringResource(R.string.bedtime_reminder_off)
    else pluralStringResource(R.plurals.bedtime_reminder_before, minutes, minutes)

@Composable
private fun reminderValue(minutes: Int): String =
    if (minutes == 0) stringResource(R.string.bedtime_reminder_off)
    else pluralStringResource(R.plurals.bedtime_reminder_short, minutes, minutes)

private fun requestAccess(context: Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
}
