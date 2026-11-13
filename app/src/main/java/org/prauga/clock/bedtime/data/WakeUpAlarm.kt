/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.data

data class WakeUpAlarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val days: Int,
)
