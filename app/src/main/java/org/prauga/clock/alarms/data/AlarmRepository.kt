/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.data

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.prauga.clock.BuildConfig
import org.prauga.clock.alarms.AlarmStateManager
import org.prauga.clock.alarms.AlarmUpdateHandler
import org.prauga.clock.alarms.ScrollHandler
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmInstance

class AlarmRepository(context: Context, private val io: CoroutineDispatcher = Dispatchers.IO) {
    private val app = context.applicationContext
    private val resolver = app.contentResolver

    val alarms: Flow<List<AlarmRecord>> =
        callbackFlow {
                val observer =
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean) {
                            trySend(Unit)
                        }
                    }
                resolver.registerContentObserver(PROVIDER, true, observer)
                trySend(Unit)
                awaitClose { resolver.unregisterContentObserver(observer) }
            }
            .conflate()
            .map { load() }
            .flowOn(io)

    fun add(alarm: Alarm, onAdded: (Long) -> Unit = {}) {
        updater(onAdded).asyncAddAlarm(alarm)
    }

    suspend fun insert(alarm: Alarm): Long = suspendCoroutine { add(alarm, it::resume) }

    suspend fun get(id: Long): Alarm? = withContext(io) { Alarm.getAlarm(resolver, id) }

    suspend fun update(
        id: Long,
        popToast: Boolean?,
        minorUpdate: Boolean,
        change: (Alarm) -> Unit,
    ) {
        val alarm = withContext(io) { Alarm.getAlarm(resolver, id) } ?: return
        change(alarm)
        updater().asyncUpdateAlarm(alarm, popToast ?: alarm.enabled, minorUpdate)
    }

    suspend fun delete(id: Long) {
        val alarm = withContext(io) { Alarm.getAlarm(resolver, id) } ?: return
        updater().asyncDeleteAlarm(alarm)
    }

    fun predismiss(instance: AlarmInstance) {
        app.startService(
            AlarmStateManager.createStateChangeIntent(
                app,
                AlarmStateManager.ALARM_DISMISS_TAG,
                instance,
                AlarmInstance.PREDISMISSED_STATE,
            )
        )
        updater().showPredismissToast(instance)
    }

    private fun load(): List<AlarmRecord> {
        return Alarm.getAlarmsWithInstances(resolver).sortedWith(ORDER).map { alarm ->
            val instance =
                if (alarm.canPreemptivelyDismiss()) {
                    AlarmInstance.getInstance(resolver, alarm.instanceId.toLong())
                } else {
                    null
                }
            AlarmRecord(alarm, instance)
        }
    }

    private fun updater(onAdded: (Long) -> Unit = {}) =
        AlarmUpdateHandler(
            app,
            object : ScrollHandler {
                override fun setSmoothScrollStableId(stableId: Long) = onAdded(stableId)

                override fun smoothScrollTo(position: Int) = Unit
            },
        )

    private companion object {
        val PROVIDER = "content://${BuildConfig.APPLICATION_ID}/".toUri()
        val ORDER = compareBy<Alarm>({ it.hour }, { it.minutes }, { -it.id })
    }
}
