/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.data

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.bedtime.BedtimeMode
import org.prauga.clock.bedtime.BedtimeScheduler
import org.prauga.clock.bedtime.BedtimeSettings
import org.prauga.clock.bedtime.SleepSoundService
import org.prauga.clock.bedtime.TomorrowsEvent
import org.prauga.clock.data.Weekdays
import org.prauga.clock.provider.Alarm

class BedtimeRepository(
    context: Context,
    private val alarms: AlarmRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val app = context.applicationContext
    private val settings = BedtimeSettings(app)

    suspend fun load(): BedtimeSnapshot =
        withContext(io) {
            val wakeUp =
                settings.wakeUpAlarmId
                    .takeIf { it >= 0 }
                    ?.let { Alarm.getAlarm(app.contentResolver, it) }
                    ?.toWakeUp()
            BedtimeSnapshot(
                enabled = settings.enabled,
                hour = settings.hour,
                minute = settings.minute,
                days = settings.days.bits,
                reminderMinutes = settings.reminderMinutes,
                introDone = settings.introDone,
                modeEnabled = settings.modeEnabled,
                hasModeAccess = BedtimeMode.hasAccess(app),
                wakeUp = wakeUp,
                sleepSound = settings.sleepSound,
                sleepSoundMinutes = settings.sleepSoundMinutes,
                sleepSoundPlaying = SleepSoundService.isPlaying,
                sleepGoalMinutes = settings.sleepGoalMinutes,
            )
        }

    suspend fun change(block: BedtimeSettings.() -> Unit) {
        withContext(io) {
            settings.block()
            BedtimeScheduler.schedule(app)
        }
    }

    fun removeMode() = BedtimeMode.remove(app)

    suspend fun wakeUpChoices(): List<WakeUpAlarm> =
        withContext(io) {
            Alarm.getAlarms(app.contentResolver, null)
                .sortedWith(compareBy({ it.hour }, { it.minutes }))
                .map { it.toWakeUp() }
        }

    fun addWakeUpAlarm(hour: Int, minute: Int, days: Int, label: String, onAdded: (Long) -> Unit) {
        alarms.add(
            Alarm(hour, minute).apply {
                daysOfWeek = Weekdays.fromBits(days)
                enabled = true
                this.label = label
            },
            onAdded,
        )
    }

    suspend fun setWakeUpTime(id: Long, hour: Int, minute: Int) =
        alarms.update(id, popToast = null, minorUpdate = false) {
            it.hour = hour
            it.minutes = minute
        }

    fun startSleepSound(sound: Int, minutes: Int) =
        SleepSoundService.start(app, SleepSoundService.Noise.entries[sound], minutes)

    fun stopSleepSound() = SleepSoundService.stop(app)

    suspend fun tomorrowsEvent(): String = withContext(io) { TomorrowsEvent.describe(app) }

    private fun Alarm.toWakeUp() = WakeUpAlarm(id, hour, minutes, enabled, daysOfWeek.bits)
}
