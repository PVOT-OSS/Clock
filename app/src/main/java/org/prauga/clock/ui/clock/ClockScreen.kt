/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui.clock

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.prauga.pvot.designsystem.components.PvotCard
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlinx.coroutines.delay
import org.prauga.clock.AnalogClock
import org.prauga.clock.R
import org.prauga.clock.Utils
import org.prauga.clock.data.City
import org.prauga.clock.data.CityListener
import org.prauga.clock.data.DataModel

/** The Clock tab: the time here, then the world clocks. */
@Composable
fun ClockScreen(padding: PaddingValues, listState: LazyListState) {
    val context = LocalContext.current
    val model = DataModel.getDataModel()
    val seconds = model.displayClockSeconds
    val now by rememberNow(if (seconds) DateUtils.SECOND_IN_MILLIS else DateUtils.MINUTE_IN_MILLIS)
    var cities by remember { mutableStateOf(worldClocks()) }
    DisposableEffect(Unit) {
        val listener = CityListener { cities = worldClocks() }
        model.addCityListener(listener)
        onDispose { model.removeCityListener(listener) }
    }

    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 160.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { MainClock(context, now, seconds) }
        items(cities, key = { it.id }) { city -> CityCard(context, city, now) }
    }
}

private fun worldClocks(): List<City> {
    val model = DataModel.getDataModel()
    val home = model.homeCity.takeIf { model.showHomeClock }
    return listOfNotNull(home) + model.selectedCities
}

/** The current time in milliseconds, updated on each [step] boundary. */
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
private fun MainClock(context: Context, now: Long, seconds: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val model = DataModel.getDataModel()
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        if (model.clockStyle == DataModel.ClockStyle.ANALOG) {
            AndroidView(
                factory = { AnalogClock(it) },
                update = { it.enableSeconds(seconds) },
                modifier = Modifier.size(240.dp).align(Alignment.CenterHorizontally),
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
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_t_alarm),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(alarm, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun CityCard(context: Context, city: City, now: Long) {
    PvotCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), skeleton)
        )
    format.timeZone = android.icu.util.TimeZone.getTimeZone(zone.id)
    return format.format(Date(now))
}

/** "5 hr 30 min behind", with "Yesterday"/"Tomorrow" when the day differs; null when equal. */
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
