/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Snackbars raised anywhere in the app ("Alarm set for…", Undo), shown by the main screen's
 * PvotSnackbarHost. A message while that screen isn't showing is dropped, as a View snackbar
 * without a window was.
 */
object ClockSnackbars {

    /** One snackbar; `null` on the flow dismisses the current one. */
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
