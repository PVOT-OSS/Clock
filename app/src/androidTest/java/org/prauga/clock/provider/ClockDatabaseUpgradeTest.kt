/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.provider

import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ClockDatabaseUpgradeTest {

    private lateinit var file: File
    private lateinit var context: Context

    @Before
    fun setUp() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(target.cacheDir, "alarms-upgrade-test.db")
        SQLiteDatabase.deleteDatabase(file)
        context =
            object : ContextWrapper(target) {
                override fun getDatabasePath(name: String): File = file
            }
    }

    @After
    fun tearDown() {
        SQLiteDatabase.deleteDatabase(file)
    }

    @Test
    fun upgradeFromVersion13KeepsAlarmsAndAddsNoColor() {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE alarm_templates (_id INTEGER PRIMARY KEY, hour INTEGER NOT NULL, " +
                    "minutes INTEGER NOT NULL, daysofweek INTEGER NOT NULL, " +
                    "enabled INTEGER NOT NULL, vibrate INTEGER NOT NULL, label TEXT NOT NULL, " +
                    "ringtone TEXT, delete_after_use INTEGER NOT NULL DEFAULT 0, " +
                    "incvol INTEGER NOT NULL DEFAULT 0, " +
                    "scheduled_date INTEGER NOT NULL DEFAULT -1, " +
                    "pause_start INTEGER NOT NULL DEFAULT -1, " +
                    "pause_end INTEGER NOT NULL DEFAULT -1)"
            )
            db.execSQL(
                "CREATE TABLE alarm_instances (_id INTEGER PRIMARY KEY, year INTEGER NOT NULL, " +
                    "month INTEGER NOT NULL, day INTEGER NOT NULL, hour INTEGER NOT NULL, " +
                    "minutes INTEGER NOT NULL, vibrate INTEGER NOT NULL, label TEXT NOT NULL, " +
                    "ringtone TEXT, alarm_state INTEGER NOT NULL, alarm_id INTEGER REFERENCES " +
                    "alarm_templates(_id) ON UPDATE CASCADE ON DELETE CASCADE, " +
                    "incvol INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL(
                "INSERT INTO alarm_templates (_id, hour, minutes, daysofweek, enabled, vibrate, " +
                    "label, ringtone) VALUES (7, 6, 45, 31, 1, 1, 'Gym', NULL)"
            )
            db.version = OLD_VERSION
        }

        ClockDatabaseHelper(context).use { helper ->
            helper.readableDatabase
                .rawQuery(
                    "SELECT hour, minutes, label, ${ClockContract.AlarmsColumns.COLOR} " +
                        "FROM alarm_templates WHERE _id = 7",
                    null,
                )
                .use { cursor ->
                    assertEquals(1, cursor.count)
                    cursor.moveToFirst()
                    assertEquals(6, cursor.getInt(0))
                    assertEquals(45, cursor.getInt(1))
                    assertEquals("Gym", cursor.getString(2))
                    assertEquals(Alarm.NO_COLOR, cursor.getInt(3))
                }
        }
    }

    private companion object {
        const val OLD_VERSION = 13
    }
}
