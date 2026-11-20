/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.motion.pvotReveal
import java.time.LocalTime
import org.prauga.clock.R
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.common.ScreenAction
import org.prauga.clock.common.TimeDialog
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.DataModel

@Composable
fun AlarmsScreen(
    padding: PaddingValues,
    listState: LazyListState,
    createRequests: Int,
    shownAlarm: Long,
    viewModel: AlarmsViewModel = alarmsViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val alarms = (state as? UiState.Success)?.data
    var expanded by rememberSaveable { mutableLongStateOf(-1L) }
    var editingTime by remember { mutableStateOf<AlarmItem?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var timeDialogOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(createRequests) {
        if (createRequests > 0) {
            adding = true
            timeDialogOpen = true
        }
    }
    LaunchedEffect(shownAlarm, alarms) {
        val index = alarms?.indexOfFirst { it.id == shownAlarm } ?: -1
        if (index >= 0) {
            expanded = shownAlarm
            listState.animateScrollToItem(index)
        }
    }

    ScreenAction(R.drawable.ic_t_plus, R.string.new_alarm) {
        adding = true
        timeDialogOpen = true
    }

    when {
        alarms == null -> Unit
        alarms.isEmpty() ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                PvotEmptyContent(
                    message = stringResource(R.string.no_alarms),
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_t_alarm),
                            contentDescription = null,
                            modifier = Modifier.size(PvotIconButtonDefaults.Size),
                        )
                    },
                )
            }
        else ->
            PvotScreen(state = listState, contentPadding = screenPadding(padding)) {
                itemsIndexed(alarms, key = { _, item -> item.id }) { index, item ->
                    AlarmCard(
                        item = item,
                        expanded = expanded == item.id,
                        onToggleExpanded = { expanded = if (expanded == item.id) -1L else item.id },
                        onEditTime = {
                            editingTime = item
                            adding = false
                            timeDialogOpen = true
                        },
                        onEvent = viewModel::onEvent,
                        modifier = Modifier.pvotReveal(index),
                    )
                }
            }
    }

    val target = editingTime.takeUnless { adding }
    TimeDialog(
        visible = timeDialogOpen,
        initial =
            if (target != null) LocalTime.of(target.hour, target.minutes)
            else LocalTime.of(LocalTime.now().hour, 0),
        onDismiss = { timeDialogOpen = false },
        onConfirm = { time ->
            viewModel.onEvent(
                if (target != null) AlarmEvent.SetTime(target.id, time.hour, time.minute)
                else AlarmEvent.Add(time.hour, time.minute)
            )
            timeDialogOpen = false
        },
    )
}

@Composable
private fun alarmsViewModel(): AlarmsViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel { AlarmsViewModel(AlarmRepository(context), DataModel.getDataModel()) }
}
