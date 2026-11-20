/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.common.formatTime

private val ChipMinWidth: Dp = 160.dp

@Composable
fun BedtimeDial(
    content: BedtimeContent,
    onEvent: (BedtimeEvent) -> Unit,
    onPickGoal: () -> Unit,
    modifier: Modifier = Modifier,
    fitHeight: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
    ) {
        SleepDial(
            bedMinutes = content.bedMinutes,
            wakeMinutes = content.wakeMinutes,
            wakeAdjustable = content.bedtime.wakeUp != null,
            onBedtimeChange = {
                onEvent(BedtimeEvent.SetTime(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR))
            },
            onWakeUpChange = {
                onEvent(BedtimeEvent.SetWakeUpTime(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR))
            },
            modifier = if (fitHeight) Modifier.weight(1f, fill = false) else Modifier,
        )
        SleepGoalLine(
            status = content.goalStatus,
            goalMinutes = content.bedtime.sleepGoalMinutes,
            onClick = onPickGoal,
        )
    }
}

@Composable
fun BedtimeChips(
    content: BedtimeContent,
    onPickBedtime: () -> Unit,
    onChooseWakeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bedtime = content.bedtime
    val wakeUp = bedtime.wakeUp
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
    ) {
        TimeChip(
            icon = R.drawable.ic_t_moon,
            label = stringResource(R.string.bedtime_schedule_title),
            time = formatTime(context, bedtime.hour, bedtime.minute),
            onClick = onPickBedtime,
            modifier = Modifier.weight(1f).widthIn(min = ChipMinWidth),
        )
        TimeChip(
            icon = R.drawable.ic_t_sun,
            label = stringResource(R.string.wakeup_title),
            time =
                if (wakeUp == null) stringResource(R.string.wakeup_not_set)
                else formatTime(context, wakeUp.hour, wakeUp.minute),
            onClick = onChooseWakeUp,
            modifier = Modifier.weight(1f).widthIn(min = ChipMinWidth),
        )
    }
}

@Composable
private fun TimeChip(
    @DrawableRes icon: Int,
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotCard(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
            modifier = Modifier.padding(PvotSpacing.L),
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(time, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
