/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.clock.data.DataModel
import org.prauga.clock.settings.SettingsActivity
import org.prauga.clock.settings.data.SettingsRepository

class SettingsViewModel(private val repository: SettingsRepository, private val model: DataModel) :
    ViewModel() {

    private val refreshes = MutableStateFlow(0)
    private var preview: Job? = null

    val state: StateFlow<UiState<SettingsContent>> =
        combine(repository.preferences, repository.alarmVolume, refreshes) { preferences, volume, _
                ->
                val zones = model.timeZones
                UiState.Success(
                    SettingsContent(
                        preferences = preferences,
                        timerRingtoneTitle = model.timerRingtoneTitle,
                        hasMotionSensor = repository.hasMotionSensor,
                        hasVibrator = repository.hasVibrator,
                        zoneNames = zones.timeZoneNames.map { it.toString() },
                        zoneIds = zones.timeZoneIds.map { it.toString() },
                        firstDayOfWeek = model.weekdayOrder.calendarDays[0].toString(),
                        alarmVolume = volume,
                    )
                ) as UiState<SettingsContent>
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Refresh -> refreshes.value++
            is SettingsEvent.SetString -> repository.putString(event.key, event.value)
            is SettingsEvent.SetBoolean -> setBoolean(event.key, event.value)
            is SettingsEvent.SetAlarmVolume -> repository.setAlarmVolume(event.volume)
            SettingsEvent.PreviewAlarmVolume -> previewAlarm()
            SettingsEvent.TurnOffAllAlarms -> repository.turnOffAllAlarms()
        }
    }

    override fun onCleared() {
        repository.stopAlarmPreview()
    }

    private fun setBoolean(key: String, value: Boolean) {
        when (key) {
            SettingsActivity.KEY_CLOCK_DISPLAY_SECONDS -> model.displayClockSeconds = value
            SettingsActivity.KEY_TIMER_VIBRATE -> model.setTimerVibrate(value)
            else -> repository.putBoolean(key, value)
        }
    }

    private fun previewAlarm() {
        if (preview?.isActive == true) return
        preview =
            viewModelScope.launch {
                repository.startAlarmPreview(model.defaultAlarmRingtoneUri)
                try {
                    delay(PREVIEW_MILLIS)
                } finally {
                    repository.stopAlarmPreview()
                }
            }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        const val PREVIEW_MILLIS = 2_000L
    }
}
