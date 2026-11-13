/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

sealed interface SettingsEvent {
    data object Refresh : SettingsEvent

    data class SetString(val key: String, val value: String) : SettingsEvent

    data class SetBoolean(val key: String, val value: Boolean) : SettingsEvent

    data class SetAlarmVolume(val volume: Int) : SettingsEvent

    data object PreviewAlarmVolume : SettingsEvent

    data object TurnOffAllAlarms : SettingsEvent
}
