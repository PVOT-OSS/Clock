/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.clock.alarms.data.AlarmRecord
import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmGroup

class AlarmListTest {

    private val work = AlarmGroup(id = 1, name = "Work", position = 0, collapsed = false)
    private val gym = AlarmGroup(id = 2, name = "Gym", position = 1, collapsed = true)

    @Test
    fun wakeUpAlarmStaysOutOfItsGroup() {
        val items = listOf(item(10, work.id), item(11, work.id))

        val content = alarmsContent(items, listOf(work), wakeUpId = 10)

        assertEquals(10L, content.wakeUp?.id)
        assertEquals(listOf(11L), content.groups.single().alarms.map { it.id })
        assertTrue(content.others.isEmpty())
    }

    @Test
    fun alarmOfAMissingGroupGoesToOthers() {
        val content = alarmsContent(listOf(item(20, groupId = 99)), listOf(work), Alarm.INVALID_ID)

        assertEquals(listOf(20L), content.others.map { it.id })
        assertTrue(content.groups.single().alarms.isEmpty())
    }

    @Test
    fun groupsKeepTheirOrderAndEmptyOnesStay() {
        val content = alarmsContent(listOf(item(30, gym.id)), listOf(work, gym), Alarm.INVALID_ID)

        assertEquals(listOf("Work", "Gym"), content.groups.map { it.group.name })
        assertEquals(0, content.groups[0].alarms.size)
        assertEquals(1, content.groups[1].alarms.size)
    }

    @Test
    fun entriesHideCollapsedAlarmsAndTitleOthersOnlyBesideGroups() {
        val content =
            alarmsContent(
                listOf(item(40, work.id), item(41, work.id), item(42, work.id), item(43, gym.id)) +
                    listOf(item(44), item(45)),
                listOf(work, gym),
                Alarm.INVALID_ID,
            )

        val entries = alarmListEntries(content, columns = 2)

        assertEquals(
            listOf(
                "group-1",
                "row-40",
                "row-42",
                "group-2",
                "title-${AlarmListSection.Others}",
                "row-44",
            ),
            entries.map { it.key },
        )
        assertEquals(3, entries.indexOfAlarm(43))
        assertEquals(5, entries.indexOfAlarm(45))
    }

    @Test
    fun plainListHasNoTitles() {
        val content = alarmsContent(listOf(item(50), item(51)), emptyList(), Alarm.INVALID_ID)

        val entries = alarmListEntries(content, columns = 1)

        assertTrue(entries.all { it is AlarmListEntry.Cards })
        assertNull(content.wakeUp)
    }

    private fun item(id: Long, groupId: Long = Alarm.NO_GROUP): AlarmItem {
        lateinit var alarm: Alarm
        InstrumentationRegistry.getInstrumentation().runOnMainSync { alarm = Alarm(7, 0) }
        alarm.id = id
        alarm.groupId = groupId
        alarm.enabled = true
        return AlarmItem(AlarmRecord(alarm, null))
    }
}
