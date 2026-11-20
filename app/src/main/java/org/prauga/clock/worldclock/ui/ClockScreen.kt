/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.adaptive.PvotTabletopSplit
import com.prauga.pvot.designsystem.adaptive.PvotTwoPane
import com.prauga.pvot.designsystem.adaptive.rememberPvotWindowLayout
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotSheetItem
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlinx.coroutines.delay
import org.prauga.clock.AnalogClock
import org.prauga.clock.R
import org.prauga.clock.Utils
import org.prauga.clock.common.ScreenAction
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.City
import org.prauga.clock.data.DataModel
import org.prauga.clock.worldclock.CitySelectionActivity

@Composable
fun ClockScreen(padding: PaddingValues, listState: LazyListState) {
    val context = LocalContext.current.applicationContext
    val viewModel: ClockViewModel = viewModel {
        ClockViewModel(DataModel.getDataModel(), { Utils.getNextAlarm(context) })
    }
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
    val haptics = LocalHapticFeedback.current
    var menuCityId by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    ScreenAction(R.drawable.ic_t_world, R.string.add_city) {
        context.startActivity(Intent(context, CitySelectionActivity::class.java))
    }
    val onLongPress: (City) -> Unit = { city ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        menuCityId = city.id
        menuOpen = true
    }

    val layout = rememberPvotWindowLayout()
    when {
        layout.tabletop ->
            PvotTabletopSplit(
                top = { MainClockPane(content, now, abovePanePadding(padding)) },
                bottom = {
                    CityList(content, now, listState, belowPanePadding(padding), onLongPress)
                },
                modifier = Modifier.fillMaxSize(),
            )
        layout.twoPane && content.clocks.isEmpty() ->
            MainClockPane(content, now, screenPadding(padding))
        layout.twoPane ->
            PvotTwoPane(
                first = { MainClockPane(content, now, screenPadding(padding)) },
                second = { CityList(content, now, listState, screenPadding(padding), onLongPress) },
                modifier = Modifier.fillMaxSize(),
            )
        else ->
            PvotScreen(state = listState, contentPadding = screenPadding(padding)) {
                item(key = "clock") { MainClock(content, now) }
                if (content.clocks.isNotEmpty()) {
                    item(key = "cities") {
                        CityGroup(
                            content,
                            content.clocks,
                            now,
                            onLongPress,
                            Modifier.padding(top = PvotSpacing.L),
                        )
                    }
                }
            }
    }

    val selected = content.selected
    val shown = selected.firstOrNull { it.id == menuCityId }
    if (shown != null) {
        CityMenu(
            city = shown,
            index = selected.indexOf(shown),
            last = selected.lastIndex,
            visible = menuOpen,
            onDismiss = { menuOpen = false },
            onEvent = {
                onEvent(it)
                menuOpen = false
            },
        )
    }
}

@Composable
private fun CityMenu(
    city: City,
    index: Int,
    last: Int,
    visible: Boolean,
    onDismiss: () -> Unit,
    onEvent: (ClockEvent) -> Unit,
) {
    PvotBottomSheet(visible = visible, onDismissRequest = onDismiss, title = city.name) {
        PvotSheetItem(
            title = stringResource(R.string.city_move_up),
            enabled = index > 0,
            onClick = { onEvent(ClockEvent.Move(city, -1)) },
        )
        PvotSheetItem(
            title = stringResource(R.string.city_move_down),
            enabled = index in 0 until last,
            onClick = { onEvent(ClockEvent.Move(city, 1)) },
        )
        PvotSheetItem(
            title = stringResource(R.string.city_remove),
            destructive = true,
            onClick = { onEvent(ClockEvent.Remove(city)) },
        )
    }
}

@Composable
private fun rememberNow(step: Long): State<Long> {
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
private fun MainClockPane(content: ClockContent, now: Long, padding: PaddingValues) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        MainClock(
            content = content,
            now = now,
            large = maxWidth >= LargeClockMinWidth,
            dialSize =
                min(maxWidth * PANE_DIAL_WIDTH_FRACTION, maxHeight * PANE_DIAL_HEIGHT_FRACTION),
        )
    }
}

