/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.clock.bedtime.BedtimeScheduler
import org.prauga.clock.data.DataModel
import org.prauga.clock.stopwatch.StopwatchService
import org.prauga.clock.timer.TimerService
import org.prauga.clock.uidata.UiDataModel
import org.prauga.clock.uidata.UiDataModel.Tab

/** The Compose app on PvotLib: five tabs in a floating navigation bar. */
class ClockActivity : PvotBaseActivity() {

    /** Bumped when an intent asks for the timer keypad, so the Timer tab opens it. */
    private val timerSetupRequests = mutableIntStateOf(0)

    /** Bumped when an intent asks to create an alarm, so the Alarms tab opens the time picker. */
    private val createAlarmRequests = mutableIntStateOf(0)

    /** The alarm an intent asks to show, expanded in the Alarms tab; -1 for none. */
    private val shownAlarm = mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            route(intent)
        }
        setPvotContent {
            PvotAppTheme {
                ClockApp(
                    timerSetupRequests = timerSetupRequests.intValue,
                    createAlarmRequests = createAlarmRequests.intValue,
                    shownAlarm = shownAlarm.longValue,
                )
            }
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
        if (intent.getBooleanExtra(EXTRA_TIMER_SETUP, false)) {
            model.setSelectedTab(Tab.TIMERS)
            timerSetupRequests.intValue++
        }
        if (intent.getBooleanExtra(EXTRA_CREATE_ALARM, false)) {
            model.setSelectedTab(Tab.ALARMS)
            createAlarmRequests.intValue++
        }
        val alarmId = intent.getLongExtra(EXTRA_SHOW_ALARM, -1L)
        if (alarmId >= 0) {
            model.setSelectedTab(Tab.ALARMS)
            shownAlarm.longValue = alarmId
        }
    }

    companion object {
        /** Opens the timer keypad. */
        const val EXTRA_TIMER_SETUP = "org.prauga.clock.action.TIMER_SETUP"

        /** Opens the time picker for a new alarm. */
        const val EXTRA_CREATE_ALARM = "deskclock.create.new"

        /** Shows the alarm with this id. */
        const val EXTRA_SHOW_ALARM = "deskclock.scroll.to.alarm"

        @JvmStatic
        fun timerSetupIntent(context: Context): Intent =
            Intent(context, ClockActivity::class.java).putExtra(EXTRA_TIMER_SETUP, true)
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
