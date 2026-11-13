/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.button.PvotFab

/**
 * The scaffold's floating action, filled by whichever screen is showing: the button sits above the
 * nav bar's end, where every Pvot app keeps it, while the screen owns what it does.
 */
class FloatingActionSlot {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)

    /** Whether the label shows; it folds away while the list is scrolled. */
    var expanded by mutableStateOf(true)
}

val LocalFloatingActionSlot = staticCompositionLocalOf { FloatingActionSlot() }

/** Puts a labelled [PvotFab] in the scaffold for as long as the calling screen is shown. */
@Composable
fun ScreenFab(@DrawableRes icon: Int, @StringRes text: Int, onClick: () -> Unit) {
    val slot = LocalFloatingActionSlot.current
    val latestIcon by rememberUpdatedState(icon)
    val latestText by rememberUpdatedState(text)
    val latestClick by rememberUpdatedState(onClick)
    DisposableEffect(slot) {
        val fab: @Composable () -> Unit = {
            PvotFab(
                icon = painterResource(latestIcon),
                onClick = { latestClick() },
                contentDescription = null,
                text = stringResource(latestText),
                expanded = slot.expanded,
            )
        }
        slot.content = fab
        onDispose { if (slot.content === fab) slot.content = null }
    }
}
