/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotSwitch
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.clock.AlarmUtils
import org.prauga.clock.R
import org.prauga.clock.Utils
import org.prauga.clock.alarms.AlarmDatePicker
import org.prauga.clock.alarms.AlarmStateManager
import org.prauga.clock.alarms.AlarmUpdateHandler
import org.prauga.clock.data.DataModel
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmDates
import org.prauga.clock.provider.AlarmInstance
import org.prauga.clock.ringtone.RingtonePickerActivity
import org.prauga.clock.ui.WeekdayButtons

@Composable
fun AlarmCard(
    modifier: Modifier = Modifier,
    alarm: Alarm,
    version: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEditTime: () -> Unit,
    updater: AlarmUpdateHandler,
) {
    val context = LocalContext.current
    var editingLabel by remember { mutableStateOf(false) }
    var editingDate by remember { mutableStateOf(false) }
    PvotCard(modifier = modifier.fillMaxWidth(), onClick = onToggleExpanded) {
        Column(Modifier.padding(16.dp)) {
            if (alarm.label.isNotEmpty() || expanded) {
                Text(
                    text = alarm.label.ifEmpty { stringResource(R.string.add_label) },
                    style = MaterialTheme.typography.titleSmall,
                    modifier =
                        if (expanded) Modifier.clickable { editingLabel = true } else Modifier,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTime(context, alarm.hour, alarm.minutes),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = if (alarm.enabled) FontWeight.Normal else FontWeight.Light,
                    color =
                        if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).clickable(onClick = onEditTime),
                )
                PvotSwitch(
                    checked = alarm.enabled,
                    onCheckedChange = {
                        alarm.enabled = it
                        updater.asyncUpdateAlarm(alarm, it, false)
                    },
                )
            }
            Text(text = summary(context, alarm), style = MaterialTheme.typography.bodyMedium)
            PredismissButton(alarm, version, updater)

            if (expanded) {
                WeekdayButtons(alarm.daysOfWeek) { calendarDay, on ->
                    alarm.daysOfWeek = alarm.daysOfWeek.setBit(calendarDay, on)
                    if (alarm.daysOfWeek.isRepeating) {
                        alarm.scheduledDate = AlarmDates.NONE
                    } else {
                        alarm.pauseStart = AlarmDates.NONE
                        alarm.pauseEnd = AlarmDates.NONE
                    }
                    updater.asyncUpdateAlarm(alarm, true, false)
                }
                SettingRow(
                    icon = R.drawable.ic_t_bell,
                    text = DataModel.getDataModel().getRingtoneTitle(alarm.alert),
                    onClick = {
                        context.startActivity(
                            RingtonePickerActivity.createAlarmRingtonePickerIntent(context, alarm)
                        )
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SettingRow(
                        icon = R.drawable.ic_t_device_mobile_vibration,
                        text = stringResource(R.string.alarm_vibrate),
                        onClick = {
                            alarm.vibrate = !alarm.vibrate
                            updater.asyncUpdateAlarm(alarm, false, true)
                        },
                        modifier = Modifier.weight(1f),
                    )
                    PvotSwitch(
                        checked = alarm.vibrate,
                        onCheckedChange = {
                            alarm.vibrate = it
                            updater.asyncUpdateAlarm(alarm, false, true)
                        },
                    )
                }
                SettingRow(
                    icon = R.drawable.ic_t_calendar,
                    text = AlarmDatePicker.rowText(context, alarm),
                    onClick = { editingDate = true },
                )
                PvotButton(
                    text = stringResource(R.string.delete),
                    onClick = { updater.asyncDeleteAlarm(alarm) },
                    style = PvotButtonStyle.Text,
                )
            }
        }
    }
    LabelDialog(
        visible = editingLabel,
        initial = alarm.label,
        onDismiss = { editingLabel = false },
        onConfirm = {
            alarm.label = it
            updater.asyncUpdateAlarm(alarm, false, true)
            editingLabel = false
        },
    )
    if (editingDate) {
        AlarmDateDialog(
            alarm = alarm,
            onDismiss = { editingDate = false },
            onChanged = {
                updater.asyncUpdateAlarm(alarm, alarm.enabled, false)
                editingDate = false
            },
        )
    }
}

@Composable
private fun SettingRow(
    icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun summary(context: android.content.Context, alarm: Alarm): String {
    AlarmDatePicker.summary(context, alarm, Calendar.getInstance())?.let {
        return it
    }
    if (alarm.daysOfWeek.isRepeating) {
        return alarm.daysOfWeek.toString(context, DataModel.getDataModel().weekdayOrder)
    }
    val silent = alarm.alert == Utils.RINGTONE_SILENT
    val day =
        context.getString(
            if (Alarm.isTomorrow(alarm, Calendar.getInstance())) R.string.alarm_tomorrow
            else R.string.alarm_today
        )
    return if (silent) "$day · ${context.getString(R.string.alarm_badge_silent)}" else day
}

internal fun formatTime(context: android.content.Context, hour: Int, minute: Int): String {
    val calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
    return android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
}

/** Stops a snoozed alarm, or one about to ring (its notification is up), before it goes off. */
@Composable
private fun PredismissButton(alarm: Alarm, version: Int, updater: AlarmUpdateHandler) {
    val context = LocalContext.current
    var instance by remember { mutableStateOf<AlarmInstance?>(null) }
    LaunchedEffect(alarm.id, alarm.instanceId, alarm.instanceState, version) {
        instance =
            if (alarm.canPreemptivelyDismiss()) {
                withContext(Dispatchers.IO) {
                    AlarmInstance.getInstance(context.contentResolver, alarm.instanceId.toLong())
                }
            } else {
                null
            }
    }
    val shown = instance ?: return
    PvotButton(
        text =
            if (alarm.instanceState == AlarmInstance.SNOOZE_STATE) {
                stringResource(
                    R.string.alarm_alert_snooze_until,
                    AlarmUtils.getAlarmText(context, shown, false),
                )
            } else {
                stringResource(R.string.alarm_alert_dismiss_text)
            },
        onClick = {
            context.startService(
                AlarmStateManager.createStateChangeIntent(
                    context,
                    AlarmStateManager.ALARM_DISMISS_TAG,
                    shown,
                    AlarmInstance.PREDISMISSED_STATE,
                )
            )
            updater.showPredismissToast(shown)
            instance = null
        },
        style = PvotButtonStyle.Tonal,
        modifier = Modifier.padding(top = 8.dp),
    )
}
