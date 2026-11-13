/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Weekdays
import org.prauga.clock.uidata.UiDataModel

@Composable
fun WeekdayButtons(days: Weekdays, onChange: (Int, Boolean) -> Unit) {
    val order = DataModel.getDataModel().weekdayOrder.calendarDays
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (day in order) {
            val on = days.isBitOn(day)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier.size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (on) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onChange(day, !on) },
            ) {
                Text(
                    text = UiDataModel.getUiDataModel().getShortWeekday(day),
                    color =
                        if (on) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
