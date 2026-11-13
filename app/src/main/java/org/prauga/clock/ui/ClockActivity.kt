/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.runtime.mutableIntStateOf
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.clock.bedtime.BedtimeScheduler
import org.prauga.clock.data.DataModel
import org.prauga.clock.stopwatch.StopwatchService
import org.prauga.clock.timer.TimerFragment
import org.prauga.clock.timer.TimerService
import org.prauga.clock.uidata.UiDataModel
import org.prauga.clock.uidata.UiDataModel.Tab

/** The Compose app on PvotLib: five tabs in a floating navigation bar. */
class ClockActivity : PvotBaseActivity() {

    /** Bumped when an intent asks for the timer keypad, so the Timer tab opens it. */
    private val timerSetupRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            route(intent)
        }
        setPvotContent {
            PvotAppTheme(dynamicColor = true) { ClockApp(timerSetupRequests.intValue) }
        }
        if (
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        route(intent)
    }

    /** Selects the tab an intent from a notification, shortcut or another app asks for. */
    private fun route(intent: Intent) {
        val model = UiDataModel.getUiDataModel()
        when (intent.action) {
            TimerService.ACTION_SHOW_TIMER -> model.setSelectedTab(Tab.TIMERS)
            StopwatchService.ACTION_SHOW_STOPWATCH -> model.setSelectedTab(Tab.STOPWATCH)
            BedtimeScheduler.ACTION_SHOW_BEDTIME -> model.setSelectedTab(Tab.BEDTIME)
        }
        if (intent.getBooleanExtra(TimerFragment.EXTRA_TIMER_SETUP, false)) {
            model.setSelectedTab(Tab.TIMERS)
            timerSetupRequests.intValue++
        }
    }

    override fun onStart() {
        // Timer and stopwatch notifications hide while the app is open.
        DataModel.getDataModel().setApplicationInForeground(true)
        super.onStart()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            DataModel.getDataModel().setApplicationInForeground(false)
        }
        super.onStop()
    }
}
