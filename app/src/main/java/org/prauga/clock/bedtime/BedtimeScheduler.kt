/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar
import org.prauga.clock.DeskClock
import org.prauga.clock.R
import org.prauga.clock.provider.Alarm

/** Schedules the bedtime reminder and keeps it scheduled across reboots and clock changes. */
object BedtimeScheduler {

    const val ACTION_SHOW_BEDTIME = "org.prauga.clock.action.SHOW_BEDTIME"
    private const val ACTION_REMINDER = "org.prauga.clock.action.BEDTIME_REMINDER"
    private const val ACTION_MODE_ON = "org.prauga.clock.action.BEDTIME_MODE_ON"
    private const val ACTION_MODE_OFF = "org.prauga.clock.action.BEDTIME_MODE_OFF"
    private const val CHANNEL_ID = "bedtime"
    private const val NOTIFICATION_ID = 0x0bed

    /** Sets the AlarmManager alarm for the next reminder, or clears it when there is none. */
    @JvmStatic
    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val settings = BedtimeSettings(context)
        val operation = reminderIntent(context)
        alarms.cancel(operation)
        nextReminder(settings)?.let {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, it.timeInMillis, operation)
        }
        scheduleMode(context, alarms, settings)
    }

    /** Bedtime mode turns on at the next bedtime and off at the following wake-up. */
    private fun scheduleMode(context: Context, alarms: AlarmManager, settings: BedtimeSettings) {
        val on = intent(context, ACTION_MODE_ON, 1)
        val off = intent(context, ACTION_MODE_OFF, 2)
        alarms.cancel(on)
        alarms.cancel(off)
        if (!settings.modeEnabled) {
            return
        }
        val now = Calendar.getInstance()
        val nextBedtime = settings.nextBedtime(now) ?: return
        val lastBedtime = (nextBedtime.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val wakeAfterLast = wakeUpAfter(context, settings, lastBedtime)
        // Inside last night's bedtime still: keep the mode on until the wake-up.
        if (
            settings.days.isBitOn(lastBedtime.get(Calendar.DAY_OF_WEEK)) &&
                now.after(lastBedtime) &&
                now.before(wakeAfterLast)
        ) {
            alarms.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                wakeAfterLast.timeInMillis,
                off,
            )
            return
        }
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextBedtime.timeInMillis, on)
    }

    /** The linked wake-up alarm's next ring after [bedtime], or eight hours later. */
    private fun wakeUpAfter(
        context: Context,
        settings: BedtimeSettings,
        bedtime: Calendar,
    ): Calendar {
        val alarm =
            settings.wakeUpAlarmId
                .takeIf { it >= 0 }
                ?.let { Alarm.getAlarm(context.contentResolver, it) }
        return alarm?.takeIf { it.enabled }?.getNextAlarmTime(bedtime)
            ?: (bedtime.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 8) }
    }

    internal fun onModeAlarm(context: Context, intent: Intent) {
        val settings = BedtimeSettings(context)
        when (intent.action) {
            ACTION_MODE_ON -> {
                BedtimeMode.setActive(context, true)
                val wake = wakeUpAfter(context, settings, Calendar.getInstance())
                context
                    .getSystemService(AlarmManager::class.java)
                    .setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        wake.timeInMillis,
                        intent(context, ACTION_MODE_OFF, 2),
                    )
                return
            }
            ACTION_MODE_OFF -> BedtimeMode.setActive(context, false)
        }
        schedule(context)
    }

    internal fun isModeAlarm(intent: Intent) =
        intent.action == ACTION_MODE_ON || intent.action == ACTION_MODE_OFF

    private fun intent(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            code,
            Intent(context, BedtimeReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun nextReminder(settings: BedtimeSettings): Calendar? {
        if (settings.reminderMinutes == 0) {
            return null
        }
        val now = Calendar.getInstance()
        var bedtime = settings.nextBedtime(now) ?: return null
        var reminder =
            (bedtime.clone() as Calendar).apply { add(Calendar.MINUTE, -settings.reminderMinutes) }
        if (!reminder.after(now)) {
            bedtime = settings.nextBedtime(bedtime) ?: return null
            reminder =
                (bedtime.clone() as Calendar).apply {
                    add(Calendar.MINUTE, -settings.reminderMinutes)
                }
        }
        return reminder
    }

    private fun reminderIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, BedtimeReceiver::class.java).setAction(ACTION_REMINDER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    internal fun showReminder(context: Context) {
        val settings = BedtimeSettings(context)
        val bedtime =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.hour)
                set(Calendar.MINUTE, settings.minute)
            }
        val manager = NotificationManagerCompat.from(context)
        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.bedtime_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, DeskClock::class.java)
                    .setAction(ACTION_SHOW_BEDTIME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.stat_notify_bedtime)
                .setContentTitle(
                    context.getString(
                        R.string.bedtime_reminder_notification,
                        DateFormat.getTimeFormat(context).format(bedtime.time),
                    )
                )
                .setContentText(context.getString(R.string.bedtime_reminder_notification_text))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        if (manager.areNotificationsEnabled()) {
            @Suppress("MissingPermission") manager.notify(NOTIFICATION_ID, notification)
        }
    }

    internal fun isReminder(intent: Intent) = intent.action == ACTION_REMINDER
}

/** Shows the bedtime reminder, and reschedules it after a reboot or a clock change. */
class BedtimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (BedtimeScheduler.isModeAlarm(intent)) {
            BedtimeScheduler.onModeAlarm(context, intent)
            return
        }
        if (BedtimeScheduler.isReminder(intent)) {
            BedtimeScheduler.showReminder(context)
        }
        BedtimeScheduler.schedule(context)
    }
}
