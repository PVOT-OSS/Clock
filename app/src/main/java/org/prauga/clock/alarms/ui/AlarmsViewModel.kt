/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.data.DataModel
import org.prauga.clock.provider.Alarm

class AlarmsViewModel(private val repository: AlarmRepository, private val model: DataModel) :
    ViewModel() {

    val state: StateFlow<UiState<List<AlarmItem>>> =
        repository.alarms
            .map<_, UiState<List<AlarmItem>>> { records ->
                UiState.Success(
                    records.map { AlarmItem(it, model.getRingtoneTitle(it.alarm.alert)) }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    private var seenCreateRequests = 0
    private var seenShownAlarm = Alarm.INVALID_ID

    fun onEvent(event: AlarmEvent) {
        when (event) {
            is AlarmEvent.SetEnabled ->
                viewModelScope.launch {
                    repository.update(event.id, popToast = event.enabled, minorUpdate = false) {
                        it.enabled = event.enabled
                    }
                }
            is AlarmEvent.Predismiss -> {
                val item =
                    (state.value as? UiState.Success)?.data?.firstOrNull { it.id == event.id }
                item?.instance?.let(repository::predismiss)
            }
        }
    }

    fun isNewCreateRequest(requests: Int): Boolean {
        val fresh = requests > 0 && requests != seenCreateRequests
        seenCreateRequests = requests
        return fresh
    }

    fun isNewShownAlarm(alarmId: Long): Boolean {
        val fresh = alarmId != Alarm.INVALID_ID && alarmId != seenShownAlarm
        seenShownAlarm = alarmId
        return fresh
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
