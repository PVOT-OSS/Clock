/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.prauga.clock.R
import org.prauga.clock.provider.AlarmInstance

/**
 * Sunrise: the screen slowly brightens from a dark red to a warm white over the minutes before the
 * next alarm, as a gentler start than the sound alone.
 */
object Sunrise {

    const val KEY_MINUTES = "sunrise_minutes"
    internal const val EXTRA_ALARM_TIME = "alarm_time"
    private const val CHANNEL_ID = "sunrise"
    internal const val NOTIFICATION_ID = 0x5005

    private fun minutes(context: Context): Int {
        val storage = context.createDeviceProtectedStorageContext()
        return storage
            .getSharedPreferences("${storage.packageName}_preferences", Context.MODE_PRIVATE)
            .getString(KEY_MINUTES, "0")
            ?.toIntOrNull() ?: 0
    }

    /** Schedules the sunrise before [nextAlarm], or clears it when there is none. */
    @JvmStatic
    @JvmOverloads
    fun schedule(context: Context, nextAlarm: AlarmInstance?, minutes: Int = minutes(context)) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val alarmTime = nextAlarm?.alarmTime?.timeInMillis ?: 0L
        val operation = operation(context, alarmTime)
        alarms.cancel(operation)
        if (nextAlarm == null || minutes == 0) {
            return
        }
        val start = alarmTime - minutes * 60_000L
        if (start > System.currentTimeMillis()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start, operation)
        }
    }

    /** Applies a new sunrise length from the settings before it is saved. */
    @JvmStatic
    fun onSettingChanged(context: Context, newValue: Any?) {
        val minutes = newValue?.toString()?.toIntOrNull() ?: 0
        Thread { schedule(context, AlarmStateManager.getNextFiringAlarm(context), minutes) }.start()
    }

    private fun operation(context: Context, alarmTime: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SunriseReceiver::class.java).putExtra(EXTRA_ALARM_TIME, alarmTime),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    internal fun show(context: Context, alarmTime: Long) {
        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.sunrise_channel),
                        NotificationManager.IMPORTANCE_HIGH,
                    )
                    .apply { setSound(null, null) }
            )
        val activity =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, SunriseActivity::class.java)
                    .putExtra(EXTRA_ALARM_TIME, alarmTime)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.stat_notify_alarm)
                .setContentTitle(context.getString(R.string.sunrise_notification))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)
                .setContentIntent(activity)
                .setFullScreenIntent(activity, true)
                .setTimeoutAfter(alarmTime - System.currentTimeMillis())
                .build()
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            @Suppress("MissingPermission") manager.notify(NOTIFICATION_ID, notification)
        }
    }
}

/** Starts the sunrise when it is due. */
class SunriseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmTime = intent.getLongExtra(Sunrise.EXTRA_ALARM_TIME, 0L)
        if (alarmTime > System.currentTimeMillis()) {
            Sunrise.show(context, alarmTime)
        }
    }
}

/** A full-screen sunrise over the lock screen until the alarm rings or the user taps it away. */
class SunriseActivity : ComponentActivity() {

    private var animator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val view = View(this).apply { setOnClickListener { finish() } }
        setContentView(view)
        NotificationManagerCompat.from(this).cancel(Sunrise.NOTIFICATION_ID)

        val alarmTime = intent.getLongExtra(Sunrise.EXTRA_ALARM_TIME, 0L)
        val remaining = alarmTime - System.currentTimeMillis()
        if (remaining <= 0) {
            finish()
            return
        }
        val colors = ArgbEvaluator()
        animator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = remaining
                interpolator = LinearInterpolator()
                addUpdateListener {
                    val f = it.animatedValue as Float
                    view.setBackgroundColor(
                        if (f < 0.5f) colors.evaluate(f * 2, DAWN, SUNRISE) as Int
                        else colors.evaluate((f - 0.5f) * 2, SUNRISE, DAY) as Int
                    )
                    window.attributes =
                        window.attributes.apply { screenBrightness = 0.01f + 0.99f * f }
                }
                start()
            }
        // The alarm takes over the screen when it rings.
        view.postDelayed({ finish() }, remaining + 1_000)
    }

    override fun onDestroy() {
        animator?.cancel()
        super.onDestroy()
    }

    private companion object {
        const val DAWN = 0xFF1A0500.toInt()
        const val SUNRISE = 0xFFC1440E.toInt()
        const val DAY = 0xFFFFE6B3.toInt()
    }
}
