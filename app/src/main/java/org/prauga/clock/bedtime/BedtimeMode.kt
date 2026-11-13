/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.util.Log
import org.prauga.clock.DeskClock
import org.prauga.clock.R

object BedtimeMode {

    private val CONDITION_ID: Uri = Uri.parse("condition://org.prauga.clock/bedtime")

    @JvmStatic
    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun setActive(context: Context, active: Boolean) {
        if (!hasAccess(context)) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            val id = ruleId(context, manager) ?: return
            val summary = context.getString(R.string.bedtime_mode_title)
            manager.setAutomaticZenRuleState(
                id,
                Condition(
                    CONDITION_ID,
                    summary,
                    if (active) Condition.STATE_TRUE else Condition.STATE_FALSE,
                ),
            )
        } catch (e: RuntimeException) {
            // Do Not Disturb is a convenience here; never take the alarm process down for it.
            Log.w("BedtimeMode", "Could not set bedtime mode", e)
        }
    }

    fun remove(context: Context) {
        if (!hasAccess(context)) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val settings = BedtimeSettings(context)
        settings.zenRuleId?.let { manager.removeAutomaticZenRule(it) }
        settings.zenRuleId = null
    }

    // getAutomaticZenRules() comes back empty for us on Android 17, so the id is kept instead.
    private fun ruleId(context: Context, manager: NotificationManager): String? {
        val settings = BedtimeSettings(context)
        settings.zenRuleId
            ?.takeIf { manager.getAutomaticZenRule(it) != null }
            ?.let {
                return it
            }
        val name = context.getString(R.string.bedtime_mode_title)
        val settingsActivity = ComponentName(context, DeskClock::class.java)
        val rule =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                AutomaticZenRule.Builder(name, CONDITION_ID)
                    // TYPE_BEDTIME is reserved for the system's Wellbeing app.
                    .setType(AutomaticZenRule.TYPE_SCHEDULE_TIME)
                    .setConfigurationActivity(settingsActivity)
                    .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    .setManualInvocationAllowed(true)
                    .build()
            } else {
                AutomaticZenRule(
                    name,
                    null,
                    settingsActivity,
                    CONDITION_ID,
                    null,
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                    true,
                )
            }
        return manager.addAutomaticZenRule(rule).also { settings.zenRuleId = it }
    }
}
