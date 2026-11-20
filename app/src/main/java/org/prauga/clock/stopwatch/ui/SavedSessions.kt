/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.stopwatch.ui

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.stopwatch.data.StopwatchSession

@Composable
fun SavedSessionsSheet(
    visible: Boolean,
    sessions: List<StopwatchSession>,
    expanded: List<Long>,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    PvotBottomSheet(
        visible = visible,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.stopwatch_saved),
    ) {
        if (sessions.isEmpty()) {
            NoSavedSessions()
        }
        sessions.forEach { session ->
            SavedSessionCard(
                session = session,
                expanded = session.savedAt in expanded,
                onToggle = { onToggle(session.savedAt) },
                onDelete = { onDelete(session.savedAt) },
            )
        }
    }
}

@Composable
fun NoSavedSessions(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.stopwatch_no_saved),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(PvotSpacing.L),
    )
}

@Composable
fun SavedSessionCard(
    session: StopwatchSession,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val date =
        DateUtils.formatDateTime(
            context,
            session.savedAt,
            DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_ABBREV_MONTH,
        )
    val supporting =
        if (session.laps.isEmpty()) date
        else
            "$date · " +
                pluralStringResource(
                    R.plurals.stopwatch_lap_count,
                    session.laps.size,
                    session.laps.size,
                )
    PvotCard(modifier.fillMaxWidth()) {
        PvotListRow(
            title = formatStopwatch(session.total),
            supportingText = supporting,
            onClick = if (session.laps.isEmpty()) null else onToggle,
            trailing = {
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_t_trash),
                    contentDescription = stringResource(R.string.stopwatch_delete_saved),
                    onClick = onDelete,
                    colors = PvotIconButtonDefaults.plainColors(),
                )
            },
        )
        AnimatedVisibility(expanded && session.laps.isNotEmpty()) {
            Column(
                modifier =
                    Modifier.padding(
                        start = PvotSpacing.L,
                        end = PvotSpacing.L,
                        bottom = PvotSpacing.L,
                    ),
                verticalArrangement = Arrangement.spacedBy(PvotSpacing.XS),
            ) {
                var accumulated = 0L
                session.laps.forEachIndexed { index, lap ->
                    accumulated += lap
                    LapRow(index + 1, lap, accumulated)
                }
            }
        }
    }
}
