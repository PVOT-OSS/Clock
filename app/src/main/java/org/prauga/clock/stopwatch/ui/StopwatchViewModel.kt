/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import androidx.lifecycle.ViewModel
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.StopwatchListener

class StopwatchViewModel(private val model: DataModel) : ViewModel() {

    private val content = MutableStateFlow<UiState<StopwatchContent>>(UiState.Success(snapshot()))
    val state: StateFlow<UiState<StopwatchContent>> = content.asStateFlow()

    private val listener = StopwatchListener { _, _ -> refresh() }

    init {
        model.addStopwatchListener(listener)
    }

    fun onEvent(event: StopwatchEvent) {
        when (event) {
            StopwatchEvent.Start -> model.startStopwatch()
            StopwatchEvent.Pause -> model.pauseStopwatch()
            StopwatchEvent.Reset -> model.resetStopwatch()
            StopwatchEvent.Lap -> {
                model.addLap()
                refresh()
            }
        }
    }

    override fun onCleared() {
        model.removeStopwatchListener(listener)
    }

    private fun refresh() {
        content.value = UiState.Success(snapshot())
    }

    private fun snapshot() =
        StopwatchContent(model.stopwatch, model.laps.toList(), model.canAddMoreLaps())
}