@Composable
private fun MainClock(
    content: ClockContent,
    now: Long,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    dialSize: Dp? = null,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = PvotSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (content.analog) {
            AndroidView(
                factory = { AnalogClock(it) },
                update = { it.enableSeconds(content.seconds) },
                modifier =
                    if (dialSize != null) Modifier.size(dialSize)
                    else Modifier.fillMaxWidth(ANALOG_FRACTION).aspectRatio(1f),
            )
        } else {
            val style = MaterialTheme.typography.displayLarge
            Text(
                text = formatTime(context, now, TimeZone.getDefault(), content.seconds),
                style =
                    if (large) style.copy(fontSize = style.fontSize * LARGE_CLOCK_SCALE) else style,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text =
                DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), now)
                    .toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content.nextAlarm?.let { alarm ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = PvotSpacing.XS),
            ) {
                Icon(
                    painterResource(R.drawable.ic_t_alarm),
                    contentDescription = null,
                    modifier = Modifier.size(PvotIconButtonDefaults.SmallIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(PvotSpacing.XS))
                Text(
                    alarm,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CityList(
    content: ClockContent,
    now: Long,
    listState: LazyListState,
    padding: PaddingValues,
    onLongPress: (City) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val direction = LocalLayoutDirection.current
        val inner =
            maxWidth -
                padding.calculateStartPadding(direction) -
                padding.calculateEndPadding(direction)
        val columns = (inner / CityCellMinWidth).toInt().coerceAtLeast(1)
        PvotScreen(state = listState, contentPadding = padding) {
            if (columns == 1) {
                item(key = "cities") { CityGroup(content, content.clocks, now, onLongPress) }
            } else {
                items(content.clocks.chunked(columns), key = { it.first().id }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M)) {
                        row.forEach { city ->
                            CityGroup(content, listOf(city), now, onLongPress, Modifier.weight(1f))
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityGroup(
    content: ClockContent,
    cities: List<City>,
    now: Long,
    onLongPress: (City) -> Unit,
    modifier: Modifier = Modifier,
) {
    PvotListGroup(modifier = modifier) {
        cities.forEach { city ->
            // The home clock follows the time zone, so only chosen cities move.
            item { CityRow(city, now, if (city in content.selected) onLongPress else null) }
        }
    }
}

private fun abovePanePadding(scaffold: PaddingValues): PaddingValues =
    PaddingValues(start = PvotSpacing.L, top = scaffold.calculateTopPadding(), end = PvotSpacing.L)

@Composable
private fun belowPanePadding(scaffold: PaddingValues): PaddingValues {
    val full = screenPadding(scaffold)
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = full.calculateStartPadding(direction),
        top = PvotSpacing.L,
        end = full.calculateEndPadding(direction),
        bottom = full.calculateBottomPadding(),
    )
}

@Composable
private fun CityRow(city: City, now: Long, onLongPress: ((City) -> Unit)?) {
    val context = LocalContext.current
    PvotListRow(
        title = city.name,
        supportingText = difference(context, city, now),
        trailing = { CityTime(context, city, now) },
        onLongClick = onLongPress?.let { { it(city) } },
    )
}

@Composable
private fun CityTime(context: Context, city: City, now: Long) {
    val day = isDaytime(city.timeZone, now)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(if (day) R.drawable.ic_t_sun else R.drawable.ic_t_moon),
            contentDescription =
                stringResource(if (day) R.string.world_daytime else R.string.world_night),
            modifier = Modifier.size(PvotIconButtonDefaults.SmallIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(PvotSpacing.S))
        Text(
            text = formatTime(context, now, city.timeZone, false),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun isDaytime(zone: TimeZone, now: Long): Boolean {
    val hour = Calendar.getInstance(zone).apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    return hour in DAY_START_HOUR until DAY_END_HOUR
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

private fun difference(context: Context, city: City, now: Long): String {
    val delta = city.timeZone.getOffset(now) - TimeZone.getDefault().getOffset(now).toLong()
    val hours = (delta / DateUtils.HOUR_IN_MILLIS).toInt()
    val minutes = (delta / DateUtils.MINUTE_IN_MILLIS).toInt() % 60
    val showMinutes = delta % DateUtils.HOUR_IN_MILLIS != 0L
    if (hours == 0 && !showMinutes) {
        return context.getString(R.string.world_same_time)
    }
    val ahead = hours > 0 || (hours == 0 && minutes > 0)
    val text = Utils.createHoursDifferentString(context, showMinutes, ahead, hours, minutes)
    val local = Calendar.getInstance().apply { timeInMillis = now }
    val there = Calendar.getInstance(city.timeZone).apply { timeInMillis = now }
    return if (local.get(Calendar.DAY_OF_WEEK) != there.get(Calendar.DAY_OF_WEEK)) {
        context.getString(
            if (ahead) R.string.world_hours_tomorrow else R.string.world_hours_yesterday,
            text,
        )
    } else text
}

private const val ANALOG_FRACTION = 0.6f
private const val PANE_DIAL_WIDTH_FRACTION = 0.8f
private const val PANE_DIAL_HEIGHT_FRACTION = 0.6f
private const val LARGE_CLOCK_SCALE = 1.5f
private val LargeClockMinWidth = 400.dp
private val CityCellMinWidth = 320.dp
private const val DAY_START_HOUR = 6
private const val DAY_END_HOUR = 18
