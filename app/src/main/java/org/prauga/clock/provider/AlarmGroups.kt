/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import org.prauga.clock.provider.ClockContract.AlarmGroupsColumns

data class AlarmGroup(val id: Long, val name: String, val position: Int, val collapsed: Boolean)

object AlarmGroups {

    private val COLUMNS =
        arrayOf(
            AlarmGroupsColumns._ID,
            AlarmGroupsColumns.NAME,
            AlarmGroupsColumns.POSITION,
            AlarmGroupsColumns.COLLAPSED,
        )

    fun getAll(resolver: ContentResolver): List<AlarmGroup> =
        resolver
            .query(
                AlarmGroupsColumns.CONTENT_URI,
                COLUMNS,
                null,
                null,
                "${AlarmGroupsColumns.POSITION}, ${AlarmGroupsColumns._ID}",
            )
            ?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            AlarmGroup(
                                id = cursor.getLong(0),
                                name = cursor.getString(1),
                                position = cursor.getInt(2),
                                collapsed = cursor.getInt(3) != 0,
                            )
                        )
                    }
                }
            }
            .orEmpty()

    fun members(resolver: ContentResolver, id: Long): List<Alarm> =
        Alarm.getAlarms(resolver, "${ClockContract.AlarmsColumns.GROUP_ID}=?", id.toString())

    fun insert(resolver: ContentResolver, name: String): Long {
        val position = (getAll(resolver).maxOfOrNull { it.position } ?: -1) + 1
        val values =
            ContentValues(2).apply {
                put(AlarmGroupsColumns.NAME, name)
                put(AlarmGroupsColumns.POSITION, position)
            }
        val uri = resolver.insert(AlarmGroupsColumns.CONTENT_URI, values) ?: return Alarm.NO_GROUP
        return ContentUris.parseId(uri)
    }

    fun rename(resolver: ContentResolver, id: Long, name: String) {
        update(resolver, id, ContentValues(1).apply { put(AlarmGroupsColumns.NAME, name) })
    }

    fun setCollapsed(resolver: ContentResolver, id: Long, collapsed: Boolean) {
        update(
            resolver,
            id,
            ContentValues(1).apply { put(AlarmGroupsColumns.COLLAPSED, if (collapsed) 1 else 0) },
        )
    }

    fun delete(resolver: ContentResolver, id: Long) {
        resolver.delete(ContentUris.withAppendedId(AlarmGroupsColumns.CONTENT_URI, id), null, null)
    }

    private fun update(resolver: ContentResolver, id: Long, values: ContentValues) {
        resolver.update(
            ContentUris.withAppendedId(AlarmGroupsColumns.CONTENT_URI, id),
            values,
            null,
            null,
        )
    }
}
