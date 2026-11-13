/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarmclock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import org.prauga.clock.DeskClock
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.StopwatchListener
import org.prauga.clock.stopwatch.StopwatchService

class StopwatchAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val model = DataModel.getDataModel()
        when (intent.action) {
            ACTION_TOGGLE ->
                if (model.stopwatch.isRunning) model.pauseStopwatch() else model.startStopwatch()
            ACTION_LAP -> if (model.stopwatch.isRunning && model.canAddMoreLaps()) model.addLap()
            ACTION_RESET -> model.resetStopwatch()
            else -> return super.onReceive(context, intent)
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "org.prauga.clock.widget.STOPWATCH_TOGGLE"
        private const val ACTION_LAP = "org.prauga.clock.widget.STOPWATCH_LAP"
        private const val ACTION_RESET = "org.prauga.clock.widget.STOPWATCH_RESET"

        @JvmStatic
        fun observe(context: Context) {
            val app = context.applicationContext
            DataModel.getDataModel()
                .addStopwatchListener(StopwatchListener { _, _ -> updateAll(app) })
        }

        private fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids =
                manager.getAppWidgetIds(
                    ComponentName(context, StopwatchAppWidgetProvider::class.java)
                )
            if (ids.isNotEmpty()) {
                update(context, manager, ids)
            }
        }

        private fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val model = DataModel.getDataModel()
            val stopwatch = model.stopwatch
            val views = RemoteViews(context.packageName, R.layout.stopwatch_widget)
            views.setChronometer(
                R.id.stopwatch_widget_time,
                SystemClock.elapsedRealtime() - stopwatch.totalTime,
                null,
                stopwatch.isRunning,
            )
            val laps = model.laps.size
            views.setViewVisibility(
                R.id.stopwatch_widget_laps,
                if (laps > 0) View.VISIBLE else View.INVISIBLE,
            )
            if (laps > 0) {
                views.setTextViewText(
                    R.id.stopwatch_widget_laps,
                    context.getString(R.string.sw_notification_lap_number, laps + 1),
                )
            }
            views.setImageViewResource(
                R.id.stopwatch_widget_toggle,
                if (stopwatch.isRunning) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            views.setContentDescription(
                R.id.stopwatch_widget_toggle,
                context.getString(
                    if (stopwatch.isRunning) R.string.sw_pause_button else R.string.sw_start_button
                ),
            )
            views.setViewVisibility(
                R.id.stopwatch_widget_lap,
                if (stopwatch.isRunning) View.VISIBLE else View.GONE,
            )
            views.setViewVisibility(
                R.id.stopwatch_widget_reset,
                if (stopwatch.isReset) View.GONE else View.VISIBLE,
            )
            views.setOnClickPendingIntent(
                R.id.stopwatch_widget_toggle,
                action(context, ACTION_TOGGLE),
            )
            views.setOnClickPendingIntent(R.id.stopwatch_widget_lap, action(context, ACTION_LAP))
            views.setOnClickPendingIntent(
                R.id.stopwatch_widget_reset,
                action(context, ACTION_RESET),
            )
            views.setOnClickPendingIntent(R.id.stopwatch_widget_time, openStopwatch(context))
            manager.updateAppWidget(ids, views)
        }

        private fun action(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, StopwatchAppWidgetProvider::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun openStopwatch(context: Context): PendingIntent {
            val intent =
                Intent(context, DeskClock::class.java)
                    .setAction(StopwatchService.ACTION_SHOW_STOPWATCH)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
