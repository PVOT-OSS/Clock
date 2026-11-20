/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotSheetItem
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import org.prauga.clock.R

@Composable
fun GroupActionsSheet(
    visible: Boolean,
    name: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    PvotBottomSheet(visible = visible, onDismissRequest = onDismiss, title = name) {
        PvotSheetItem(
            title = stringResource(R.string.alarm_group_rename),
            onClick = onRename,
            icon = { Icon(painterResource(R.drawable.ic_t_pencil), contentDescription = null) },
        )
        PvotSheetItem(
            title = stringResource(R.string.alarm_group_delete),
            onClick = onDelete,
            icon = { Icon(painterResource(R.drawable.ic_t_trash), contentDescription = null) },
            destructive = true,
        )
    }
}

@Composable
fun GroupNameDialog(
    visible: Boolean,
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable(visible, initial) { mutableStateOf(initial) }
    PvotDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = title,
        content = {
            PvotTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.alarm_group_name),
                singleLine = true,
                maxLength = GROUP_NAME_MAX_LENGTH,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        PvotDialogAction(
            label = stringResource(R.string.alarm_group_save),
            style = PvotDialogActionStyle.Primary,
            enabled = name.isNotBlank(),
            onClick = { onSave(name.trim()) },
        )
        PvotDialogAction(label = stringResource(android.R.string.cancel), onClick = onDismiss)
    }
}

@Composable
fun DeleteGroupDialog(
    visible: Boolean,
    name: String,
    alarmCount: Int,
    onDismiss: () -> Unit,
    onDelete: (withAlarms: Boolean) -> Unit,
) {
    PvotDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.alarm_group_delete_title, name),
        message =
            if (alarmCount > 0) {
                pluralStringResource(R.plurals.alarm_group_delete_message, alarmCount, alarmCount)
            } else {
                null
            },
        destructive = true,
    ) {
        if (alarmCount > 0) {
            PvotDialogAction(
                label = stringResource(R.string.alarm_group_delete_keep),
                style = PvotDialogActionStyle.Primary,
                onClick = { onDelete(false) },
            )
            PvotDialogAction(
                label = stringResource(R.string.alarm_group_delete_all),
                style = PvotDialogActionStyle.Destructive,
                onClick = { onDelete(true) },
            )
        } else {
            PvotDialogAction(
                label = stringResource(R.string.alarm_group_delete),
                style = PvotDialogActionStyle.Destructive,
                onClick = { onDelete(false) },
            )
        }
        PvotDialogAction(label = stringResource(android.R.string.cancel), onClick = onDismiss)
    }
}

private const val GROUP_NAME_MAX_LENGTH = 30
