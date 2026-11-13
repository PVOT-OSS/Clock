/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlarmRingingViewModel : ViewModel() {

    private val outcome = MutableStateFlow<RingingOutcome?>(null)
    val state: StateFlow<RingingOutcome?> = outcome.asStateFlow()

    fun snoozed(minutes: Int) {
        outcome.value = RingingOutcome.Snoozed(minutes)
    }

    fun dismissed() {
        outcome.value = RingingOutcome.Dismissed
    }
}
