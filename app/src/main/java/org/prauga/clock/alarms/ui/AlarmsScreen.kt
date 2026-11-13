/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.motion.pvotReveal
import java.time.LocalTime
import java.util.concurrent.Executors
import org.prauga.clock.BuildConfig
import org.prauga.clock.R
import org.prauga.clock.alarms.AlarmUpdateHandler
import org.prauga.clock.alarms.ScrollHandler
import org.prauga.clock.common.ScreenFab
import org.prauga.clock.common.screenPadding
import org.prauga.clock.provider.Alarm

@Composable
fun AlarmsScreen(
    padding: PaddingValues,
    listState: LazyListState,
    createRequests: Int,
    shownAlarm: Long,
) {
    val context = LocalContext.current
    // Alarm.equals compares ids only: store every reload and redraw the cards on each version.
    var alarms by remember { mutableStateOf(emptyList<Alarm>(), neverEqualPolicy()) }
    var version by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val main = Handler(Looper.getMainLooper())
        val reload = {
            Executors.newSingleThreadExecutor().execute {
                val list = Alarm.getAlarmsWithInstances(context.contentResolver).sortedWith(ORDER)
                main.post {
                    alarms = list
                    loaded = true
                    version++
                }
            }
        }
        val observer =
            object : ContentObserver(main) {
                override fun onChange(selfChange: Boolean) = reload()
            }
        context.contentResolver.registerContentObserver(PROVIDER, true, observer)
        reload()
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val updater = remember { updater(context) }
    var expanded by rememberSaveable { mutableLongStateOf(-1L) }
    var editingTime by remember { mutableStateOf<Alarm?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var timeDialogOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(createRequests) {
        if (createRequests > 0) {
            adding = true
            timeDialogOpen = true
        }
    }
    LaunchedEffect(shownAlarm, alarms) {
        val index = alarms.indexOfFirst { it.id == shownAlarm }
        if (index >= 0) {
            expanded = shownAlarm
            listState.animateScrollToItem(index)
        }
    }

    ScreenFab(R.drawable.ic_t_plus, R.string.new_alarm) {
        adding = true
        timeDialogOpen = true
    }

    if (loaded && alarms.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            PvotEmptyContent(
                message = stringResource(R.string.no_alarms),
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_t_alarm),
                        contentDescription = null,
                        modifier = Modifier.size(PvotIconButtonDefaults.Size),
                    )
                },
            )
        }
    } else {
        PvotScreen(state = listState, contentPadding = screenPadding(padding, withFab = true)) {
            itemsIndexed(alarms, key = { _, alarm -> alarm.id }) { index, alarm ->
                AlarmCard(
                    modifier = Modifier.pvotReveal(index),
                    alarm = alarm,
                    version = version,
                    expanded = expanded == alarm.id,
                    onToggleExpanded = { expanded = if (expanded == alarm.id) -1L else alarm.id },
                    onEditTime = {
                        editingTime = alarm
                        adding = false
                        timeDialogOpen = true
                    },
                    updater = updater,
                )
            }
        }
    }

    val target = editingTime.takeUnless { adding }
    TimeDialog(
        visible = timeDialogOpen,
        initial =
            if (target != null) LocalTime.of(target.hour, target.minutes)
            else LocalTime.of(LocalTime.now().hour, 0),
        onDismiss = { timeDialogOpen = false },
        onConfirm = { time ->
            if (target != null) {
                target.hour = time.hour
                target.minutes = time.minute
                target.enabled = true
                updater.asyncUpdateAlarm(target, true, false)
            } else {
                updater.asyncAddAlarm(Alarm(time.hour, time.minute).apply { enabled = true })
            }
            timeDialogOpen = false
        },
    )
}

private val PROVIDER: Uri = "content://${BuildConfig.APPLICATION_ID}/".toUri()

private val ORDER = compareBy<Alarm>({ it.hour }, { it.minutes }, { -it.id })

private fun updater(context: Context): AlarmUpdateHandler {
    return AlarmUpdateHandler(
        context,
        object : ScrollHandler {
            override fun setSmoothScrollStableId(stableId: Long) {}

            override fun smoothScrollTo(position: Int) {}
        },
    )
}
