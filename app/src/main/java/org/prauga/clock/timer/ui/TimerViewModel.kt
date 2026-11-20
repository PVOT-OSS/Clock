/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Timer
import org.prauga.clock.data.TimerListener
import org.prauga.clock.timer.data.TimerPresetsRepository

class TimerViewModel(
    private val model: DataModel,
    private val presets: TimerPresetsRepository,
    private val expiredOnly: Boolean = false,
) : ViewModel() {

    private val timers = MutableStateFlow(snapshot())

    val state: StateFlow<UiState<TimerContent>> =
        combine(timers, if (expiredOnly) flowOf(emptyList()) else presets.presets) { items, saved ->
                UiState.Success(TimerContent(items, saved))
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                UiState.Loading,
            )

    private val listener =
        object : TimerListener {
            override fun timerAdded(timer: Timer) = refresh()

            override fun timerUpdated(before: Timer, after: Timer) = refresh()

            override fun timerRemoved(timer: Timer) = refresh()
        }

    init {
        model.addTimerListener(listener)
    }

    fun onEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.Create ->
                model.startTimer(model.addTimer(event.lengthMillis, event.label, false))
            is TimerEvent.Start -> find(event.id)?.let { model.startTimer(it) }
            is TimerEvent.Pause -> find(event.id)?.let { model.pauseTimer(it) }
            is TimerEvent.Reset -> find(event.id)?.let { model.resetTimer(it) }
            is TimerEvent.Stop ->
                find(event.id)?.let { model.resetOrDeleteTimer(it, R.string.label_deskclock) }
            is TimerEvent.AddMinute -> find(event.id)?.let { model.addTimerMinute(it) }
            is TimerEvent.Delete -> find(event.id)?.let { model.removeTimer(it) }
            is TimerEvent.SavePreset ->
                viewModelScope.launch { presets.save(event.id, event.name, event.lengthMillis) }
            is TimerEvent.DeletePreset -> viewModelScope.launch { presets.delete(event.id) }
        }
    }

    override fun onCleared() {
        model.removeTimerListener(listener)
    }

    private fun find(id: Int): Timer? = model.getTimer(id)

    private fun refresh() {
        timers.value = snapshot()
    }

    private fun snapshot(): List<TimerItem> =
        (if (expiredOnly) model.expiredTimers else model.timers).map(::TimerItem)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
