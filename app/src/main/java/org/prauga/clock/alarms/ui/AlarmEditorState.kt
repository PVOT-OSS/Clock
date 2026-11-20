/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmGroup

data class AlarmEditorState(
    val draft: AlarmDraft,
    val ringtoneTitle: String,
    val preview: Alarm,
    val groups: List<AlarmGroup>,
    val isWakeUp: Boolean,
)
