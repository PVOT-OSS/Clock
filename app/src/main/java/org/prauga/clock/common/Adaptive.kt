/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

import android.graphics.Rect
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class WindowWidth {
    Compact,
    Medium,
    Expanded,
}

class Hinge(
    val bounds: Rect,
    val horizontal: Boolean,
    val halfOpened: Boolean,
    val separating: Boolean,
)

data class WindowLayout(val width: WindowWidth, val hinge: Hinge?) {
    val tabletop: Boolean
        get() = hinge?.let { it.horizontal && it.halfOpened } == true

    val book: Boolean
        get() = hinge?.let { !it.horizontal && (it.halfOpened || it.separating) } == true

    val twoPane: Boolean
        get() = width != WindowWidth.Compact || book
}

val ContentMaxWidth: Dp = 640.dp
val PaneGap: Dp = 24.dp

private val MediumWidth = 600.dp
private val ExpandedWidth = 840.dp

@Composable
fun rememberWindowLayout(): WindowLayout {
    val activity = LocalActivity.current
    val widthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val hinges =
        remember(activity) {
            if (activity == null) {
                flowOf(null)
            } else {
                WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).map { info ->
                    info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()?.let {
                        Hinge(
                            bounds = it.bounds,
                            horizontal = it.orientation == FoldingFeature.Orientation.HORIZONTAL,
                            halfOpened = it.state == FoldingFeature.State.HALF_OPENED,
                            separating = it.isSeparating,
                        )
                    }
                }
            }
        }
    val hinge by hinges.collectAsState(initial = null)
    val width =
        when {
            widthDp >= ExpandedWidth -> WindowWidth.Expanded
            widthDp >= MediumWidth -> WindowWidth.Medium
            else -> WindowWidth.Compact
        }
    return WindowLayout(width, hinge)
}
