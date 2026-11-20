/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.prauga.clock.LogUtils

class StopwatchSessionsRepository(
    context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val storage = context.applicationContext.createDeviceProtectedStorageContext()
    private val prefs: SharedPreferences by lazy {
        storage.getSharedPreferences("${storage.packageName}_preferences", Context.MODE_PRIVATE)
    }
    private val writes = Mutex()

    val sessions: Flow<List<StopwatchSession>> =
        callbackFlow {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (key == KEY) trySend(Unit)
                    }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                send(Unit)
                awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            .map { read() }
            .flowOn(io)

    suspend fun save(session: StopwatchSession) = update { listOf(session) + it }

    suspend fun delete(savedAt: Long) = update { all -> all.filter { it.savedAt != savedAt } }

    private suspend fun update(change: (List<StopwatchSession>) -> List<StopwatchSession>) {
        withContext(io) {
            writes.withLock {
                val json = JSONArray(change(read()).map { it.toJson() }).toString()
                prefs.edit { putString(KEY, json) }
            }
        }
    }

    private fun read(): List<StopwatchSession> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            List(array.length()) { array.getJSONObject(it).toSession() }
        } catch (e: JSONException) {
            LogUtils.w("Could not read saved stopwatch times: %s", e)
            emptyList()
        }
    }

    private fun StopwatchSession.toJson(): JSONObject =
        JSONObject().put(SAVED_AT, savedAt).put(TOTAL, total).put(LAPS, JSONArray(laps))

    private fun JSONObject.toSession(): StopwatchSession {
        val laps = getJSONArray(LAPS)
        return StopwatchSession(
            savedAt = getLong(SAVED_AT),
            total = getLong(TOTAL),
            laps = List(laps.length()) { laps.getLong(it) },
        )
    }

    private companion object {
        const val KEY = "stopwatch_saved_sessions"
        const val SAVED_AT = "savedAt"
        const val TOTAL = "total"
        const val LAPS = "laps"
    }
}
