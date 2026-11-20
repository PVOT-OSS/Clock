/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock

import android.content.Intent
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.PvotSnackbarHost
import com.prauga.pvot.designsystem.navigation.PvotNavBar
import com.prauga.pvot.designsystem.navigation.PvotTabItem
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import kotlinx.coroutines.flow.collectLatest
import org.prauga.clock.alarms.ui.AlarmsScreen
import org.prauga.clock.bedtime.ui.BedtimeScreen
import org.prauga.clock.common.ClockSnackbars
import org.prauga.clock.common.ContentMaxWidth
import org.prauga.clock.common.LocalTabActionSlot
import org.prauga.clock.common.TabActionSlot
import org.prauga.clock.settings.SettingsActivity
import org.prauga.clock.stopwatch.ui.StopwatchScreen
import org.prauga.clock.timer.ui.TimerScreen
import org.prauga.clock.uidata.TabListener
import org.prauga.clock.uidata.UiDataModel
import org.prauga.clock.uidata.UiDataModel.Tab
import org.prauga.clock.worldclock.ui.ClockScreen

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
    val compact by
        remember(listState) {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            }
        }

    val snackbars = remember { SnackbarHostState() }
    LaunchedEffect(snackbars) {
        ClockSnackbars.messages.collectLatest { message ->
            if (message != null) {
                val result =
                    snackbars.showSnackbar(
                        message = message.text,
                        actionLabel = message.actionLabel,
                        duration =
                            if (message.long) SnackbarDuration.Long else SnackbarDuration.Short,
                    )
                if (result == SnackbarResult.ActionPerformed) message.onAction?.run()
            }
        }
    }
    val actions = remember { TabActionSlot() }
    val action = actions.action
    val tabs =
        NAV_TABS.mapIndexed { index, item ->
            if (index == selected && action != null) {
                item.copy(
                    expandedIconRes = action.icon,
                    expandedLabelRes = action.label,
                    contentDescriptionRes = action.label,
                )
            } else {
                item
            }
        }

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
                    if (it == selected && action != null) {
                        action.onClick()
                    } else {
                        selected = it
                        model.setSelectedTab(Tab.entries[it])
                    }
                },
                tabs = tabs,
                modifier = Modifier.widthIn(max = ContentMaxWidth),
            )
        },
        snackbarHost = { PvotSnackbarHost(snackbars) },
    ) { padding ->
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalTabActionSlot provides actions,
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
