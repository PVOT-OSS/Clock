/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings

import android.os.Bundle
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.clock.settings.ui.SettingsScreen

class SettingsActivity : PvotBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPvotContent { PvotAppTheme { SettingsScreen(onBack = ::finish) } }
    }

    companion object {
        const val KEY_ALARM_SNOOZE = "snooze_duration"
        const val KEY_ALARM_CRESCENDO = "alarm_crescendo_duration"
        const val KEY_TIMER_CRESCENDO = "timer_crescendo_duration"
        const val KEY_TIMER_RINGTONE = "timer_ringtone"
        const val KEY_TIMER_VIBRATE = "timer_vibrate"
        const val KEY_AUTO_SILENCE = "auto_silence"
        const val KEY_CLOCK_STYLE = "clock_style"
        const val KEY_CLOCK_DISPLAY_SECONDS = "display_clock_seconds"
        const val KEY_HOME_TZ = "home_time_zone"
        const val KEY_AUTO_HOME_CLOCK = "automatic_home_clock"
        const val KEY_VOLUME_BUTTONS = "volume_button_setting"
        const val KEY_WEEK_START = "week_start"
        const val KEY_FLIP_ACTION = "flip_action"
        const val KEY_SHAKE_ACTION = "shake_action"

        const val DEFAULT_VOLUME_BEHAVIOR = "0"
        const val VOLUME_BEHAVIOR_SNOOZE = "1"
        const val VOLUME_BEHAVIOR_DISMISS = "2"
    }
}
