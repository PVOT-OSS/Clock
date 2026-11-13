/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui.alarms

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.picker.PvotClockPicker
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import java.time.LocalTime
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.prauga.clock.R
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmDates

private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)

@Composable
fun TimeDialog(
    visible: Boolean,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var time by remember(visible) { mutableStateOf(initial) }
    PvotDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = null,
        content = { PvotClockPicker(time = time, onTimeChange = { time = it }) },
        actions = {
            PvotDialogAction(
                label = stringResource(android.R.string.ok),
                onClick = { onConfirm(time) },
                style = PvotDialogActionStyle.Primary,
            )
            PvotDialogAction(label = stringResource(android.R.string.cancel), onClick = onDismiss)
        },
    )
}

@Composable
fun LabelDialog(
    visible: Boolean,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var label by remember(visible) { mutableStateOf(initial) }
    PvotDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.label),
        content = { PvotTextField(value = label, onValueChange = { label = it }) },
        actions = {
            PvotDialogAction(
                label = stringResource(android.R.string.ok),
                onClick = { onConfirm(label.trim()) },
                style = PvotDialogActionStyle.Primary,
            )
            PvotDialogAction(label = stringResource(android.R.string.cancel), onClick = onDismiss)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDateDialog(alarm: Alarm, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val today = AlarmDates.epochDay(Calendar.getInstance())
    val future =
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MILLIS >= today
        }
    if (alarm.daysOfWeek.isRepeating) {
        val paused = alarm.pauseStart != AlarmDates.NONE
        val state =
            rememberDateRangePickerState(
                initialSelectedStartDateMillis =
                    if (paused) alarm.pauseStart * DAY_MILLIS else null,
                initialSelectedEndDateMillis = if (paused) alarm.pauseEnd * DAY_MILLIS else null,
                selectableDates = future,
            )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = state.selectedEndDateMillis != null,
                    onClick = {
                        alarm.pauseStart = state.selectedStartDateMillis!! / DAY_MILLIS
                        alarm.pauseEnd = state.selectedEndDateMillis!! / DAY_MILLIS
                        alarm.enabled = true
                        onChanged()
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (paused) {
                            alarm.pauseStart = AlarmDates.NONE
                            alarm.pauseEnd = AlarmDates.NONE
                            onChanged()
                        } else onDismiss()
                    }
                ) {
                    Text(
                        stringResource(
                            if (paused) R.string.alarm_pause_resume else android.R.string.cancel
                        )
                    )
                }
            },
        ) {
            DateRangePicker(
                state = state,
                title = { Text(stringResource(R.string.alarm_pause_title)) },
            )
        }
    } else {
        val dated = alarm.scheduledDate != AlarmDates.NONE
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    (if (dated) maxOf(alarm.scheduledDate, today) else today) * DAY_MILLIS,
                selectableDates = future,
            )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        alarm.scheduledDate = state.selectedDateMillis!! / DAY_MILLIS
                        alarm.enabled = true
                        onChanged()
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (dated) {
                            alarm.scheduledDate = AlarmDates.NONE
                            onChanged()
                        } else onDismiss()
                    }
                ) {
                    Text(
                        stringResource(
                            if (dated) R.string.alarm_date_remove else android.R.string.cancel
                        )
                    )
                }
            },
        ) {
            DatePicker(
                state = state,
                title = { Text(stringResource(R.string.alarm_schedule_date_title)) },
            )
        }
    }
}
