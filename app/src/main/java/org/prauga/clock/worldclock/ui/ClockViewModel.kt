/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.prauga.clock.data.CityListener
import org.prauga.clock.data.DataModel

class ClockViewModel(
    private val model: DataModel,
    private val nextAlarm: () -> String?,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val content = MutableStateFlow<UiState<ClockContent>>(UiState.Success(snapshot(null)))
    val state: StateFlow<UiState<ClockContent>> = content.asStateFlow()

    private val listener = CityListener { refresh() }

    init {
        model.addCityListener(listener)
    }

    fun onEvent(event: ClockEvent) {
        when (event) {
            ClockEvent.Refresh -> {
                refresh()
                refreshNextAlarm()
            }
            is ClockEvent.Move -> model.moveSelectedCity(event.city, event.delta)
            is ClockEvent.Remove -> model.removeSelectedCity(event.city)
        }
    }

    override fun onCleared() {
        model.removeCityListener(listener)
    }

    private fun refresh() {
        content.update { UiState.Success(snapshot(it.alarm())) }
    }

    private fun refreshNextAlarm() {
        viewModelScope.launch {
            val alarm = withContext(io) { nextAlarm() }
            content.update { state ->
                (state as? UiState.Success)?.let {
                    UiState.Success(it.data.copy(nextAlarm = alarm))
                } ?: state
            }
        }
    }

    private fun UiState<ClockContent>.alarm(): String? = (this as? UiState.Success)?.data?.nextAlarm

    private fun snapshot(alarm: String?): ClockContent {
        val selected = model.selectedCities
        val home = model.homeCity.takeIf { model.showHomeClock }
        return ClockContent(
            seconds = model.displayClockSeconds,
            analog = model.clockStyle == DataModel.ClockStyle.ANALOG,
            clocks = listOfNotNull(home) + selected,
            selected = selected,
            nextAlarm = alarm,
        )
    }
}
