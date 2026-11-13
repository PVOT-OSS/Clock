/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock

import android.os.Bundle
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.clock.worldclock.ui.CitySelectionScreen

class CitySelectionActivity : PvotBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPvotContent { PvotAppTheme { CitySelectionScreen(onBack = ::finish) } }
    }
}
