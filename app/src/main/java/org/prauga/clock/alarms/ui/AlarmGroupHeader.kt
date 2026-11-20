/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.prauga.pvot.designsystem.components.PvotSwitch
import com.prauga.pvot.designsystem.theme.PvotShapes
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmGroupHeader(
    section: AlarmGroupSection,
    onEvent: (AlarmEvent) -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val group = section.group
    val rotation by
        animateFloatAsState(if (group.collapsed) COLLAPSED_ANGLE else 0f, label = "groupArrow")
    val summary =
        if (section.alarms.isEmpty()) {
            stringResource(R.string.alarm_group_empty)
        } else {
            pluralStringResource(
                R.plurals.alarm_group_summary,
                section.alarms.size,
                section.enabledCount,
                section.alarms.size,
            )
        }
    val switchLabel = stringResource(R.string.alarm_group_switch, group.name)
    Row(
        modifier = modifier.fillMaxWidth().padding(end = PvotSpacing.L),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
    ) {
        Row(
            modifier =
                Modifier.weight(1f)
                    .clip(PvotShapes.Card)
                    .combinedClickable(
                        onClickLabel =
                            stringResource(
                                if (group.collapsed) R.string.alarm_group_expand
                                else R.string.alarm_group_collapse
                            ),
                        onLongClickLabel =
                            onLongPress?.let { stringResource(R.string.alarm_group_options) },
                        onLongClick = onLongPress,
                        onClick = {
                            onEvent(AlarmEvent.SetGroupCollapsed(group.id, !group.collapsed))
                        },
                    )
                    .padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        ) {
            Icon(
                painterResource(R.drawable.ic_t_chevron_down),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PvotSwitch(
            checked = section.anyOn,
            onCheckedChange = { onEvent(AlarmEvent.SetGroupEnabled(group.id, it)) },
            enabled = section.alarms.isNotEmpty(),
            modifier = Modifier.semantics { contentDescription = switchLabel },
        )
    }
}

private const val COLLAPSED_ANGLE = -90f
