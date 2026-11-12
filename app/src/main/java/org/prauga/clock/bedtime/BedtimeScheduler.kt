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

/** Schedules the bedtime reminder and keeps it scheduled across reboots and clock changes. */
object BedtimeScheduler {

    const val ACTION_SHOW_BEDTIME = "org.prauga.clock.action.SHOW_BEDTIME"
    private const val ACTION_REMINDER = "org.prauga.clock.action.BEDTIME_REMINDER"
    private const val CHANNEL_ID = "bedtime"
    private const val NOTIFICATION_ID = 0x0bed

    /** Sets the AlarmManager alarm for the next reminder, or clears it when there is none. */
    @JvmStatic
    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val operation = reminderIntent(context)
        alarms.cancel(operation)
        val reminderAt = nextReminder(BedtimeSettings(context)) ?: return
        alarms.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderAt.timeInMillis,
            operation,
        )
    }

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
        if (BedtimeScheduler.isReminder(intent)) {
            BedtimeScheduler.showReminder(context)
        }
        BedtimeScheduler.schedule(context)
    }
}
