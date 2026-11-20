/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prauga.pvot.designsystem.adaptive.PvotTabletopSplit
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.common.ScreenAction
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.Timer

@Composable
fun TimerTabletop(
    timers: List<TimerItem>,
    padding: PaddingValues,
    onEvent: (TimerEvent) -> Unit,
    onAdd: () -> Unit,
) {
    val now = rememberNow(timers)
    ScreenAction(R.drawable.ic_t_plus, R.string.new_timer, onAdd)
    PvotTabletopSplit(
        modifier = Modifier.fillMaxSize(),
        top = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = PvotSpacing.L,
                        end = PvotSpacing.L,
                        top = padding.calculateTopPadding(),
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(PvotSpacing.S, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(timers, key = { it.id }) { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(timerLabel(item), style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatRemaining(item.remainingAt(now)),
                            style =
                                if (timers.size == 1) MaterialTheme.typography.displayLarge
                                else MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Light,
                            color =
                                if (item.state == Timer.State.EXPIRED) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    }
                }
            }
        },
        bottom = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = PvotSpacing.L,
                        end = PvotSpacing.L,
                        top = PvotSpacing.M,
                        bottom = screenPadding(padding).calculateBottomPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items(timers, key = { it.id }) { item ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (timers.size > 1) {
                            Text(
                                timerLabel(item),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TimerControls(item, onEvent)
                    }
                }
            }
        },
    )
}

@Composable
fun timerLabel(item: TimerItem): String =
    item.label.ifEmpty { stringResource(R.string.timer_notification_label) }
