/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import org.prauga.clock.bedtime.BedtimeScheduler
import org.prauga.clock.data.DataModel
import org.prauga.clock.stopwatch.StopwatchService
import org.prauga.clock.uidata.UiDataModel

/**
 * Deep links for automation, laid out like Google Clock's `clock-app://` links:
 * - `alarm/create?hour=7&minutes=30&label=Gym`, `alarm/show`
 * - `timer/create?length=300&label=Tea`, `timer/show`
 * - `stopwatch/start|pause|reset|lap|show`
 * - `clock/view`, `bedtime/view`
 *
 * Any app or web page can open a link, so creating alarms and timers always shows the UI rather
 * than acting silently the way the SET_ALARM permission allows.
 */
class HandleUris : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.data?.let { handle(it) }
        finish()
    }

    private fun handle(uri: Uri) {
        val segments = uri.pathSegments
        when (segments.getOrNull(0) to segments.getOrNull(1)) {
            "alarm" to "create" ->
                api(AlarmClock.ACTION_SET_ALARM) {
                    uri.int("hour")?.let { putExtra(AlarmClock.EXTRA_HOUR, it) }
                    uri.int("minutes")?.let { putExtra(AlarmClock.EXTRA_MINUTES, it) }
                    uri.getQueryParameter("label")?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
                }
            "alarm" to "show" -> api(AlarmClock.ACTION_SHOW_ALARMS)
            "timer" to "create" ->
                api(AlarmClock.ACTION_SET_TIMER) {
                    uri.int("length")?.let { putExtra(AlarmClock.EXTRA_LENGTH, it) }
                    uri.getQueryParameter("label")?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) }
                }
            "timer" to "show" -> api(AlarmClock.ACTION_SHOW_TIMERS)
            "stopwatch" to "start" -> stopwatch { startStopwatch() }
            "stopwatch" to "pause" -> stopwatch { pauseStopwatch() }
            "stopwatch" to "reset" -> stopwatch { resetStopwatch() }
            "stopwatch" to "lap" ->
                stopwatch { if (stopwatch.isRunning && canAddMoreLaps()) addLap() }
            "stopwatch" to "show" -> stopwatch {}
            "clock" to "view" -> show(UiDataModel.Tab.CLOCKS, null)
            "bedtime" to "view" ->
                show(UiDataModel.Tab.BEDTIME, BedtimeScheduler.ACTION_SHOW_BEDTIME)
        }
    }

    private fun api(action: String, extras: Intent.() -> Unit = {}) {
        startActivity(Intent(action).setClass(this, HandleApiCalls::class.java).apply(extras))
    }

    private fun stopwatch(change: DataModel.() -> Unit) {
        DataModel.getDataModel().change()
        show(UiDataModel.Tab.STOPWATCH, StopwatchService.ACTION_SHOW_STOPWATCH)
    }

    private fun show(tab: UiDataModel.Tab, action: String?) {
        UiDataModel.getUiDataModel().setSelectedTab(tab)
        startActivity(Intent(this, DeskClock::class.java).setAction(action))
    }

    private fun Uri.int(name: String) = getQueryParameter(name)?.toIntOrNull()
}
