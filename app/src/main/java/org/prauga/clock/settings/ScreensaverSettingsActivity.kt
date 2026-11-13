/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.settings

import android.os.Bundle
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.clock.settings.ui.ScreensaverSettingsScreen

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
