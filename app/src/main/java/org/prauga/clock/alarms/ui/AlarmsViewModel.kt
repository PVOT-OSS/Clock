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
import org.prauga.clock.provider.AlarmDates

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

    fun onEvent(event: AlarmEvent) {
        when (event) {
            is AlarmEvent.Add ->
                repository.add(Alarm(event.hour, event.minute).apply { enabled = true })
            is AlarmEvent.SetTime ->
                update(event.id, popToast = true) {
                    it.hour = event.hour
                    it.minutes = event.minute
                    it.enabled = true
                }
            is AlarmEvent.SetEnabled ->
                update(event.id, popToast = event.enabled) { it.enabled = event.enabled }
            is AlarmEvent.SetDay ->
                update(event.id, popToast = true) {
                    it.daysOfWeek = it.daysOfWeek.setBit(event.calendarDay, event.on)
                    if (it.daysOfWeek.isRepeating) {
                        it.scheduledDate = AlarmDates.NONE
                    } else {
                        it.pauseStart = AlarmDates.NONE
                        it.pauseEnd = AlarmDates.NONE
                    }
                }
            is AlarmEvent.SetVibrate ->
                update(event.id, minorUpdate = true) { it.vibrate = event.vibrate }
            is AlarmEvent.SetLabel ->
                update(event.id, minorUpdate = true) { it.label = event.label }
            is AlarmEvent.SetDate ->
                update(event.id, popToast = null) {
                    it.scheduledDate = event.scheduledDate
                    if (event.scheduledDate != AlarmDates.NONE) it.enabled = true
                }
            is AlarmEvent.SetPause ->
                update(event.id, popToast = null) {
                    it.pauseStart = event.start
                    it.pauseEnd = event.end
                    if (event.start != AlarmDates.NONE) it.enabled = true
                }
            is AlarmEvent.Delete -> viewModelScope.launch { repository.delete(event.id) }
            is AlarmEvent.Predismiss -> {
                val item =
                    (state.value as? UiState.Success)?.data?.firstOrNull { it.id == event.id }
                item?.instance?.let(repository::predismiss)
            }
        }
    }

    private fun update(
        id: Long,
        popToast: Boolean? = false,
        minorUpdate: Boolean = false,
        change: (Alarm) -> Unit,
    ) {
        viewModelScope.launch { repository.update(id, popToast, minorUpdate, change) }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
