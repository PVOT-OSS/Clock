/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2022-2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.prauga.clock;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.prauga.clock.alarmclock.StopwatchAppWidgetProvider;
import org.prauga.clock.controller.Controller;
import org.prauga.clock.data.DataModel;
import org.prauga.clock.events.LogEventTracker;
import org.prauga.clock.uidata.UiDataModel;

public class DeskClockApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final Context applicationContext = getApplicationContext();
        final SharedPreferences prefs = getDefaultSharedPreferences(applicationContext);

        DataModel.getDataModel().init(applicationContext, prefs);
        UiDataModel.getUiDataModel().init(applicationContext, prefs);
        StopwatchAppWidgetProvider.observe(applicationContext);
        Controller.getController().setContext(applicationContext);
        Controller.getController().addEventTracker(new LogEventTracker(applicationContext));
    }

    /**
     * Returns the default {@link SharedPreferences} instance from the underlying storage context.
     */
    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        final Context storageContext = context.createDeviceProtectedStorageContext();
        return storageContext.getSharedPreferences(
                PreferenceManager.getDefaultSharedPreferencesName(storageContext),
                Context.MODE_PRIVATE);
    }
}
