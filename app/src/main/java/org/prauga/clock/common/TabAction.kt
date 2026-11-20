/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class TabAction(@DrawableRes val icon: Int, @StringRes val label: Int, val onClick: () -> Unit)

class TabActionSlot {
    var action by mutableStateOf<TabAction?>(null)
}

val LocalTabActionSlot = staticCompositionLocalOf { TabActionSlot() }

// The selected tab's pill in the nav bar shows this action and runs it when tapped again.
@Composable
fun ScreenAction(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    val slot = LocalTabActionSlot.current
    val latestClick by rememberUpdatedState(onClick)
    DisposableEffect(slot, icon, label) {
        val action = TabAction(icon, label) { latestClick() }
        slot.action = action
        onDispose { if (slot.action === action) slot.action = null }
    }
}
