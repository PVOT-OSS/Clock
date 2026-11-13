/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import androidx.lifecycle.ViewModel
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Timer
import org.prauga.clock.data.TimerListener

class TimerViewModel(private val model: DataModel, private val expiredOnly: Boolean = false) :
    ViewModel() {

    private val timers = MutableStateFlow<UiState<List<TimerItem>>>(UiState.Success(snapshot()))
    val state: StateFlow<UiState<List<TimerItem>>> = timers.asStateFlow()

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
            is TimerEvent.Create -> model.startTimer(model.addTimer(event.lengthMillis, "", false))
            is TimerEvent.Start -> find(event.id)?.let { model.startTimer(it) }
            is TimerEvent.Pause -> find(event.id)?.let { model.pauseTimer(it) }
            is TimerEvent.Reset -> find(event.id)?.let { model.resetTimer(it) }
            is TimerEvent.Stop ->
                find(event.id)?.let { model.resetOrDeleteTimer(it, R.string.label_deskclock) }
            is TimerEvent.AddMinute -> find(event.id)?.let { model.addTimerMinute(it) }
            is TimerEvent.Delete -> find(event.id)?.let { model.removeTimer(it) }
        }
    }

    override fun onCleared() {
        model.removeTimerListener(listener)
    }

    private fun find(id: Int): Timer? = model.getTimer(id)

    private fun refresh() {
        timers.value = UiState.Success(snapshot())
    }

    private fun snapshot(): List<TimerItem> =
        (if (expiredOnly) model.expiredTimers else model.timers).map(::TimerItem)
}
