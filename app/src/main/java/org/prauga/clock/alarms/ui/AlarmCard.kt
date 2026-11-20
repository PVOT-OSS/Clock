/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

private val SelectedBorderWidth = 2.dp

@Composable
fun AlarmCard(
    item: AlarmItem,
    large: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onEvent: (AlarmEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val alarm = item.alarm
    val accent = AlarmColor.of(item.color)?.accent()
    PvotCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        border =
            if (selected) BorderStroke(SelectedBorderWidth, MaterialTheme.colorScheme.primary)
            else null,
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            if (accent != null) {
                Box(Modifier.width(PvotSpacing.XS).fillMaxHeight().background(accent.color))
            }
            Column(Modifier.weight(1f).padding(if (large) PvotSpacing.L else PvotSpacing.M)) {
                if (item.label.isNotEmpty()) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = accent?.color ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatTime(context, item.hour, item.minutes),
                    style =
                        if (large) MaterialTheme.typography.displayMedium
                        else MaterialTheme.typography.headlineMedium,
                    fontWeight = if (item.enabled) FontWeight.Normal else FontWeight.Light,
                    color =
                        if (item.enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summary(context, alarm),
                        style =
                            if (large) MaterialTheme.typography.bodyMedium
                            else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = PvotSpacing.S),
                    )
                    PvotSwitch(
                        checked = item.enabled,
                        onCheckedChange = { onEvent(AlarmEvent.SetEnabled(item.id, it)) },
                    )
                }
                PredismissButton(item) { onEvent(AlarmEvent.Predismiss(item.id)) }
            }
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
