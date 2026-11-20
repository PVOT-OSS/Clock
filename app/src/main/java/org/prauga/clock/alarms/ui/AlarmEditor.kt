/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRowDefaults
import com.prauga.pvot.designsystem.components.list.PvotPreferenceRow
import com.prauga.pvot.designsystem.components.list.PvotRowIconStyle
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.components.picker.PvotClockPicker
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.time.LocalTime
import org.prauga.clock.R
import org.prauga.clock.alarms.AlarmDatePicker
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.common.SubScreen
import org.prauga.clock.common.WeekdayButtons
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Weekdays
import org.prauga.clock.provider.Alarm
import org.prauga.clock.ringtone.RingtonePickerActivity

@Composable
fun AlarmEditorScreen(alarmId: Long, onClose: () -> Unit) {
    val viewModel = alarmEditorViewModel(alarmId, key = null)
    val state by viewModel.state.collectAsStateWithLifecycle()
    EditorEffects(viewModel, onClose)
    val content = (state as? UiState.Success)?.data
    SubScreen(title = editorTitle(content), onBack = onClose) {
        if (content != null) alarmEditorItems(content, viewModel::onEvent)
    }
}

@Composable
fun AlarmEditorPane(
    alarmId: Long,
    session: Int,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = alarmEditorViewModel(alarmId, key = "alarm-editor-$session")
    val state by viewModel.state.collectAsStateWithLifecycle()
    EditorEffects(viewModel, onClose)
    val content = (state as? UiState.Success)?.data ?: return
    PvotScreen(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.L),
        horizontalAlignment = Alignment.Start,
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    editorTitle(content),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_t_x),
                    contentDescription = stringResource(android.R.string.cancel),
                    onClick = onClose,
                )
            }
        }
        alarmEditorItems(content, viewModel::onEvent)
    }
}

@Composable
fun AlarmEditorPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.alarm_editor_none_selected),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun editorTitle(content: AlarmEditorState?): String =
    stringResource(
        if (content?.draft?.isNew != false) R.string.new_alarm else R.string.alarm_editor_edit_title
    )

@Composable
private fun EditorEffects(viewModel: AlarmEditorViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AlarmEditorEffect.PickSound ->
                    context.startActivity(
                        RingtonePickerActivity.createAlarmRingtonePickerIntent(
                            context,
                            effect.alarm,
                        )
                    )
                AlarmEditorEffect.Close -> close()
            }
        }
    }
}

private fun LazyListScope.alarmEditorItems(
    content: AlarmEditorState,
    onEvent: (AlarmEditorEvent) -> Unit,
) {
    val draft = content.draft
    item(key = "time") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PvotClockPicker(
                time = LocalTime.of(draft.hour, draft.minute),
                onTimeChange = { onEvent(AlarmEditorEvent.SetTime(it.hour, it.minute)) },
            )
        }
    }
    item(key = "label") { LabelField(draft.label, onEvent) }
    item(key = "repeat") {
        Section(stringResource(R.string.alarm_editor_repeat)) {
            WeekdayButtons(Weekdays.fromBits(draft.days)) { day, on ->
                onEvent(AlarmEditorEvent.SetDay(day, on))
            }
        }
    }
    item(key = "color") {
        Section(stringResource(R.string.alarm_editor_color)) {
            ColorSwatches(draft.color) { onEvent(AlarmEditorEvent.SetColor(it)) }
        }
    }
    item(key = "settings") { SettingsGroup(content, onEvent) }
    item(key = "actions") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S, Alignment.End),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (!draft.isNew) {
                PvotButton(
                    text = stringResource(R.string.delete),
                    onClick = { onEvent(AlarmEditorEvent.Delete) },
                    style = PvotButtonStyle.Text,
                )
            }
            PvotButton(
                text = stringResource(R.string.alarm_editor_save),
                onClick = { onEvent(AlarmEditorEvent.Save) },
            )
        }
    }
}

@Composable
private fun LabelField(initial: String, onEvent: (AlarmEditorEvent) -> Unit) {
    var label by rememberSaveable { mutableStateOf(initial) }
    PvotTextField(
        value = label,
        onValueChange = {
            label = it
            onEvent(AlarmEditorEvent.SetLabel(it))
        },
        label = stringResource(R.string.label),
        placeholder = stringResource(R.string.add_label),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        SectionTitle(title)
        content()
    }
}

@Composable
private fun ColorSwatches(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        modifier =
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = PvotSpacing.M),
    ) {
        Swatch(
            color = MaterialTheme.colorScheme.surface,
            checkColor = MaterialTheme.colorScheme.onSurface,
            label = stringResource(R.string.alarm_color_none),
            selected = AlarmColor.of(selected) == null,
            onClick = { onSelect(Alarm.NO_COLOR) },
        )
        AlarmColor.entries.forEach { color ->
            val accent = color.accent()
            Swatch(
                color = accent.color,
                checkColor = accent.onColor,
                label = stringResource(color.label),
                selected = selected == color.ordinal,
                onClick = { onSelect(color.ordinal) },
            )
        }
    }
}

@Composable
private fun Swatch(
    color: Color,
    checkColor: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier.minimumInteractiveComponentSize()
                .size(PvotListRowDefaults.LeadingSize)
                .clip(CircleShape)
                .background(color)
                .border(Dp.Hairline, MaterialTheme.colorScheme.outline, CircleShape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label },
    ) {
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_t_check),
                contentDescription = null,
                tint = checkColor,
            )
        }
    }
}

@Composable
private fun SettingsGroup(content: AlarmEditorState, onEvent: (AlarmEditorEvent) -> Unit) {
    val context = LocalContext.current
    val draft = content.draft
    var editingDate by rememberSaveable { mutableStateOf(false) }
    PvotListGroup(title = stringResource(R.string.alarm_editor_sound)) {
        item {
            PvotPreferenceRow(
                title = stringResource(R.string.alarm_sound),
                value = content.ringtoneTitle,
                onClick = { onEvent(AlarmEditorEvent.PickSound) },
                icon = { Icon(painterResource(R.drawable.ic_t_bell), contentDescription = null) },
                iconStyle = PvotRowIconStyle.Plain,
            )
        }
        item {
            PvotSwitchRow(
                title = stringResource(R.string.alarm_vibrate),
                checked = draft.vibrate,
                onCheckedChange = { onEvent(AlarmEditorEvent.SetVibrate(it)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_t_device_mobile_vibration),
                        contentDescription = null,
                    )
                },
                iconStyle = PvotRowIconStyle.Plain,
            )
        }
        item {
            PvotPreferenceRow(
                title = AlarmDatePicker.rowText(context, content.preview),
                onClick = { editingDate = true },
                showChevron = false,
                icon = {
                    Icon(painterResource(R.drawable.ic_t_calendar), contentDescription = null)
                },
                iconStyle = PvotRowIconStyle.Plain,
            )
        }
    }
    if (editingDate) {
        AlarmDateDialog(
            alarm = content.preview,
            onDismiss = { editingDate = false },
            onSetDate = {
                onEvent(AlarmEditorEvent.SetDate(it))
                editingDate = false
            },
            onSetPause = { start, end ->
                onEvent(AlarmEditorEvent.SetPause(start, end))
                editingDate = false
            },
        )
    }
}

@Composable
private fun alarmEditorViewModel(alarmId: Long, key: String?): AlarmEditorViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel(key = key) {
        AlarmEditorViewModel(AlarmRepository(context), DataModel.getDataModel(), alarmId)
    }
}
