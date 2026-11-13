/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

data class ScreensaverContent(
    val style: String,
    val digital: Boolean,
    val color: String,
    val boldText: Boolean,
    val showAmPm: Boolean,
    val nightMode: Boolean,
    val nightModeDnd: Boolean,
    val nightModeColor: String,
    val nightModeBrightness: Int,
)
