/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.data

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View

/** Device states, beyond silence, that keep alarms from ringing the way they should. */
object AlarmReliability {

    /** Whether alarms may launch their full-screen ringing activity (revocable from API 34). */
    @JvmStatic
    fun canUseFullScreenIntent(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    @JvmField
    val openFullScreenIntentSettings =
        View.OnClickListener { v ->
            openAppSetting(v.context, Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
        }

    private fun openAppSetting(context: Context, action: String) {
        val intent =
            Intent(action)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(intent.data)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
