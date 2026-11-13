/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.Context

object AlarmGestures {

    const val KEY_DISMISS_GESTURE = "alarm_dismiss_gesture"
    private const val TAP = "tap"

    @JvmStatic
    fun isTapToDismiss(context: Context): Boolean {
        val storage = context.createDeviceProtectedStorageContext()
        val prefs =
            storage.getSharedPreferences("${storage.packageName}_preferences", Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISMISS_GESTURE, null) == TAP
    }
}
