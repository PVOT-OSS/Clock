/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

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
import java.time.LocalTime

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
