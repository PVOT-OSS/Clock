/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

sealed interface ScreensaverEvent {
    data class SetString(val key: String, val value: String) : ScreensaverEvent

    data class SetBoolean(val key: String, val value: Boolean) : ScreensaverEvent

    data class SetBrightness(val percent: Int) : ScreensaverEvent
}
