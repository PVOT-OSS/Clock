/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.prauga.clock.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.loader.content.CursorLoader;

import org.prauga.clock.R;
import org.prauga.clock.data.DataModel;
import org.prauga.clock.data.Weekdays;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public final class Alarm implements Parcelable, ClockContract.AlarmsColumns {
    /**
     * Alarms start with an invalid id when it hasn't been saved to the database.
     */
    public static final long INVALID_ID = -1;

    public static final int NO_COLOR = -1;
    public static final long NO_GROUP = -1;

    /**
     * The default sort order for this table
     */
    private static final String DEFAULT_SORT_ORDER =
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR + ", " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." +  MINUTES + " ASC" + ", " +
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ClockContract.AlarmsColumns._ID + " DESC";

    private static final String[] QUERY_COLUMNS = {
            _ID,
            HOUR,
            MINUTES,
            DAYS_OF_WEEK,
            ENABLED,
            VIBRATE,
            LABEL,
            RINGTONE,
            DELETE_AFTER_USE,
            INCREASING_VOLUME,
            SCHEDULED_DATE,
            PAUSE_START,
            PAUSE_END,
            COLOR,
            GROUP_ID,
    };

    private static final String[] QUERY_ALARMS_WITH_INSTANCES_COLUMNS = {
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + _ID,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + HOUR,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + MINUTES,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + DAYS_OF_WEEK,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + ENABLED,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + VIBRATE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + LABEL,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + RINGTONE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + DELETE_AFTER_USE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + INCREASING_VOLUME,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "."
                    + ClockContract.InstancesColumns.ALARM_STATE,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns._ID,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.YEAR,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MONTH,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.DAY,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.HOUR,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.MINUTES,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.LABEL,
            ClockDatabaseHelper.INSTANCES_TABLE_NAME + "." + ClockContract.InstancesColumns.VIBRATE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + SCHEDULED_DATE,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + PAUSE_START,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + PAUSE_END,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + COLOR,
            ClockDatabaseHelper.ALARMS_TABLE_NAME + "." + GROUP_ID,
    };

    /**
     * These save calls to cursor.getColumnIndexOrThrow()
     * THEY MUST BE KEPT IN SYNC WITH ABOVE QUERY COLUMNS
     */
    private static final int ID_INDEX = 0;
    private static final int HOUR_INDEX = 1;
    private static final int MINUTES_INDEX = 2;
    private static final int DAYS_OF_WEEK_INDEX = 3;
    private static final int ENABLED_INDEX = 4;
    private static final int VIBRATE_INDEX = 5;
    private static final int LABEL_INDEX = 6;
    private static final int RINGTONE_INDEX = 7;
    private static final int DELETE_AFTER_USE_INDEX = 8;
    private static final int INCREASING_VOLUME_INDEX = 9;

    public static final int INSTANCE_STATE_INDEX = 10;
    public static final int INSTANCE_ID_INDEX = 11;
    public static final int INSTANCE_YEAR_INDEX = 12;
    public static final int INSTANCE_MONTH_INDEX = 13;
    public static final int INSTANCE_DAY_INDEX = 14;
    public static final int INSTANCE_HOUR_INDEX = 15;
    public static final int INSTANCE_MINUTE_INDEX = 16;
    public static final int INSTANCE_LABEL_INDEX = 17;
    public static final int INSTANCE_VIBRATE_INDEX = 18;

    private static final int COLUMN_COUNT = QUERY_COLUMNS.length;
    private static final int ALARM_JOIN_INSTANCE_COLUMN_COUNT =
            QUERY_ALARMS_WITH_INSTANCES_COLUMNS.length;

    public static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(COLUMN_COUNT);
        if (alarm.id != INVALID_ID) {
            values.put(ClockContract.AlarmsColumns._ID, alarm.id);
        }

        values.put(ENABLED, alarm.enabled ? 1 : 0);
        values.put(HOUR, alarm.hour);
        values.put(MINUTES, alarm.minutes);
        values.put(DAYS_OF_WEEK, alarm.daysOfWeek.getBits());
        values.put(VIBRATE, alarm.vibrate ? 1 : 0);
        values.put(LABEL, alarm.label);
        values.put(DELETE_AFTER_USE, alarm.deleteAfterUse);
        values.put(INCREASING_VOLUME, alarm.increasingVolume ? 1 : 0);
        values.put(SCHEDULED_DATE, alarm.scheduledDate);
        values.put(PAUSE_START, alarm.pauseStart);
        values.put(PAUSE_END, alarm.pauseEnd);
        values.put(COLOR, alarm.color);
        values.put(GROUP_ID, alarm.groupId);
        if (alarm.alert == null) {
            // We want to put null, so default alarm changes
            values.putNull(RINGTONE);
        } else {
            values.put(RINGTONE, alarm.alert.toString());
        }

        return values;
    }

    public static Intent createIntent(Context context, Class<?> cls, long alarmId) {
        return new Intent(context, cls).setData(getContentUri(alarmId));
    }

    public static Uri getContentUri(long alarmId) {
        return ContentUris.withAppendedId(CONTENT_URI, alarmId);
    }

    public static long getId(Uri contentUri) {
        return ContentUris.parseId(contentUri);
    }

    /**
     * Get alarm cursor loader for all alarms.
     *
     * @param context to query the database.
     * @return cursor loader with all the alarms.
     */
    public static CursorLoader getAlarmsCursorLoader(Context context) {
        return new CursorLoader(context, ALARMS_WITH_INSTANCES_URI,
                QUERY_ALARMS_WITH_INSTANCES_COLUMNS, null, null, DEFAULT_SORT_ORDER) {
            @Override
            public void onContentChanged() {
                // There is a bug in Loader which can result in stale data if a loader is stopped
                // immediately after a call to onContentChanged. As a workaround we stop the
                // loader before delivering onContentChanged to ensure mContentChanged is set to
                // true before forceLoad is called.
                if (isStarted() && !isAbandoned()) {
                    stopLoading();
                    super.onContentChanged();
                    startLoading();
                } else {
                    super.onContentChanged();
                }
            }

            @Override
            public Cursor loadInBackground() {
                // Prime the ringtone title cache for later access. Most alarms will refer to
                // system ringtones.
                DataModel.getDataModel().loadRingtoneTitles();

                return super.loadInBackground();
            }
        };
    }

    /**
     * Get alarm by id.
     *
     * @param cr provides access to the content model
     * @param alarmId for the desired alarm.
     * @return alarm if found, null otherwise
     */
    public static Alarm getAlarm(ContentResolver cr, long alarmId) {
        try (Cursor cursor = cr.query(getContentUri(alarmId), QUERY_COLUMNS, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new Alarm(cursor);
            }
        }

        return null;
    }

    /**
     * Get all alarms given conditions.
     *
     * @param cr provides access to the content model
     * @param selection A filter declaring which rows to return, formatted as an
     *         SQL WHERE clause (excluding the WHERE itself). Passing null will
     *         return all rows for the given URI.
     * @param selectionArgs You may include ?s in selection, which will be
     *         replaced by the values from selectionArgs, in the order that they
     *         appear in the selection. The values will be bound as Strings.
     * @return list of alarms matching where clause or empty list if none found.
     */
    public static List<Alarm> getAlarms(ContentResolver cr, String selection,
            String... selectionArgs) {
        final List<Alarm> result = new LinkedList<>();
        try (Cursor cursor = cr.query(CONTENT_URI, QUERY_COLUMNS, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(new Alarm(cursor));
                } while (cursor.moveToNext());
            }
        }

        return result;
    }

    public static List<Alarm> getAlarmsWithInstances(ContentResolver cr) {
        final List<Alarm> result = new LinkedList<>();
        try (Cursor cursor = cr.query(ALARMS_WITH_INSTANCES_URI,
                QUERY_ALARMS_WITH_INSTANCES_COLUMNS, null, null, DEFAULT_SORT_ORDER)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    result.add(new Alarm(cursor));
                } while (cursor.moveToNext());
            }
        }
        return result;
    }

    public static boolean isTomorrow(Alarm alarm, Calendar now) {
        if (alarm.instanceState == AlarmInstance.SNOOZE_STATE) {
            return false;
        }

        final int totalAlarmMinutes = alarm.hour * 60 + alarm.minutes;
        final int totalNowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return totalAlarmMinutes <= totalNowMinutes;
    }

    public static Alarm addAlarm(ContentResolver contentResolver, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        Uri uri = contentResolver.insert(CONTENT_URI, values);
        alarm.id = getId(uri);
        return alarm;
    }

    public static void updateAlarm(ContentResolver contentResolver, Alarm alarm) {
        if (alarm.id == Alarm.INVALID_ID) return;
        ContentValues values = createContentValues(alarm);
        contentResolver.update(getContentUri(alarm.id), values, null, null);
    }

    public static boolean deleteAlarm(ContentResolver contentResolver, long alarmId) {
        if (alarmId == INVALID_ID) return false;
        int deletedRows = contentResolver.delete(getContentUri(alarmId), "", null);
        return deletedRows == 1;
    }

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<>() {
        public Alarm createFromParcel(Parcel p) {
            return new Alarm(p);
        }

        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    // Public fields
    // TODO: Refactor instance names
    public long id;
    public boolean enabled;
    public int hour;
    public int minutes;
    public Weekdays daysOfWeek;
    public boolean vibrate;
    public String label;
    public Uri alert;
    public boolean deleteAfterUse;
    public boolean increasingVolume;
    public long scheduledDate = AlarmDates.NONE;
    public long pauseStart = AlarmDates.NONE;
    public long pauseEnd = AlarmDates.NONE;
    public int color = NO_COLOR;
    public long groupId = NO_GROUP;
    public int instanceState;
    public int instanceId;

    // Creates a default alarm at the current time.
    public Alarm() {
        this(0, 0);
    }

    public Alarm(int hour, int minutes) {
        this.id = INVALID_ID;
        this.hour = hour;
        this.minutes = minutes;
        this.vibrate = true;
        this.daysOfWeek = Weekdays.NONE;
        this.label = "";
        this.alert = DataModel.getDataModel().getDefaultAlarmRingtoneUri();
        this.deleteAfterUse = false;
        this.increasingVolume = false;
    }

    public Alarm(Cursor c) {
        id = c.getLong(ID_INDEX);
        enabled = c.getInt(ENABLED_INDEX) == 1;
        hour = c.getInt(HOUR_INDEX);
        minutes = c.getInt(MINUTES_INDEX);
        daysOfWeek = Weekdays.fromBits(c.getInt(DAYS_OF_WEEK_INDEX));
        vibrate = c.getInt(VIBRATE_INDEX) == 1;
        label = c.getString(LABEL_INDEX);
        deleteAfterUse = c.getInt(DELETE_AFTER_USE_INDEX) == 1;
        increasingVolume = c.getInt(INCREASING_VOLUME_INDEX) == 1;
        scheduledDate = getLong(c, SCHEDULED_DATE);
        pauseStart = getLong(c, PAUSE_START);
        pauseEnd = getLong(c, PAUSE_END);
        final int colorIndex = c.getColumnIndex(COLOR);
        color = colorIndex < 0 || c.isNull(colorIndex) ? NO_COLOR : c.getInt(colorIndex);
        final int groupIndex = c.getColumnIndex(GROUP_ID);
        groupId = groupIndex < 0 || c.isNull(groupIndex) ? NO_GROUP : c.getLong(groupIndex);

        if (c.getColumnCount() == ALARM_JOIN_INSTANCE_COLUMN_COUNT) {
            instanceState = c.getInt(INSTANCE_STATE_INDEX);
            instanceId = c.getInt(INSTANCE_ID_INDEX);
        }

        if (c.isNull(RINGTONE_INDEX)) {
            // Should we be saving this with the current ringtone or leave it null
            // so it changes when user changes default ringtone?
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            alert = Uri.parse(c.getString(RINGTONE_INDEX));
        }
    }

    Alarm(Parcel p) {
        id = p.readLong();
        enabled = p.readInt() == 1;
        hour = p.readInt();
        minutes = p.readInt();
        daysOfWeek = Weekdays.fromBits(p.readInt());
        vibrate = p.readInt() == 1;
        label = p.readString();
        alert = p.readParcelable(null);
        deleteAfterUse = p.readInt() == 1;
        increasingVolume = p.readInt() == 1;
        scheduledDate = p.readLong();
        pauseStart = p.readLong();
        pauseEnd = p.readLong();
        color = p.readInt();
        groupId = p.readLong();
    }

    private static long getLong(Cursor c, String column) {
        final int index = c.getColumnIndex(column);
        return index < 0 || c.isNull(index) ? AlarmDates.NONE : c.getLong(index);
    }

    public String getLabelOrDefault(Context context) {
        return label.isEmpty() ? context.getString(R.string.default_label) : label;
    }

    /**
     * Whether the alarm is in a state to show preemptive dismiss. Valid states are SNOOZE_STATE
     * HIGH_NOTIFICATION, LOW_NOTIFICATION, and HIDE_NOTIFICATION.
     */
    public boolean canPreemptivelyDismiss() {
        return instanceState == AlarmInstance.SNOOZE_STATE
                || instanceState == AlarmInstance.HIGH_NOTIFICATION_STATE
                || instanceState == AlarmInstance.LOW_NOTIFICATION_STATE
                || instanceState == AlarmInstance.HIDE_NOTIFICATION_STATE;
    }

    public void writeToParcel(Parcel p, int flags) {
        p.writeLong(id);
        p.writeInt(enabled ? 1 : 0);
        p.writeInt(hour);
        p.writeInt(minutes);
        p.writeInt(daysOfWeek.getBits());
        p.writeInt(vibrate ? 1 : 0);
        p.writeString(label);
        p.writeParcelable(alert, flags);
        p.writeInt(deleteAfterUse ? 1 : 0);
        p.writeInt(increasingVolume ? 1 : 0);
        p.writeLong(scheduledDate);
        p.writeLong(pauseStart);
        p.writeLong(pauseEnd);
        p.writeInt(color);
        p.writeLong(groupId);
    }

    public int describeContents() {
        return 0;
    }

    public AlarmInstance createInstanceAfter(Calendar time) {
        Calendar nextInstanceTime = getNextAlarmTime(time);
        AlarmInstance result = new AlarmInstance(nextInstanceTime, id);
        result.mVibrate = vibrate;
        result.mLabel = label;
        result.mRingtone = alert;
        result.mIncreasingVolume = increasingVolume;
        return result;
    }

    /**
     *
     * @param currentTime the current time
     * @return previous firing time, or null if this is a one-time alarm.
     */
    public Calendar getPreviousAlarmTime(Calendar currentTime) {
        final Calendar previousInstanceTime = Calendar.getInstance(currentTime.getTimeZone());
        previousInstanceTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
        previousInstanceTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
        previousInstanceTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));
        previousInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        previousInstanceTime.set(Calendar.MINUTE, minutes);
        previousInstanceTime.set(Calendar.SECOND, 0);
        previousInstanceTime.set(Calendar.MILLISECOND, 0);

        final int subtractDays = daysOfWeek.getDistanceToPreviousDay(previousInstanceTime);
        if (subtractDays > 0) {
            previousInstanceTime.add(Calendar.DAY_OF_WEEK, -subtractDays);
            return previousInstanceTime;
        } else {
            return null;
        }
    }

    public Calendar getNextAlarmTime(Calendar currentTime) {
        return AlarmDates.nextAlarmTime(this, currentTime);
    }

    /**
     * @return the next time after {@code currentTime} that matches the alarm's time and weekdays,
     *     ignoring its scheduled date and pause range
     */
    public Calendar getNextWeekdayTime(Calendar currentTime) {
        final Calendar nextInstanceTime = Calendar.getInstance(currentTime.getTimeZone());
        nextInstanceTime.set(Calendar.YEAR, currentTime.get(Calendar.YEAR));
        nextInstanceTime.set(Calendar.MONTH, currentTime.get(Calendar.MONTH));
        nextInstanceTime.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH));
        nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        nextInstanceTime.set(Calendar.MINUTE, minutes);
        nextInstanceTime.set(Calendar.SECOND, 0);
        nextInstanceTime.set(Calendar.MILLISECOND, 0);

        // If we are still behind the passed in currentTime, then add a day
        if (nextInstanceTime.getTimeInMillis() <= currentTime.getTimeInMillis()) {
            nextInstanceTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        // The day of the week might be invalid, so find next valid one
        final int addDays = daysOfWeek.getDistanceToNextDay(nextInstanceTime);
        if (addDays > 0) {
            nextInstanceTime.add(Calendar.DAY_OF_WEEK, addDays);
        }

        // Daylight Savings Time can alter the hours and minutes when adjusting the day above.
        // Reset the desired hour and minute now that the correct day has been chosen.
        nextInstanceTime.set(Calendar.HOUR_OF_DAY, hour);
        nextInstanceTime.set(Calendar.MINUTE, minutes);

        return nextInstanceTime;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Alarm)) return false;
        final Alarm other = (Alarm) o;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "Alarm{" +
                "alert=" + alert +
                ", id=" + id +
                ", enabled=" + enabled +
                ", hour=" + hour +
                ", minutes=" + minutes +
                ", daysOfWeek=" + daysOfWeek +
                ", vibrate=" + vibrate +
                ", label='" + label + '\'' +
                ", deleteAfterUse=" + deleteAfterUse +
                ", increasingVolume=" + increasingVolume +
                ", scheduledDate=" + scheduledDate +
                ", pauseStart=" + pauseStart +
                ", pauseEnd=" + pauseEnd +
                ", color=" + color +
                ", groupId=" + groupId +
                '}';
    }
}
