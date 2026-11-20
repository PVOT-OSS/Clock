/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotChoiceSheet
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotSubScreen
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListGroupScope
import com.prauga.pvot.designsystem.components.list.PvotPreferenceRow
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.components.slider.PvotSlider
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.ScreensaverActivity
import org.prauga.clock.Utils
import org.prauga.clock.alarms.AlarmGestures
import org.prauga.clock.alarms.Sunrise
import org.prauga.clock.data.DataModel
import org.prauga.clock.events.Events
import org.prauga.clock.ringtone.RingtonePickerActivity
import org.prauga.clock.settings.ScreensaverSettingsActivity
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_ALARM_CRESCENDO
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_ALARM_SNOOZE
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_AUTO_HOME_CLOCK
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_AUTO_SILENCE
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_CLOCK_DISPLAY_SECONDS
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_CLOCK_STYLE
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_FLIP_ACTION
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_HOME_TZ
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_SHAKE_ACTION
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_TIMER_CRESCENDO
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_TIMER_VIBRATE
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_VOLUME_BUTTONS
import org.prauga.clock.settings.SettingsActivity.Companion.KEY_WEEK_START
import org.prauga.clock.settings.data.AlarmVolume
import org.prauga.clock.settings.data.SettingsRepository

internal class ListSetting(
    val key: String,
    @StringRes val title: Int,
    val entries: List<String>,
    val values: List<String>,
    val default: String,
)

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = settingsViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(SettingsEvent.Refresh)
        onPauseOrDispose {}
    }
    val content = (state as? UiState.Success)?.data ?: return
    SettingsBody(content, onBack, viewModel::onEvent)
}

@Composable
internal fun settingsViewModel(): SettingsViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel { SettingsViewModel(SettingsRepository(context), DataModel.getDataModel()) }
}

