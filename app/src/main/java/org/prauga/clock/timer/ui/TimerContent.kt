/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import org.prauga.clock.timer.data.TimerPreset

data class TimerContent(val timers: List<TimerItem>, val presets: List<TimerPreset>)
