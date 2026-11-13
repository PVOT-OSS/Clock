/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.prauga.clock.bedtime.BedtimeSettings
import org.prauga.clock.bedtime.data.BedtimeRepository
import org.prauga.clock.bedtime.data.WakeUpAlarm

class BedtimeViewModel(private val repository: BedtimeRepository) : ViewModel() {

    private val content = MutableStateFlow<UiState<BedtimeContent>>(UiState.Loading)
    val state: StateFlow<UiState<BedtimeContent>> = content.asStateFlow()

    private var wakeUpChoices: List<WakeUpAlarm>? = null
    private var tomorrow: String? = null

    init {
        refresh()
    }

    fun onEvent(event: BedtimeEvent) {
        when (event) {
            BedtimeEvent.Refresh -> refresh()
            BedtimeEvent.FinishIntro -> change { introDone = true }
            is BedtimeEvent.SetEnabled -> change { enabled = event.enabled }
            is BedtimeEvent.SetDay -> change { days = days.setBit(event.calendarDay, event.on) }
            is BedtimeEvent.SetTime ->
                change {
                    hour = event.hour
                    minute = event.minute
                    enabled = true
                }
            is BedtimeEvent.SetReminder -> change { reminderMinutes = event.minutes }
            is BedtimeEvent.SetMode -> {
                if (!event.enabled) repository.removeMode()
                change { modeEnabled = event.enabled }
            }
            BedtimeEvent.LoadWakeUpChoices ->
                viewModelScope.launch {
                    wakeUpChoices = repository.wakeUpChoices()
                    publish()
                }
            is BedtimeEvent.ChooseWakeUp -> change { wakeUpAlarmId = event.id }
            is BedtimeEvent.AddWakeUpAlarm -> addWakeUpAlarm(event.label)
            is BedtimeEvent.SetSleepSound -> change { sleepSound = event.sound }
            is BedtimeEvent.SetSleepMinutes -> change { sleepSoundMinutes = event.minutes }
            BedtimeEvent.ToggleSleepSound -> toggleSleepSound()
            BedtimeEvent.LoadTomorrow ->
                viewModelScope.launch {
                    tomorrow = repository.tomorrowsEvent()
                    publish()
                }
        }
    }

    private fun addWakeUpAlarm(label: String) {
        val bedtime = current()?.bedtime ?: return
        val wake =
            (bedtime.hour * MINUTES_PER_HOUR + bedtime.minute + SLEEP_MINUTES) % MINUTES_PER_DAY
        repository.addWakeUpAlarm(
            wake / MINUTES_PER_HOUR,
            wake % MINUTES_PER_HOUR,
            bedtime.days,
            label,
        ) { id ->
            change { wakeUpAlarmId = id }
        }
    }

    private fun toggleSleepSound() {
        val bedtime = current()?.bedtime ?: return
        if (bedtime.sleepSoundPlaying) {
            repository.stopSleepSound()
        } else {
            repository.startSleepSound(bedtime.sleepSound, bedtime.sleepSoundMinutes)
        }
        refresh()
    }

    private fun change(block: BedtimeSettings.() -> Unit) {
        viewModelScope.launch {
            repository.change(block)
            publish()
        }
    }

    private fun refresh() {
        viewModelScope.launch { publish() }
    }

    private suspend fun publish() {
        content.value = UiState.Success(BedtimeContent(repository.load(), wakeUpChoices, tomorrow))
    }

    private fun current(): BedtimeContent? = (content.value as? UiState.Success)?.data

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
        const val SLEEP_MINUTES = 8 * MINUTES_PER_HOUR
    }
}