@Composable
private fun SettingsBody(
    content: SettingsContent,
    onBack: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf<ListSetting?>(null) }
    var shown by remember { mutableStateOf<ListSetting?>(null) }
    var confirmOff by remember { mutableStateOf(false) }

    val hasMotion = content.hasMotionSensor
    val hasVibrator = content.hasVibrator
    val firstDay = content.firstDayOfWeek

    val clockStyle =
        list(
            KEY_CLOCK_STYLE,
            R.string.clock_style,
            R.array.clock_style_entries,
            R.array.clock_style_values,
            stringResource(R.string.default_clock_style),
        )
    val homeZone =
        ListSetting(
            KEY_HOME_TZ,
            R.string.home_time_zone,
            content.zoneNames,
            content.zoneIds,
            java.util.TimeZone.getDefault().id,
        )
    val autoSilence =
        list(
            KEY_AUTO_SILENCE,
            R.string.auto_silence_title,
            R.array.auto_silence_entries,
            R.array.auto_silence_values,
            "10",
        )
    val snooze =
        list(
            KEY_ALARM_SNOOZE,
            R.string.snooze_duration_title,
            R.array.snooze_duration_entries,
            R.array.snooze_duration_values,
            "10",
        )
    val alarmCrescendo =
        list(
            KEY_ALARM_CRESCENDO,
            R.string.crescendo_duration_title,
            R.array.crescendo_entries,
            R.array.crescendo_values,
            "0",
        )
    val sunrise =
        list(
            Sunrise.KEY_MINUTES,
            R.string.sunrise_title,
            R.array.sunrise_entries,
            R.array.sunrise_values,
            "0",
        )
    val volumeButtons =
        list(
            KEY_VOLUME_BUTTONS,
            R.string.volume_button_setting_title,
            R.array.volume_button_setting_entries,
            R.array.volume_button_setting_values,
            "0",
        )
    val dismissGesture =
        list(
            AlarmGestures.KEY_DISMISS_GESTURE,
            R.string.dismiss_gesture_title,
            R.array.dismiss_gesture_entries,
            R.array.dismiss_gesture_values,
            "swipe",
        )
    val flip =
        list(
            KEY_FLIP_ACTION,
            R.string.flip_action_title,
            R.array.action_setting_entries,
            R.array.volume_button_setting_values,
            "0",
        )
    val shake =
        list(
            KEY_SHAKE_ACTION,
            R.string.shake_action_title,
            R.array.action_setting_entries,
            R.array.volume_button_setting_values,
            "0",
        )
    val weekStart =
        list(
            KEY_WEEK_START,
            R.string.week_start_title,
            R.array.week_start_entries,
            R.array.week_start_values,
            firstDay,
        )
    val timerCrescendo =
        list(
            KEY_TIMER_CRESCENDO,
            R.string.crescendo_duration_title,
            R.array.crescendo_entries,
            R.array.crescendo_values,
            "0",
        )

    fun value(setting: ListSetting) = content.string(setting.key, setting.default)
    fun entry(setting: ListSetting): String {
        val index = setting.values.indexOf(value(setting))
        return if (index >= 0) setting.entries[index] else ""
    }
    fun ListSetting.row(
        scope: PvotListGroupScope,
        enabled: Boolean = true,
        summary: String? = null,
    ) =
        scope.item {
            PvotPreferenceRow(
                title = stringResource(title),
                value = summary ?: entry(this),
                enabled = enabled,
                onClick = {
                    open = this
                    shown = this
                },
            )
        }
    val autoHome = content.boolean(KEY_AUTO_HOME_CLOCK, true)

    PvotSubScreen(stringResource(R.string.settings), onBack) {
        item {
            PvotListGroup(title = stringResource(R.string.clock_settings)) {
                clockStyle.row(this)
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.display_clock_seconds_pref),
                        checked = content.boolean(KEY_CLOCK_DISPLAY_SECONDS, false),
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetBoolean(KEY_CLOCK_DISPLAY_SECONDS, it))
                        },
                    )
                }
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.automatic_home_clock),
                        summary = stringResource(R.string.automatic_home_clock_summary),
                        checked = autoHome,
                        onCheckedChange = {
                            onEvent(SettingsEvent.SetBoolean(KEY_AUTO_HOME_CLOCK, it))
                        },
                    )
                }
                // A zone the list doesn't name (an alias such as Asia/Calcutta) shows by name.
                homeZone.row(
                    this,
                    enabled = autoHome,
                    summary =
                        entry(homeZone).ifEmpty {
                            java.util.TimeZone.getTimeZone(value(homeZone)).displayName
                        },
                )
                item {
                    PvotPreferenceRow(
                        title = stringResource(R.string.open_date_settings),
                        onClick = { context.startActivity(Intent(Settings.ACTION_DATE_SETTINGS)) },
                    )
                }
            }
        }
        item {
            PvotListGroup(title = stringResource(R.string.screensaver_settings_title)) {
                item {
                    PvotPreferenceRow(
                        title = stringResource(R.string.screensaver_preview),
                        summary = stringResource(R.string.screensaver_preview_summary),
                        onClick = {
                            context.startActivity(
                                Intent(context, ScreensaverActivity::class.java)
                                    .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock)
                            )
                        },
                    )
                }
                item {
                    PvotPreferenceRow(
                        title = stringResource(R.string.screensaver_change_settings),
                        summary = stringResource(R.string.screensaver_change_settings_summary),
                        onClick = {
                            context.startActivity(
                                Intent(context, ScreensaverSettingsActivity::class.java)
                            )
                        },
                    )
                }
                item {
                    PvotPreferenceRow(
                        title = stringResource(R.string.screensaver_daydream_settings),
                        summary = stringResource(R.string.screensaver_daydream_settings_summary),
                        onClick = { context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS)) },
                    )
                }
            }
        }
        item {
            PvotListGroup(title = stringResource(R.string.alarm_settings)) {
                autoSilence.row(this, summary = autoSilenceText(context, value(autoSilence)))
                snooze.row(this)
                item { AlarmVolumeRow(content.alarmVolume, onEvent) }
                alarmCrescendo.row(this)
                sunrise.row(this)
                volumeButtons.row(this)
                dismissGesture.row(this)
                if (hasMotion) {
                    flip.row(this)
                    shake.row(this)
                }
                weekStart.row(this)
                item {
                    PvotPreferenceRow(
                        title = stringResource(R.string.turn_off_all_alarms),
                        onClick = { confirmOff = true },
                    )
                }
            }
        }
        item {
            PvotListGroup(title = stringResource(R.string.timer_settings)) {
                item {
                    PvotPreferenceRow(
                        title = stringResource(R.string.timer_sound),
                        value = content.timerRingtoneTitle,
                        onClick = {
                            context.startActivity(
                                RingtonePickerActivity.createTimerRingtonePickerIntent(context)
                            )
                        },
                    )
                }
                timerCrescendo.row(this)
                if (hasVibrator) {
                    item {
                        PvotSwitchRow(
                            title = stringResource(R.string.timer_vibrate_title),
                            checked = content.boolean(KEY_TIMER_VIBRATE, false),
                            onCheckedChange = {
                                onEvent(SettingsEvent.SetBoolean(KEY_TIMER_VIBRATE, it))
                            },
                        )
                    }
                }
            }
        }
    }

    val sheet = shown
    if (sheet != null) {
        PvotChoiceSheet(
            visible = open != null,
            title = stringResource(sheet.title),
            entries = sheet.entries,
            selected = sheet.values.indexOf(value(sheet)),
            onDismiss = { open = null },
            onSelect = { index ->
                onEvent(SettingsEvent.SetString(sheet.key, sheet.values[index]))
                open = null
            },
        )
    }
    PvotDialog(
        visible = confirmOff,
        onDismissRequest = { confirmOff = false },
        title = stringResource(R.string.turn_off_all_alarms),
        message = stringResource(R.string.turn_off_all_alarms_message),
        actions = {
            PvotDialogAction(
                label = stringResource(R.string.turn_off_all_alarms_confirm),
                onClick = {
                    onEvent(SettingsEvent.TurnOffAllAlarms)
                    confirmOff = false
                },
                style = PvotDialogActionStyle.Destructive,
            )
            PvotDialogAction(
                label = stringResource(android.R.string.cancel),
                onClick = { confirmOff = false },
            )
        },
    )
}

