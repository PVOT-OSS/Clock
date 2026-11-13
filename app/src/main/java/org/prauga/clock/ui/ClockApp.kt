/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.navigation.PvotNavBar
import com.prauga.pvot.designsystem.navigation.PvotTabItem
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import org.prauga.clock.DeskClock
import org.prauga.clock.R
import org.prauga.clock.settings.SettingsActivity
import org.prauga.clock.ui.clock.ClockScreen
import org.prauga.clock.ui.stopwatch.StopwatchScreen
import org.prauga.clock.uidata.UiDataModel
import org.prauga.clock.uidata.UiDataModel.Tab
import org.prauga.clock.worldclock.CitySelectionActivity

private val NAV_TABS =
    listOf(
        PvotTabItem(R.drawable.ic_tab_alarm_static, R.string.menu_alarm, R.string.menu_alarm),
        PvotTabItem(R.drawable.ic_tab_clock_static, R.string.menu_clock, R.string.menu_clock),
        PvotTabItem(R.drawable.ic_tab_timer_static, R.string.menu_timer, R.string.menu_timer),
        PvotTabItem(
            R.drawable.ic_tab_stopwatch_static,
            R.string.menu_stopwatch,
            R.string.menu_stopwatch,
        ),
        PvotTabItem(R.drawable.ic_tab_bedtime, R.string.menu_bedtime, R.string.menu_bedtime),
    )

@Composable
fun ClockApp() {
    val model = UiDataModel.getUiDataModel()
    var selected by rememberSaveable { mutableIntStateOf(model.selectedTab.ordinal) }
    val tab = Tab.entries[selected]
    val context = LocalContext.current

    PvotScreenScaffold(
        topBar = {
            PvotTopBar(
                title = stringResource(tab.labelResId),
                actions = {
                    PvotTopBarAction(
                        icon = painterResource(R.drawable.ic_settings),
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
        floatingAction = {
            if (tab == Tab.CLOCKS) {
                PvotFab(
                    icon = painterResource(R.drawable.ic_public),
                    contentDescription = stringResource(R.string.button_cities),
                    onClick = {
                        context.startActivity(Intent(context, CitySelectionActivity::class.java))
                    },
                )
            }
        },
    ) { padding ->
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground
        ) {
            when (tab) {
                Tab.CLOCKS -> ClockScreen(padding)
                Tab.STOPWATCH -> StopwatchScreen(padding)
                else -> ClassicTab(tab, padding, context)
            }
        }
    }
}

/** A tab not yet rebuilt in Compose: opens it in the classic screens. */
@Composable
private fun ClassicTab(tab: Tab, padding: PaddingValues, context: Context) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(tab.labelResId), style = MaterialTheme.typography.headlineSmall)
        PvotButton(
            text = stringResource(R.string.open_classic_view),
            onClick = {
                UiDataModel.getUiDataModel().setSelectedTab(tab)
                context.startActivity(Intent(context, DeskClock::class.java))
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
