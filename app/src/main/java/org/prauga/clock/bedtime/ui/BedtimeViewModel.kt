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
            is BedtimeEvent.SetWakeUpTime -> setWakeUpTime(event.hour, event.minute)
            is BedtimeEvent.AddWakeUpAlarm -> addWakeUpAlarm(event.label)
            is BedtimeEvent.SetSleepSound -> change { sleepSound = event.sound }
            is BedtimeEvent.SetSleepMinutes -> change { sleepSoundMinutes = event.minutes }
            BedtimeEvent.ToggleSleepSound -> toggleSleepSound()
            is BedtimeEvent.SetSleepGoal -> change { sleepGoalMinutes = event.minutes }
            BedtimeEvent.LoadTomorrow ->
                viewModelScope.launch {
                    tomorrow = repository.tomorrowsEvent()
                    publish()
                }
        }
    }

    private fun addWakeUpAlarm(label: String) {
        val bedtime = current()?.bedtime ?: return
        val wake = wrapDay(minuteOfDay(bedtime.hour, bedtime.minute) + bedtime.sleepGoalMinutes)
        repository.addWakeUpAlarm(
            wake / MINUTES_PER_HOUR,
            wake % MINUTES_PER_HOUR,
            bedtime.days,
            label,
        ) { id ->
            change { wakeUpAlarmId = id }
        }
    }

    private fun setWakeUpTime(hour: Int, minute: Int) {
        val shown = current() ?: return
        val wakeUp = shown.bedtime.wakeUp ?: return
        content.value =
            UiState.Success(
                shown.copy(
                    bedtime = shown.bedtime.copy(wakeUp = wakeUp.copy(hour = hour, minute = minute))
                )
            )
        viewModelScope.launch { repository.setWakeUpTime(wakeUp.id, hour, minute) }
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
}