@Composable
internal fun list(
    key: String,
    @StringRes title: Int,
    @ArrayRes entries: Int,
    @ArrayRes values: Int,
    default: String,
): ListSetting {
    val entryList = stringArrayResource(entries).toList()
    val valueList = stringArrayResource(values).toList()
    return remember(key, entryList, valueList, default) {
        ListSetting(key, title, entryList, valueList, default)
    }
}

private fun autoSilenceText(context: Context, value: String): String {
    val minutes = value.toIntOrNull() ?: return ""
    return if (minutes == -1) context.getString(R.string.auto_silence_never)
    else Utils.getNumberFormattedQuantityString(context, R.plurals.auto_silence_summary, minutes)
}

@Composable
private fun AlarmVolumeRow(volume: AlarmVolume, onEvent: (SettingsEvent) -> Unit) {
    var dragged by remember(volume.current) { mutableFloatStateOf(volume.current.toFloat()) }
    Column(Modifier.padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.M)) {
        Text(
            stringResource(R.string.alarm_volume_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_t_bell),
                contentDescription = null,
                modifier =
                    Modifier.padding(end = PvotSpacing.M).size(PvotIconButtonDefaults.IconSize),
            )
            // Do Not Disturb with total silence keeps alarms quiet, so the volume can't be set
            // then.
            PvotSlider(
                value = dragged,
                onValueChange = {
                    dragged = it
                    onEvent(SettingsEvent.SetAlarmVolume(it.toInt()))
                },
                valueRange = volume.min.toFloat()..volume.max.toFloat(),
                steps = (volume.max - volume.min - 1).coerceAtLeast(0),
                enabled = volume.adjustable,
                onValueChangeFinished = { onEvent(SettingsEvent.PreviewAlarmVolume) },
            )
        }
    }
}
