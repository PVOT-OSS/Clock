/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotPreferenceRow
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.components.slider.PvotSlider
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.common.ChoiceSheet
import org.prauga.clock.common.SubScreen
import org.prauga.clock.data.DataModel
import org.prauga.clock.settings.ScreensaverSettingsActivity.Companion.KEY_BOLD_TEXT
import org.prauga.clock.settings.ScreensaverSettingsActivity.Companion.KEY_CLOCK_COLOR
import org.prauga.clock.settings.ScreensaverSettingsActivity.Companion.KEY_CLOCK_STYLE
import org.prauga.clock.settings.ScreensaverSettingsActivity.Companion.KEY_NIGHT_MODE
import org.prauga.clock.settings.ScreensaverSettingsActivity.Companion.KEY_NIGHT_MODE_COLOR
import org.prauga.clock.settings.ScreensaverSettingsActivity.Companion.KEY_NIGHT_MODE_DND
import org.prauga.clock.settings.ScreensaverSettingsActivity.Companion.KEY_SHOW_AMPM
import org.prauga.clock.settings.data.SettingsRepository

@Composable
fun ScreensaverSettingsScreen(
    onBack: () -> Unit,
    viewModel: ScreensaverSettingsViewModel = screensaverSettingsViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = (state as? UiState.Success)?.data ?: return
    ScreensaverSettingsBody(content, onBack, viewModel::onEvent)
}

@Composable
private fun screensaverSettingsViewModel(): ScreensaverSettingsViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel {
        ScreensaverSettingsViewModel(SettingsRepository(context), DataModel.getDataModel())
    }
}

@Composable
private fun ScreensaverSettingsBody(
    content: ScreensaverContent,
    onBack: () -> Unit,
    onEvent: (ScreensaverEvent) -> Unit,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf<ListSetting?>(null) }
    var shown by remember { mutableStateOf<ListSetting?>(null) }
    val style =
        list(
            KEY_CLOCK_STYLE,
            R.string.clock_style,
            R.array.clock_style_entries,
            R.array.clock_style_values,
            stringResource(R.string.default_clock_style),
        )
    val color =
        list(
            KEY_CLOCK_COLOR,
            R.string.clock_color_title,
            R.array.clock_color_entries,
            R.array.clock_color_values,
            stringResource(R.string.default_clock_color),
        )
    val nightColor =
        list(
            KEY_NIGHT_MODE_COLOR,
            R.string.clock_color_title,
            R.array.clock_color_entries,
            R.array.clock_color_values,
            stringResource(R.string.default_clock_color),
        )
    val values =
        mapOf(
            style.key to content.style,
            color.key to content.color,
            nightColor.key to content.nightModeColor,
        )
    fun entry(setting: ListSetting): String {
        val index = setting.values.indexOf(values[setting.key])
        return if (index >= 0) setting.entries[index] else ""
    }
    val choose = { setting: ListSetting ->
        open = setting
        shown = setting
    }
    val switch = { key: String, value: Boolean -> onEvent(ScreensaverEvent.SetBoolean(key, value)) }

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
                if (content.digital) {
                    item {
                        PvotSwitchRow(
                            title = stringResource(R.string.bold_text_title),
                            checked = content.boldText,
                            onCheckedChange = { switch(KEY_BOLD_TEXT, it) },
                        )
                    }
                    item {
                        PvotSwitchRow(
                            title = stringResource(R.string.show_ampm_title),
                            checked = content.showAmPm,
                            enabled = !DateFormat.is24HourFormat(context),
                            onCheckedChange = { switch(KEY_SHOW_AMPM, it) },
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
                        checked = content.nightMode,
                        onCheckedChange = { switch(KEY_NIGHT_MODE, it) },
                    )
                }
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.night_mode_dnd_title),
                        summary = stringResource(R.string.night_mode_dnd_summary),
                        checked = content.nightModeDnd,
                        enabled = content.nightMode,
                        onCheckedChange = { switch(KEY_NIGHT_MODE_DND, it) },
                    )
                }
                item {
                    PvotPreferenceRow(
                        title = stringResource(nightColor.title),
                        value = entry(nightColor),
                        enabled = content.nightMode,
                        onClick = { choose(nightColor) },
                    )
                }
                item {
                    BrightnessRow(content.nightModeBrightness, content.nightMode) {
                        onEvent(ScreensaverEvent.SetBrightness(it))
                    }
                }
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
                onEvent(ScreensaverEvent.SetString(sheet.key, sheet.values[index]))
                open = null
            },
        )
    }
}

@Composable
private fun BrightnessRow(initial: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    var percent by remember(initial) { mutableFloatStateOf(initial.toFloat()) }
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
            onValueChangeFinished = { onChange(percent.toInt()) },
        )
    }
}

private const val DISABLED_ALPHA = 0.38f
