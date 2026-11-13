/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.PvotSectionHeader
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotSheetItem
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlinx.coroutines.delay
import org.prauga.clock.AnalogClock
import org.prauga.clock.R
import org.prauga.clock.Utils
import org.prauga.clock.common.ScreenFab
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.City
import org.prauga.clock.data.DataModel
import org.prauga.clock.worldclock.CitySelectionActivity

@Composable
fun ClockScreen(
    padding: PaddingValues,
    listState: LazyListState,
    viewModel: ClockViewModel = viewModel { ClockViewModel(DataModel.getDataModel()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = (state as? UiState.Success)?.data ?: return
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(ClockEvent.Refresh)
        onPauseOrDispose {}
    }
    ClockBody(content, padding, listState, viewModel::onEvent)
}

@Composable
private fun ClockBody(
    content: ClockContent,
    padding: PaddingValues,
    listState: LazyListState,
    onEvent: (ClockEvent) -> Unit,
) {
    val context = LocalContext.current
    val seconds = content.seconds
    val now by rememberNow(if (seconds) DateUtils.SECOND_IN_MILLIS else DateUtils.MINUTE_IN_MILLIS)
    val cities = content.clocks
    val haptics = LocalHapticFeedback.current
    var menuCity by remember { mutableStateOf<City?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    ScreenFab(R.drawable.ic_t_world, R.string.add_city) {
        context.startActivity(Intent(context, CitySelectionActivity::class.java))
    }

    PvotScreen(
        state = listState,
        contentPadding = screenPadding(padding, withFab = true),
        horizontalAlignment = Alignment.Start,
    ) {
        item { MainClock(context, now, seconds, content.analog) }
        if (cities.isNotEmpty()) {
            item(key = "cities") {
                PvotSectionHeader(
                    stringResource(R.string.cities_activity_title),
                    Modifier.padding(top = PvotSpacing.L),
                )
            }
        }
        itemsIndexed(cities, key = { _, city -> city.id }) { index, city ->
            CityCard(
                context,
                city,
                now,
                Modifier.pvotReveal(index).pointerInput(city) {
                    detectTapGestures(
                        onLongPress = {
                            // The home clock follows the time zone, so only chosen cities move.
                            if (city in content.selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuCity = city
                                menuOpen = true
                            }
                        }
                    )
                },
            )
        }
    }

    val shown = menuCity
    if (shown != null) {
        val selected = content.selected
        val index = selected.indexOf(shown)
        PvotBottomSheet(
            visible = menuOpen,
            onDismissRequest = { menuOpen = false },
            title = shown.name,
        ) {
            PvotSheetItem(
                title = stringResource(R.string.city_move_up),
                enabled = index > 0,
                onClick = {
                    onEvent(ClockEvent.Move(shown, -1))
                    menuOpen = false
                },
            )
            PvotSheetItem(
                title = stringResource(R.string.city_move_down),
                enabled = index in 0 until selected.lastIndex,
                onClick = {
                    onEvent(ClockEvent.Move(shown, 1))
                    menuOpen = false
                },
            )
            PvotSheetItem(
                title = stringResource(R.string.city_remove),
                destructive = true,
                onClick = {
                    onEvent(ClockEvent.Remove(shown))
                    menuOpen = false
                },
            )
        }
    }
}

@Composable
private fun rememberNow(step: Long): androidx.compose.runtime.State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(step) {
        while (true) {
            val time = System.currentTimeMillis()
            now.longValue = time
            delay(step - time % step)
        }
    }
    return now
}

@Composable
private fun MainClock(context: Context, now: Long, seconds: Boolean, analog: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = PvotSpacing.L)) {
        if (analog) {
            AndroidView(
                factory = { AnalogClock(it) },
                update = { it.enableSeconds(seconds) },
                modifier =
                    Modifier.fillMaxWidth(ANALOG_FRACTION)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally),
            )
        } else {
            Text(
                text = formatTime(context, now, TimeZone.getDefault(), seconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
            )
        }
        Text(
            text =
                DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), now)
                    .toString()
                    .uppercase(),
            style = MaterialTheme.typography.titleSmall,
        )
        Utils.getNextAlarm(context)?.let { alarm ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = PvotSpacing.XS),
            ) {
                Icon(
                    painterResource(R.drawable.ic_t_alarm),
                    contentDescription = null,
                    modifier = Modifier.size(PvotIconButtonDefaults.SmallIconSize),
                )
                Spacer(Modifier.width(PvotSpacing.XS))
                Text(alarm, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CityCard(context: Context, city: City, now: Long, modifier: Modifier = Modifier) {
    PvotCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(PvotSpacing.L),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(city.name, style = MaterialTheme.typography.titleMedium)
                difference(context, city, now)?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                text = formatTime(context, now, city.timeZone, false),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
            )
        }
    }
}

private fun formatTime(context: Context, now: Long, zone: TimeZone, seconds: Boolean): String {
    val skeleton =
        when {
            DateFormat.is24HourFormat(context) -> if (seconds) "Hms" else "Hm"
            else -> if (seconds) "hmsa" else "hma"
        }
    val format =
        android.icu.text.SimpleDateFormat(
            DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), skeleton),
            java.util.Locale.getDefault(),
        )
    format.timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
    return format.format(Date(now))
}

private fun difference(context: Context, city: City, now: Long): String? {
    val delta = city.timeZone.getOffset(now) - TimeZone.getDefault().getOffset(now).toLong()
    val hours = (delta / DateUtils.HOUR_IN_MILLIS).toInt()
    val minutes = (delta / DateUtils.MINUTE_IN_MILLIS).toInt() % 60
    val showMinutes = delta % DateUtils.HOUR_IN_MILLIS != 0L
    if (hours == 0 && !showMinutes) {
        return null
    }
    val ahead = hours > 0 || (hours == 0 && minutes > 0)
    val text = Utils.createHoursDifferentString(context, showMinutes, ahead, hours, minutes)
    val local = Calendar.getInstance()
    val there = Calendar.getInstance(city.timeZone)
    return if (local.get(Calendar.DAY_OF_WEEK) != there.get(Calendar.DAY_OF_WEEK)) {
        context.getString(
            if (ahead) R.string.world_hours_tomorrow else R.string.world_hours_yesterday,
            text,
        )
    } else text
}

private const val ANALOG_FRACTION = 0.6f
