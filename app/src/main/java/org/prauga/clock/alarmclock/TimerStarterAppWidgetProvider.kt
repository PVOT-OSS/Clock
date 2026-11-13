/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarmclock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.RemoteViews
import android.widget.Toast
import org.prauga.clock.ClockActivity
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.uidata.UiDataModel

class TimerStarterAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.timer_starter_widget)
        PRESETS.forEachIndexed { i, (viewId, minutes) ->
            val label = context.getString(R.string.timer_preset_minutes, minutes)
            views.setTextViewText(viewId, label)
            views.setContentDescription(
                viewId,
                context.resources.getQuantityString(
                    R.plurals.timer_preset_minutes_description,
                    minutes,
                    minutes,
                ),
            )
            val start =
                Intent(context, TimerStarterAppWidgetProvider::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_MINUTES, minutes)
            views.setOnClickPendingIntent(
                viewId,
                PendingIntent.getBroadcast(
                    context,
                    i,
                    start,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        val setup = ClockActivity.timerSetupIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(
            R.id.timer_widget_new,
            PendingIntent.getActivity(context, 0, setup, PendingIntent.FLAG_IMMUTABLE),
        )
        manager.updateAppWidget(ids, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_START) {
            return super.onReceive(context, intent)
        }
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        if (minutes <= 0) {
            return
        }
        UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.TIMERS)
        val model = DataModel.getDataModel()
        val timer = model.addTimer(minutes * DateUtils.MINUTE_IN_MILLIS, "", false)
        model.startTimer(timer)
        Toast.makeText(context, R.string.timer_created, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val ACTION_START = "org.prauga.clock.widget.START_TIMER"
        const val EXTRA_MINUTES = "minutes"
        val PRESETS =
            listOf(
                R.id.timer_widget_preset_0 to 5,
                R.id.timer_widget_preset_1 to 10,
                R.id.timer_widget_preset_2 to 30,
            )
    }
}
