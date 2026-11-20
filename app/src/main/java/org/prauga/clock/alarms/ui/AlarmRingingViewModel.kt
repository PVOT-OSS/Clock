/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.provider.Alarm

class AlarmRingingViewModel(repository: AlarmRepository, alarmId: Long) : ViewModel() {

    private val outcome = MutableStateFlow<RingingOutcome?>(null)
    val state: StateFlow<RingingOutcome?> = outcome.asStateFlow()

    private val alarmColor = MutableStateFlow(Alarm.NO_COLOR)
    val color: StateFlow<Int> = alarmColor.asStateFlow()

    init {
        if (alarmId != Alarm.INVALID_ID) {
            viewModelScope.launch {
                alarmColor.value = repository.get(alarmId)?.color ?: Alarm.NO_COLOR
            }
        }
    }

    fun snoozed(minutes: Int) {
        outcome.value = RingingOutcome.Snoozed(minutes)
    }

    fun dismissed() {
        outcome.value = RingingOutcome.Dismissed
    }

    companion object {
        @JvmStatic
        fun factory(context: Context, alarmId: Long): ViewModelProvider.Factory {
            val app = context.applicationContext
            return viewModelFactory {
                initializer { AlarmRingingViewModel(AlarmRepository(app), alarmId) }
            }
        }
    }
}
