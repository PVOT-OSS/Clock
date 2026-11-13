/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

import org.prauga.clock.settings.data.AlarmVolume

data class SettingsContent(
    val preferences: Map<String, Any?>,
    val timerRingtoneTitle: String,
    val hasMotionSensor: Boolean,
    val hasVibrator: Boolean,
    val zoneNames: List<String>,
    val zoneIds: List<String>,
    val firstDayOfWeek: String,
    val alarmVolume: AlarmVolume,
) {
    fun string(key: String, default: String): String = preferences[key] as? String ?: default

    fun boolean(key: String, default: Boolean): Boolean = preferences[key] as? Boolean ?: default
}
