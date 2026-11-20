/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotSheetItem
import com.prauga.pvot.designsystem.components.picker.PvotDurationPicker
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.effects.pvotPressScale
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlin.time.Duration.Companion.milliseconds
import org.prauga.clock.R
import org.prauga.clock.timer.data.TimerPreset

val PresetChipSize: Dp = 96.dp

private const val PRESET_NAME_MAX_LENGTH = 40

@Composable
fun presetTitle(preset: TimerPreset): String =
    preset.name.ifEmpty { shortLength(preset.lengthMillis) }

@Composable
private fun shortLength(lengthMillis: Long): String {
    val totalSeconds = lengthMillis / DateUtils.SECOND_IN_MILLIS
    val minutes = totalSeconds / 60
    return when {
        totalSeconds % 60 != 0L -> clockLength(lengthMillis)
        minutes < 60 -> stringResource(R.string.timer_preset_minutes, minutes.toInt())
        else ->
            stringResource(
                R.string.timer_preset_hours_minutes,
                (minutes / 60).toInt(),
                (minutes % 60).toInt(),
            )
    }
}

private fun clockLength(lengthMillis: Long): String {
    val total = lengthMillis / DateUtils.SECOND_IN_MILLIS
    return "%02d:%02d:%02d".format(total / 3600, total / 60 % 60, total % 60)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PresetChip(
    preset: TimerPreset,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    PresetCircle(
        modifier =
            modifier
                .pvotPressScale(interactionSource)
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
    ) {
        Text(
            presetTitle(preset),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (preset.name.isNotEmpty()) {
            Text(
                clockLength(preset.lengthMillis),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AddPresetChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    PresetCircle(
        modifier =
            modifier
                .pvotPressScale(interactionSource)
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
    ) {
        Icon(
            painterResource(R.drawable.ic_t_plus),
            contentDescription = stringResource(R.string.timer_preset_add),
        )
    }
}

@Composable
private fun PresetCircle(modifier: Modifier, content: @Composable () -> Unit) {
    PvotCard(modifier = modifier.fillMaxWidth().aspectRatio(1f), shape = CircleShape) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(PvotSpacing.S), Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                content()
            }
        }
    }
}

@Composable
fun PresetActionsSheet(
    preset: TimerPreset?,
    onDismiss: () -> Unit,
    onEdit: (TimerPreset) -> Unit,
    onDelete: (TimerPreset) -> Unit,
) {
    PvotBottomSheet(
        visible = preset != null,
        onDismissRequest = onDismiss,
        title = preset?.let { presetTitle(it) },
    ) {
        if (preset != null) {
            PvotSheetItem(
                title = stringResource(R.string.timer_preset_edit),
                icon = { Icon(painterResource(R.drawable.ic_t_pencil), contentDescription = null) },
                onClick = { onEdit(preset) },
            )
            PvotSheetItem(
                title = stringResource(R.string.timer_preset_delete),
                icon = { Icon(painterResource(R.drawable.ic_t_trash), contentDescription = null) },
                destructive = true,
                onClick = { onDelete(preset) },
            )
        }
    }
}

@Composable
fun PresetEditorDialog(
    preset: TimerPreset?,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, lengthMillis: Long) -> Unit,
) {
    var name by rememberSaveable(preset, visible) { mutableStateOf(preset?.name.orEmpty()) }
    var lengthMillis by
        rememberSaveable(preset, visible) { mutableLongStateOf(preset?.lengthMillis ?: 0L) }
    PvotDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title =
            stringResource(
                if (preset == null) R.string.timer_preset_new else R.string.timer_preset_edit_title
            ),
        content = {
            PvotTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.timer_preset_name),
                singleLine = true,
                maxLength = PRESET_NAME_MAX_LENGTH,
                modifier = Modifier.fillMaxWidth(),
            )
            PvotDurationPicker(
                duration = lengthMillis.milliseconds,
                onDurationChange = { lengthMillis = it.inWholeMilliseconds },
                modifier = Modifier.fillMaxWidth().padding(top = PvotSpacing.M),
            )
        },
    ) {
        PvotDialogAction(
            label = stringResource(R.string.timer_preset_save),
            style = PvotDialogActionStyle.Primary,
            enabled = lengthMillis > 0,
            onClick = { onSave(name.trim(), lengthMillis) },
        )
        PvotDialogAction(label = stringResource(android.R.string.cancel), onClick = onDismiss)
    }
}
