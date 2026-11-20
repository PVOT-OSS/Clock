/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.common.ChoiceSheet

private val GOAL_CHOICES =
    (MIN_GOAL_HOURS * MINUTES_PER_HOUR..MAX_GOAL_HOURS * MINUTES_PER_HOUR step GOAL_STEP_MINUTES)
        .toList()
private const val MIN_GOAL_HOURS = 4
private const val MAX_GOAL_HOURS = 12
private const val GOAL_STEP_MINUTES = 30

@Composable
fun SleepGoalLine(
    status: SleepGoalStatus,
    goalMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goal = sleepLengthText(goalMinutes)
    val text =
        when (status) {
            SleepGoalStatus.Below -> stringResource(R.string.bedtime_goal_below, goal)
            SleepGoalStatus.Meets -> stringResource(R.string.bedtime_goal_meets, goal)
            SleepGoalStatus.Above -> stringResource(R.string.bedtime_goal_above, goal)
        }
    PvotCard(modifier = modifier, onClick = onClick, shape = CircleShape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            modifier = Modifier.padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.M),
        ) {
            Icon(
                painterResource(R.drawable.ic_t_target),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SleepGoalSheet(
    visible: Boolean,
    goalMinutes: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val entries = GOAL_CHOICES.map { sleepLengthText(it) }
    ChoiceSheet(
        visible = visible,
        title = stringResource(R.string.bedtime_goal_title),
        entries = entries,
        selected = GOAL_CHOICES.indexOf(goalMinutes),
        onDismiss = onDismiss,
        onSelect = { onSelect(GOAL_CHOICES[it]) },
    )
}
