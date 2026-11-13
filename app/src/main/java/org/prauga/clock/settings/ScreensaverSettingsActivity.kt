/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotPreferenceRow
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.components.slider.PvotSlider
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.util.Locale
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.ui.ChoiceSheet
import org.prauga.clock.ui.SubScreen

class ScreensaverSettingsActivity : PvotBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPvotContent { PvotAppTheme { ScreensaverSettingsScreen(onBack = ::finish) } }
    }

    companion object {
        const val KEY_CLOCK_STYLE = "screensaver_clock_style"
        const val KEY_CLOCK_COLOR = "screensaver_clock_color"
        const val KEY_NIGHT_MODE = "screensaver_night_mode"
        const val KEY_NIGHT_MODE_COLOR = "screensaver_clock_night_mode_color"
        const val KEY_NIGHT_MODE_DND = "screensaver_clock_night_mode_dnd"
        const val KEY_NIGHT_MODE_BRIGHTNESS = "screensaver_clock_night_mode_brightness"
        const val KEY_SHOW_AMPM = "screensaver_show_ampm"
        const val KEY_BOLD_TEXT = "screensaver_bold_text"
    }
}

@Composable
private fun ScreensaverSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { settingsPrefs(context) }
    val model = DataModel.getDataModel()
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    var open by remember { mutableStateOf<ListSetting?>(null) }
    var shown by remember { mutableStateOf<ListSetting?>(null) }

    val style =
        list(
            ScreensaverSettingsActivity.KEY_CLOCK_STYLE,
            R.string.clock_style,
            R.array.clock_style_entries,
            R.array.clock_style_values,
            stringResource(R.string.default_clock_style),
        )
    val color =
        list(
            ScreensaverSettingsActivity.KEY_CLOCK_COLOR,
            R.string.clock_color_title,
            R.array.clock_color_entries,
            R.array.clock_color_values,
            stringResource(R.string.default_clock_color),
        )
    val nightColor =
        list(
            ScreensaverSettingsActivity.KEY_NIGHT_MODE_COLOR,
            R.string.clock_color_title,
            R.array.clock_color_entries,
            R.array.clock_color_values,
            stringResource(R.string.default_clock_color),
        )

    version.let {}
    val styleValue = model.screensaverClockStyle.toString().lowercase(Locale.US)
    val values =
        mapOf(
            style.key to styleValue,
            color.key to model.screensaverClockColor,
            nightColor.key to model.screensaverClockNightModeColor,
        )
    fun entry(setting: ListSetting): String {
        val index = setting.values.indexOf(values[setting.key])
        return if (index >= 0) setting.entries[index] else ""
    }
    val digital = model.screensaverClockStyle == DataModel.ClockStyle.DIGITAL
    val night = model.screensaverNightModeOn
    val choose = { setting: ListSetting ->
        open = setting
        shown = setting
    }
    fun switch(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    SubScreen(stringResource(R.string.screensaver_settings), onBack) {
        item {
            PvotListGroup {
                item {
                    PvotPreferenceRow(
                        title = stringResource(style.title),
                        value = entry(style),
                        onClick = { choose(style) },
                    )
                }
                item {
                    PvotPreferenceRow(
                        title = stringResource(color.title),
                        value = entry(color),
                        onClick = { choose(color) },
                    )
                }
                if (digital) {
                    item {
                        PvotSwitchRow(
                            title = stringResource(R.string.bold_text_title),
                            checked = model.screensaverBoldTextOn,
                            onCheckedChange = {
                                switch(ScreensaverSettingsActivity.KEY_BOLD_TEXT, it)
                            },
                        )
                    }
                    item {
                        PvotSwitchRow(
                            title = stringResource(R.string.show_ampm_title),
                            checked = model.screensaverShowAmPmOn,
                            enabled = !DateFormat.is24HourFormat(context),
                            onCheckedChange = {
                                switch(ScreensaverSettingsActivity.KEY_SHOW_AMPM, it)
                            },
                        )
                    }
                }
            }
        }
        item {
            PvotListGroup(title = stringResource(R.string.night_mode_title)) {
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.night_mode_enabled_title),
                        summary = stringResource(R.string.night_mode_summary),
                        checked = night,
                        onCheckedChange = { switch(ScreensaverSettingsActivity.KEY_NIGHT_MODE, it) },
                    )
                }
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.night_mode_dnd_title),
                        summary = stringResource(R.string.night_mode_dnd_summary),
                        checked = model.screensaverNightModeDndOn,
                        enabled = night,
                        onCheckedChange = {
                            switch(ScreensaverSettingsActivity.KEY_NIGHT_MODE_DND, it)
                        },
                    )
                }
                item {
                    PvotPreferenceRow(
                        title = stringResource(nightColor.title),
                        value = entry(nightColor),
                        enabled = night,
                        onClick = { choose(nightColor) },
                    )
                }
                item { BrightnessRow(prefs, model.screensaverNightModeBrightness, night) }
            }
        }
    }

    val sheet = shown
    if (sheet != null) {
        ChoiceSheet(
            visible = open != null,
            title = stringResource(sheet.title),
            entries = sheet.entries,
            selected = sheet.values.indexOf(values[sheet.key]),
            onDismiss = { open = null },
            onSelect = { index ->
                prefs.edit().putString(sheet.key, sheet.values[index]).apply()
                open = null
            },
        )
    }
}

@Composable
private fun BrightnessRow(prefs: SharedPreferences, initial: Int, enabled: Boolean) {
    var percent by remember { mutableFloatStateOf(initial.toFloat()) }
    Column(
        Modifier.padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.M)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    ) {
        Text(
            stringResource(R.string.night_mode_brightness_text_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(R.string.clock_brightness_percentage, percent.toInt().toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PvotSlider(
            value = percent,
            onValueChange = { percent = it },
            valueRange = 0f..100f,
            enabled = enabled,
            onValueChangeFinished = {
                prefs
                    .edit()
                    .putInt(ScreensaverSettingsActivity.KEY_NIGHT_MODE_BRIGHTNESS, percent.toInt())
                    .apply()
            },
        )
    }
}

private const val DISABLED_ALPHA = 0.38f
