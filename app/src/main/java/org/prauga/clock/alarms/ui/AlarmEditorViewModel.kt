/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import java.time.LocalTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Weekdays
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmDates

class AlarmEditorViewModel(
    private val repository: AlarmRepository,
    private val model: DataModel,
    alarmId: Long,
) : ViewModel() {

    private val draft = MutableStateFlow<AlarmDraft?>(null)
    private val effects = Channel<AlarmEditorEffect>(Channel.BUFFERED)
    private var busy = false

    val effect: Flow<AlarmEditorEffect> = effects.receiveAsFlow()

    val state: StateFlow<UiState<AlarmEditorState>> =
        combine(draft, repository.alarms) { draft, records ->
                if (draft == null) {
                    UiState.Loading
                } else {
                    val stored = records.firstOrNull { it.alarm.id == draft.id }?.alarm
                    val current = if (stored != null) draft.copy(alert = stored.alert) else draft
                    UiState.Success(
                        AlarmEditorState(
                            current,
                            model.getRingtoneTitle(current.alert),
                            current.toAlarm(),
                        )
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    init {
        if (alarmId == Alarm.INVALID_ID) {
            draft.value = AlarmDraft(Alarm(LocalTime.now().hour, 0))
        } else {
            viewModelScope.launch {
                val alarm = repository.get(alarmId)
                if (alarm == null) {
                    effects.send(AlarmEditorEffect.Close)
                } else {
                    draft.value = AlarmDraft(alarm)
                }
            }
        }
    }

    fun onEvent(event: AlarmEditorEvent) {
        when (event) {
            is AlarmEditorEvent.SetTime ->
                edit { it.copy(hour = event.hour, minute = event.minute) }
            is AlarmEditorEvent.SetLabel -> edit { it.copy(label = event.label) }
            is AlarmEditorEvent.SetDay ->
                edit {
                    val days = Weekdays.fromBits(it.days).setBit(event.calendarDay, event.on)
                    if (days.isRepeating) {
                        it.copy(days = days.bits, scheduledDate = AlarmDates.NONE)
                    } else {
                        it.copy(
                            days = days.bits,
                            pauseStart = AlarmDates.NONE,
                            pauseEnd = AlarmDates.NONE,
                        )
                    }
                }
            is AlarmEditorEvent.SetColor -> edit { it.copy(color = event.color) }
            is AlarmEditorEvent.SetVibrate -> edit { it.copy(vibrate = event.vibrate) }
            is AlarmEditorEvent.SetDate -> edit { it.copy(scheduledDate = event.scheduledDate) }
            is AlarmEditorEvent.SetPause ->
                edit { it.copy(pauseStart = event.start, pauseEnd = event.end) }
            AlarmEditorEvent.PickSound -> pickSound()
            AlarmEditorEvent.Save -> save()
            AlarmEditorEvent.Delete -> delete()
        }
    }

    private fun edit(change: (AlarmDraft) -> AlarmDraft) {
        draft.update { it?.let(change) }
    }

    private fun pickSound() {
        val current = draft.value ?: return
        if (!current.isNew) {
            val alarm = (state.value as? UiState.Success)?.data?.preview ?: current.toAlarm()
            effects.trySend(AlarmEditorEffect.PickSound(alarm))
            return
        }
        launchOnce {
            val id = repository.insert(current.toAlarm())
            draft.update { it?.copy(id = id) }
            effects.send(AlarmEditorEffect.PickSound(current.copy(id = id).toAlarm()))
        }
    }

    private fun save() {
        val current = draft.value ?: return
        launchOnce {
            if (current.isNew) {
                repository.insert(current.toAlarm())
            } else {
                repository.update(
                    current.id,
                    popToast = true,
                    minorUpdate = false,
                    current::applyTo,
                )
            }
            effects.send(AlarmEditorEffect.Close)
        }
    }

    private fun delete() {
        val current = draft.value ?: return
        launchOnce {
            if (!current.isNew) repository.delete(current.id)
            effects.send(AlarmEditorEffect.Close)
        }
    }

    private fun launchOnce(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                busy = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
