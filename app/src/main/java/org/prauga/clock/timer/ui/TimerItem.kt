/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import android.os.SystemClock
import org.prauga.clock.data.Timer

data class TimerItem(
    val id: Int,
    val state: Timer.State,
    val remaining: Long,
    val label: String,
    val length: Long,
    val snapshotAt: Long,
) {
    val isTicking: Boolean
        get() = state == Timer.State.RUNNING || state == Timer.State.EXPIRED

    fun remainingAt(elapsedRealtime: Long): Long =
        if (isTicking) remaining - (elapsedRealtime - snapshotAt) else remaining

    constructor(
        timer: Timer
    ) : this(
        timer.id,
        timer.state,
        timer.remainingTime,
        timer.label,
        timer.totalLength,
        SystemClock.elapsedRealtime(),
    )
}
