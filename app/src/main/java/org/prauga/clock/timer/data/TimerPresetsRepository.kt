/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.data

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.prauga.clock.LogUtils

class TimerPresetsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.createDeviceProtectedStorageContext().let {
            it.getSharedPreferences("${it.packageName}_preferences", Context.MODE_PRIVATE)
        }
    private val writes = Mutex()

    val presets: Flow<List<TimerPreset>> =
        callbackFlow {
                val listener =
                    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (key == KEY_PRESETS) trySend(Unit)
                    }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                trySend(Unit)
                awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            .conflate()
            .map { load() }
            .flowOn(Dispatchers.IO)

    suspend fun save(id: Long?, name: String, lengthMillis: Long) = update { current ->
        if (id == null) {
            current + TimerPreset((current.maxOfOrNull { it.id } ?: 0L) + 1, name, lengthMillis)
        } else {
            current.map {
                if (it.id == id) it.copy(name = name, lengthMillis = lengthMillis) else it
            }
        }
    }

    suspend fun delete(id: Long) = update { current -> current.filterNot { it.id == id } }

    private suspend fun update(change: (List<TimerPreset>) -> List<TimerPreset>) =
        writes.withLock { withContext(Dispatchers.IO) { store(change(load())) } }

    private fun load(): List<TimerPreset> {
        val json = prefs.getString(KEY_PRESETS, null) ?: return SEEDS.also(::store)
        return try {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TimerPreset(item.getLong(ID), item.optString(NAME), item.getLong(LENGTH))
            }
        } catch (e: JSONException) {
            LogUtils.e("Could not read the saved timers", e)
            emptyList()
        }
    }

    private fun store(presets: List<TimerPreset>) {
        val array = JSONArray()
        presets.forEach {
            array.put(JSONObject().put(ID, it.id).put(NAME, it.name).put(LENGTH, it.lengthMillis))
        }
        prefs.edit { putString(KEY_PRESETS, array.toString()) }
    }

    private companion object {
        const val KEY_PRESETS = "timer_presets"
        const val ID = "id"
        const val NAME = "name"
        const val LENGTH = "length"

        val SEEDS =
            listOf(5L, 10L, 30L, 45L).mapIndexed { index, minutes ->
                TimerPreset(index + 1L, "", minutes * DateUtils.MINUTE_IN_MILLIS)
            }
    }
}
