<!--
SPDX-FileCopyrightText: Saalim Quadri <danascape@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Privacy Policy — Clock

_Effective 20 November 2026_

Clock (package `org.prauga.clock`) is an alarm clock, timer, stopwatch and world clock app
published by Saalim Quadri for PVOT-OSS. This policy explains what the app does with your data.

**In short: the app doesn't collect, send, share or sell any personal data. It has no accounts, no
ads, no analytics and no tracking, and it has no permission to use the internet.**

## What the app keeps on your device

- **Alarms**: their times, days, dates, labels, sounds and vibration settings.
- **Timers and the stopwatch**: their lengths, labels, laps and whether they are running.
- **World clocks**: the cities you chose, and the names and time zones of places you added
  yourself.
- **Bedtime**: your bedtime, days, reminder and sleep sound choices, and which alarm wakes you.
- **Recorded sounds**: if you record a sound to use for an alarm, the app keeps a copy of that
  recording.
- **Settings**: your choices in the app's settings and screen saver settings.

All of this stays in the app's private storage and is deleted when you clear the app's storage or
uninstall it.

## Backups

If you turn on Android's device backup, Android may include your alarms and settings in your device
backup (for example, Google's backup on most devices), or copy them when you move to a new phone,
under the terms of that backup service. Recorded sounds are not included. The app itself never sends
anything anywhere. You can turn backup off in your device's settings.

## Your calendar

If you allow calendar access on the Bedtime tab, the app reads the time and title of tomorrow's
first timed event so it can show it to you before you go to bed. It reads nothing else, does not
store what it reads, and never sends it anywhere. You can turn calendar access off in Android's app
settings at any time; the rest of the app works without it.

## Permissions

| Permission | Why |
|---|---|
| Notifications (`POST_NOTIFICATIONS`, `POST_PROMOTED_NOTIFICATIONS`) | Show upcoming and ringing alarms, running timers and the stopwatch, and the bedtime reminder |
| Alarms and reminders (`SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`) | Ring alarms and timers at the exact minute |
| Full-screen notifications (`USE_FULL_SCREEN_INTENT`) | Show the ringing alarm or finished timer over the lock screen |
| Foreground services (`FOREGROUND_SERVICE`, `…_SYSTEM_EXEMPTED`, `…_MEDIA_PLAYBACK`) | Keep an alarm or timer ringing, and play sleep sounds, while the app is in the background |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Set your alarms again after the phone restarts |
| Keep awake (`WAKE_LOCK`) and vibration (`VIBRATE`) | Wake the phone and vibrate when an alarm or timer goes off |
| Audio settings (`MODIFY_AUDIO_SETTINGS`) | Set the alarm volume from the app's settings |
| Do Not Disturb access (`ACCESS_NOTIFICATION_POLICY`) | Turn on Do Not Disturb at bedtime, only if you switch on Bedtime mode |
| Calendar (`READ_CALENDAR`) | Show tomorrow's first event on the Bedtime tab, only if you allow it |
| `org.codeaurora.permission.POWER_OFF_ALARM` | On phones that support it, ring an alarm while the phone is switched off |

`org.prauga.clock.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` is added by the AndroidX library to
keep the app's internal broadcasts private; it gives the app no access to your data.

The app does not have the `INTERNET` permission.

## Other apps

Other apps can ask Clock to set an alarm or a timer, as Android allows for any clock app. Links that
open Clock from other apps or web pages always show Clock's own screen instead of acting silently.
When you record a sound, the recording app you choose hands the recording to Clock; Clock keeps a
copy and doesn't share it.

## Children

The app is not directed at children and collects no data from anyone, children included.

## Changes

If the app starts handling data differently, this policy will be updated before that version is
released, with a new effective date at the top.

## Contact

Questions about this policy: danascape@gmail.com, or open an issue at
https://github.com/PVOT-OSS/Clock/issues.
