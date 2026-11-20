/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.StopwatchListener
import org.prauga.clock.stopwatch.data.StopwatchSession
import org.prauga.clock.stopwatch.data.StopwatchSessionsRepository

class StopwatchViewModel(
    private val model: DataModel,
    private val sessions: StopwatchSessionsRepository,
) : ViewModel() {

    private val content = MutableStateFlow(snapshot())

    val state: StateFlow<UiState<StopwatchContent>> =
        combine(content, sessions.sessions.onStart { emit(emptyList()) }) { content, saved ->
                UiState.Success(content.copy(sessions = saved))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                UiState.Success(content.value),
            )

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
            StopwatchEvent.Save -> save()
            is StopwatchEvent.DeleteSession ->
                viewModelScope.launch { sessions.delete(event.savedAt) }
        }
    }

    override fun onCleared() {
        model.removeStopwatchListener(listener)
    }

    private fun save() {
        val stopwatch = model.stopwatch
        if (stopwatch.isReset) return
        val session =
            StopwatchSession(
                savedAt = System.currentTimeMillis(),
                total = stopwatch.totalTime,
                laps = model.laps.reversed().map { it.lapTime },
            )
        model.resetStopwatch()
        viewModelScope.launch { sessions.save(session) }
    }

    private fun refresh() {
        content.value = snapshot()
    }

    private fun snapshot() =
        StopwatchContent(
            stopwatch = model.stopwatch,
            laps = model.laps.toList(),
            canAddLap = model.canAddMoreLaps(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
