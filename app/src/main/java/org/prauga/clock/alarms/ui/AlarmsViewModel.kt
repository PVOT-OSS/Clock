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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.provider.Alarm

class AlarmsViewModel(private val repository: AlarmRepository) : ViewModel() {

    val state: StateFlow<UiState<AlarmsContent>> =
        combine(repository.alarms, repository.groups) { records, groups ->
                val content =
                    alarmsContent(records.map(::AlarmItem), groups, repository.wakeUpAlarmId())
                UiState.Success(content) as UiState<AlarmsContent>
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
                val content = (state.value as? UiState.Success)?.data
                content
                    ?.all
                    ?.firstOrNull { it.id == event.id }
                    ?.instance
                    ?.let(repository::predismiss)
            }
            is AlarmEvent.SetGroupEnabled ->
                viewModelScope.launch { repository.setGroupEnabled(event.groupId, event.enabled) }
            is AlarmEvent.SetGroupCollapsed ->
                viewModelScope.launch {
                    repository.setGroupCollapsed(event.groupId, event.collapsed)
                }
            is AlarmEvent.RenameGroup ->
                viewModelScope.launch { repository.renameGroup(event.groupId, event.name) }
            is AlarmEvent.DeleteGroup ->
                viewModelScope.launch { repository.deleteGroup(event.groupId, event.withAlarms) }
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
