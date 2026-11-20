/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotSwitch
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.util.Calendar
import org.prauga.clock.AlarmUtils
import org.prauga.clock.R
import org.prauga.clock.Utils
import org.prauga.clock.alarms.AlarmDatePicker
import org.prauga.clock.common.formatTime
import org.prauga.clock.data.DataModel
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmInstance

@Composable
fun AlarmCard(
    item: AlarmItem,
    onClick: () -> Unit,
    onEvent: (AlarmEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val alarm = item.alarm
    PvotCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(PvotSpacing.L)) {
            if (alarm.label.isNotEmpty()) {
                Text(text = alarm.label, style = MaterialTheme.typography.titleSmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTime(context, alarm.hour, alarm.minutes),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = if (alarm.enabled) FontWeight.Normal else FontWeight.Light,
                    color =
                        if (alarm.enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                PvotSwitch(
                    checked = alarm.enabled,
                    onCheckedChange = { onEvent(AlarmEvent.SetEnabled(item.id, it)) },
                )
            }
            Text(text = summary(context, alarm), style = MaterialTheme.typography.bodyMedium)
            PredismissButton(item) { onEvent(AlarmEvent.Predismiss(item.id)) }
        }
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

@Composable
private fun PredismissButton(item: AlarmItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val instance = item.instance ?: return
    PvotButton(
        text =
            if (item.instanceState == AlarmInstance.SNOOZE_STATE) {
                stringResource(
                    R.string.alarm_alert_snooze_until,
                    AlarmUtils.getAlarmText(context, instance, false),
                )
            } else {
                stringResource(R.string.alarm_alert_dismiss_text)
            },
        onClick = onDismiss,
        style = PvotButtonStyle.Tonal,
        modifier = Modifier.padding(top = PvotSpacing.S),
    )
}
