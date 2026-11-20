/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

// Side by side; on a book-folded device the panes meet at the hinge instead of the weights.
@Composable
fun TwoPane(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstWeight: Float = DEFAULT_FIRST_WEIGHT,
) {
    val layout = rememberWindowLayout()
    val hinge = layout.hinge?.takeIf { layout.book }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val gap = PaneGap
    Layout(
        contents = listOf(first, second),
        modifier = modifier.onGloballyPositioned { origin = it.positionInWindow() },
    ) { (firstMeasurables, secondMeasurables), constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val (firstWidth, secondStart) =
            if (hinge != null) {
                val left = (hinge.bounds.left - origin.x).roundToInt().coerceIn(0, width)
                val right = (hinge.bounds.right - origin.x).roundToInt().coerceIn(left, width)
                left to right
            } else {
                val w = ((width - gapPx) * firstWeight).roundToInt()
                w to w + gapPx
            }
        val secondWidth = (width - secondStart).coerceAtLeast(0)
        val height = constraints.maxHeight
        val firstPlaceables =
            firstMeasurables.map {
                it.measure(Constraints(maxWidth = firstWidth, maxHeight = height))
            }
        val secondPlaceables =
            secondMeasurables.map {
                it.measure(Constraints(maxWidth = secondWidth, maxHeight = height))
            }
        layout(width, height) {
            firstPlaceables.forEach { it.place(0, 0) }
            secondPlaceables.forEach { it.place(secondStart, 0) }
        }
    }
}

// Tabletop posture: [top] on the upright half above the hinge, [bottom] on the flat half below it.
@Composable
fun TabletopSplit(
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hinge = rememberWindowLayout().hinge
    var origin by remember { mutableStateOf(Offset.Zero) }
    Layout(
        contents = listOf(top, bottom),
        modifier = modifier.onGloballyPositioned { origin = it.positionInWindow() },
    ) { (topMeasurables, bottomMeasurables), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val hingeTop =
            hinge?.let { (it.bounds.top - origin.y).roundToInt().coerceIn(0, height) } ?: height / 2
        val hingeBottom =
            hinge?.let { (it.bounds.bottom - origin.y).roundToInt().coerceIn(hingeTop, height) }
                ?: hingeTop
        val topPlaceables =
            topMeasurables.map { it.measure(Constraints(maxWidth = width, maxHeight = hingeTop)) }
        val bottomPlaceables =
            bottomMeasurables.map {
                it.measure(Constraints(maxWidth = width, maxHeight = height - hingeBottom))
            }
        layout(width, height) {
            topPlaceables.forEach { it.place(0, 0) }
            bottomPlaceables.forEach { it.place(0, hingeBottom) }
        }
    }
}

// Keeps a single column readable on wide windows instead of stretching it edge to edge.
@Composable
fun CenteredContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth()) { content() }
    }
}

private const val DEFAULT_FIRST_WEIGHT = 0.45f
