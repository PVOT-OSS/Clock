/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object ClockSnackbars {

    class Message(
        val text: String,
        val long: Boolean,
        val actionLabel: String?,
        val onAction: Runnable?,
    )

    private val flow = MutableSharedFlow<Message?>(extraBufferCapacity = 8)

    val messages: SharedFlow<Message?>
        get() = flow

    @JvmStatic
    @JvmOverloads
    fun show(
        text: String,
        long: Boolean = false,
        actionLabel: String? = null,
        onAction: Runnable? = null,
    ) {
        flow.tryEmit(Message(text, long, actionLabel, onAction))
    }

    @JvmStatic
    fun dismiss() {
        flow.tryEmit(null)
    }
}
