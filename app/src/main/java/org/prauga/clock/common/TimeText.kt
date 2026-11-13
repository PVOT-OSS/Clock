/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar

fun formatTime(context: Context, hour: Int, minute: Int): String {
    val calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}
