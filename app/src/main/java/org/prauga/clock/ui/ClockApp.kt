/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import android.content.Intent
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.navigation.PvotNavBar
import com.prauga.pvot.designsystem.navigation.PvotTabItem
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import org.prauga.clock.R
import org.prauga.clock.settings.SettingsActivity
import org.prauga.clock.ui.alarms.AlarmsScreen
import org.prauga.clock.ui.bedtime.BedtimeScreen
import org.prauga.clock.ui.clock.ClockScreen
import org.prauga.clock.ui.stopwatch.StopwatchScreen
import org.prauga.clock.ui.timer.TimerScreen
import org.prauga.clock.uidata.TabListener
import org.prauga.clock.uidata.UiDataModel
import org.prauga.clock.uidata.UiDataModel.Tab

private val NAV_TABS =
    listOf(
        PvotTabItem(R.drawable.ic_t_alarm, R.string.menu_alarm, R.string.menu_alarm),
        PvotTabItem(R.drawable.ic_t_clock, R.string.menu_clock, R.string.menu_clock),
        PvotTabItem(R.drawable.ic_t_hourglass, R.string.menu_timer, R.string.menu_timer),
        PvotTabItem(R.drawable.ic_t_stopwatch, R.string.menu_stopwatch, R.string.menu_stopwatch),
        PvotTabItem(R.drawable.ic_t_moon, R.string.menu_bedtime, R.string.menu_bedtime),
    )

@Composable
fun ClockApp(timerSetupRequests: Int, createAlarmRequests: Int, shownAlarm: Long) {
    val model = UiDataModel.getUiDataModel()
    var selected by rememberSaveable { mutableIntStateOf(model.selectedTab.ordinal) }
    // Intents, shortcuts and widgets pick tabs through the model too.
    DisposableEffect(Unit) {
        val listener = TabListener { selected = it.ordinal }
        model.addTabListener(listener)
        selected = model.selectedTab.ordinal
        onDispose { model.removeTabListener(listener) }
    }
    val tab = Tab.entries[selected]
    val context = LocalContext.current
    val listStates = Tab.entries.map { rememberLazyListState() }
    val listState = listStates[selected]
    // The large title folds into a compact frosted bar once the tab's list scrolls.
    val compact by
        remember(listState) {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }

    val fab = remember { FloatingActionSlot() }
    fab.expanded = !compact

    PvotScreenScaffold(
        topBar = {
            PvotTopBar(
                title = stringResource(tab.labelResId),
                compact = compact,
                actions = {
                    PvotTopBarAction(
                        icon = painterResource(R.drawable.ic_t_settings),
                        contentDescription = stringResource(R.string.settings),
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        },
                    )
                },
            )
        },
        bottomBar = {
            PvotNavBar(
                selectedTab = selected,
                onTabClick = {
                    selected = it
                    model.setSelectedTab(Tab.entries[it])
                },
                tabs = NAV_TABS,
            )
        },
        floatingAction = { fab.content?.invoke() },
    ) { padding ->
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalFloatingActionSlot provides fab,
        ) {
            when (tab) {
                Tab.CLOCKS -> ClockScreen(padding, listState)
                Tab.STOPWATCH -> StopwatchScreen(padding, listState)
                Tab.TIMERS -> TimerScreen(padding, listState, timerSetupRequests)
                Tab.ALARMS -> AlarmsScreen(padding, listState, createAlarmRequests, shownAlarm)
                Tab.BEDTIME -> BedtimeScreen(padding, listState)
            }
        }
    }
}
