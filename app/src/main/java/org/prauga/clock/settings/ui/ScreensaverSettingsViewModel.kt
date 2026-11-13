/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.clock.data.DataModel
import org.prauga.clock.settings.ScreensaverSettingsActivity
import org.prauga.clock.settings.data.SettingsRepository

class ScreensaverSettingsViewModel(
    private val repository: SettingsRepository,
    private val model: DataModel,
) : ViewModel() {

    val state: StateFlow<UiState<ScreensaverContent>> =
        repository.preferences
            .map<_, UiState<ScreensaverContent>> { UiState.Success(content()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    fun onEvent(event: ScreensaverEvent) {
        when (event) {
            is ScreensaverEvent.SetString -> repository.putString(event.key, event.value)
            is ScreensaverEvent.SetBoolean -> repository.putBoolean(event.key, event.value)
            is ScreensaverEvent.SetBrightness ->
                repository.putInt(
                    ScreensaverSettingsActivity.KEY_NIGHT_MODE_BRIGHTNESS,
                    event.percent,
                )
        }
    }

    private fun content() =
        ScreensaverContent(
            style = model.screensaverClockStyle.toString().lowercase(Locale.US),
            digital = model.screensaverClockStyle == DataModel.ClockStyle.DIGITAL,
            color = model.screensaverClockColor,
            boldText = model.screensaverBoldTextOn,
            showAmPm = model.screensaverShowAmPmOn,
            nightMode = model.screensaverNightModeOn,
            nightModeDnd = model.screensaverNightModeDndOn,
            nightModeColor = model.screensaverClockNightModeColor,
            nightModeBrightness = model.screensaverNightModeBrightness,
        )

    private companion object {
        const val STOP_TIMEOUT = 5_000L
    }
}
