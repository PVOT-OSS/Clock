/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import android.os.Bundle
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.clock.data.DataModel

/** The Compose app on PvotLib: five tabs in a floating navigation bar. */
class ClockActivity : PvotBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPvotContent { PvotAppTheme(dynamicColor = true) { ClockApp() } }
    }

    override fun onStart() {
        // Timer and stopwatch notifications hide while the app is open.
        DataModel.getDataModel().setApplicationInForeground(true)
        super.onStart()
    }

    override fun onStop() {
        if (!isChangingConfigurations) {
            DataModel.getDataModel().setApplicationInForeground(false)
        }
        super.onStop()
    }
}
