/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.Context

/** How a ringing alarm is snoozed or stopped: by swiping the alarm button, or by a tap. */
object AlarmGestures {

    const val KEY_DISMISS_GESTURE = "alarm_dismiss_gesture"
    private const val TAP = "tap"

    /** Whether a single tap on snooze or stop acts at once instead of only showing a hint. */
    @JvmStatic
    fun isTapToDismiss(context: Context): Boolean {
        val storage = context.createDeviceProtectedStorageContext()
        // The default preferences file, as DeskClockApplication opens it.
        val prefs =
            storage.getSharedPreferences("${storage.packageName}_preferences", Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISMISS_GESTURE, null) == TAP
    }
}
