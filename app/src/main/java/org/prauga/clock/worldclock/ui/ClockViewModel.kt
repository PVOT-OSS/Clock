/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import androidx.lifecycle.ViewModel
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.prauga.clock.data.CityListener
import org.prauga.clock.data.DataModel

class ClockViewModel(private val model: DataModel) : ViewModel() {

    private val content = MutableStateFlow<UiState<ClockContent>>(UiState.Success(snapshot()))
    val state: StateFlow<UiState<ClockContent>> = content.asStateFlow()

    private val listener = CityListener { refresh() }

    init {
        model.addCityListener(listener)
    }

    fun onEvent(event: ClockEvent) {
        when (event) {
            ClockEvent.Refresh -> refresh()
            is ClockEvent.Move -> model.moveSelectedCity(event.city, event.delta)
            is ClockEvent.Remove -> model.removeSelectedCity(event.city)
        }
    }

    override fun onCleared() {
        model.removeCityListener(listener)
    }

    private fun refresh() {
        content.value = UiState.Success(snapshot())
    }

    private fun snapshot(): ClockContent {
        val selected = model.selectedCities
        val home = model.homeCity.takeIf { model.showHomeClock }
        return ClockContent(
            seconds = model.displayClockSeconds,
            analog = model.clockStyle == DataModel.ClockStyle.ANALOG,
            clocks = listOfNotNull(home) + selected,
            selected = selected,
        )
    }
}
