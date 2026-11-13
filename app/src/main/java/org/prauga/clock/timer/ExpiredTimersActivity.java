/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.prauga.clock.timer;

import android.app.KeyguardManager;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import org.prauga.clock.BaseActivity;
import org.prauga.clock.LogUtils;
import org.prauga.clock.R;
import org.prauga.clock.data.DataModel;
import org.prauga.clock.timer.ui.ExpiredTimers;

/**
 * This activity is designed to be shown over the lock screen. As such, it displays the expired
 * timers and a single button to reset them all. Each expired timer can also be reset to one minute
 * with a button in the user interface. All other timer operations are disabled in this activity.
 */
public class ExpiredTimersActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DataModel.getDataModel().getExpiredTimers().isEmpty()) {
            LogUtils.i("No expired timers, skipping display.");
            finish();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        setTurnScreenOn(true);
        setShowWhenLocked(true);
        KeyguardManager kgm = getSystemService(KeyguardManager.class);
        kgm.requestDismissKeyguard(this, null);

        // Honor rotation on tablets; fix the orientation on phones.
        if (!getResources().getBoolean(R.bool.rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        ExpiredTimers.show(this);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                case KeyEvent.KEYCODE_VOLUME_MUTE:
                case KeyEvent.KEYCODE_CAMERA:
                case KeyEvent.KEYCODE_FOCUS:
                    DataModel.getDataModel().resetOrDeleteExpiredTimers(
                            R.string.label_hardware_button);
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
