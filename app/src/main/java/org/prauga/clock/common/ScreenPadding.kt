/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.components.PvotScreenDefaults
import com.prauga.pvot.designsystem.components.PvotScreenScaffoldDefaults
import com.prauga.pvot.designsystem.components.button.PvotFabDefaults
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import com.prauga.pvot.designsystem.theme.PvotSpacing

// PvotScreenDefaults leaves out the system navigation bar under the floating nav bar.
@Composable
fun screenPadding(scaffold: PaddingValues, withFab: Boolean = false): PaddingValues =
    PvotScreenDefaults.contentPadding(
        top = scaffold.calculateTopPadding() + PvotSpacing.S,
        bottom =
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                LocalPvotNavBarSizes.current.barHeight +
                PvotScreenDefaults.BottomSpacing +
                if (withFab) {
                    PvotFabDefaults.Size + PvotScreenScaffoldDefaults.FloatingActionMargin
                } else {
                    0.dp
                },
    )
