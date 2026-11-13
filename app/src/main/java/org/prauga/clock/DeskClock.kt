/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.prauga.clock.ui.ClockActivity

/** The classic entry point, kept for intents and shortcuts addressed to it: forwards them on. */
class DeskClock : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(intent).setClass(this, ClockActivity::class.java))
        finish()
    }
}
