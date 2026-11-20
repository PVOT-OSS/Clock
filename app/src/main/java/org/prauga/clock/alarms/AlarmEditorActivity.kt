/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import org.prauga.clock.alarms.ui.AlarmEditorScreen
import org.prauga.clock.provider.Alarm

class AlarmEditorActivity : PvotBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, Alarm.INVALID_ID)
        setPvotContent { PvotAppTheme { AlarmEditorScreen(alarmId, onClose = ::finish) } }
    }

    companion object {
        private const val EXTRA_ALARM_ID = "org.prauga.clock.extra.ALARM_ID"

        fun intent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmEditorActivity::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
    }
}
