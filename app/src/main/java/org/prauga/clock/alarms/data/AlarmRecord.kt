/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.data

import org.prauga.clock.provider.Alarm
import org.prauga.clock.provider.AlarmInstance

class AlarmRecord(val alarm: Alarm, val instance: AlarmInstance?)
