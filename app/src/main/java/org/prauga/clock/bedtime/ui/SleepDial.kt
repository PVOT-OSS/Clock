/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.bedtime.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import org.prauga.clock.R
import org.prauga.clock.common.formatTime

private val DialMaxSize: Dp = 360.dp
private val RingWidth: Dp = 44.dp
private val HandleIconSize: Dp = 20.dp
private val TickRadius: Dp = 1.5.dp
private const val LABEL_HOURS_APART = 6
private const val HOURS_PER_DAY = 24
private const val DEGREES = 360f
private const val TOP_DEGREES = -90f

private enum class DialHandle {
    Bedtime,
    WakeUp,
}

@Composable
fun SleepDial(
    bedMinutes: Int,
    wakeMinutes: Int,
    wakeAdjustable: Boolean,
    onBedtimeChange: (Int) -> Unit,
    onWakeUpChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggedBed by remember { mutableStateOf<Int?>(null) }
    var draggedWake by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(bedMinutes) { draggedBed = null }
    LaunchedEffect(wakeMinutes) { draggedWake = null }
    val bed = draggedBed ?: bedMinutes
    val wake =
        draggedWake
            ?: if (wakeAdjustable) wakeMinutes
            else wrapDay(bed + sleepLength(bedMinutes, wakeMinutes))
    val haptics = LocalHapticFeedback.current
    val currentBed by rememberUpdatedState(bed)
    val currentWake by rememberUpdatedState(wake)
    val bedChange by rememberUpdatedState(onBedtimeChange)
    val wakeChange by rememberUpdatedState(onWakeUpChange)
    val ringWidthPx = with(LocalDensity.current) { RingWidth.toPx() }

    BoxWithConstraints(
        modifier = modifier.widthIn(max = DialMaxSize).aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        val sizePx = with(LocalDensity.current) { min(maxWidth.toPx(), maxHeight.toPx()) }
        val ringRadius = sizePx / 2 - ringWidthPx / 2

        val gesture =
            Modifier.pointerInput(wakeAdjustable, ringRadius) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val handle =
                        when {
                            near(down.position, center, ringRadius, currentBed, ringWidthPx) ->
                                DialHandle.Bedtime
                            wakeAdjustable &&
                                near(down.position, center, ringRadius, currentWake, ringWidthPx) ->
                                DialHandle.WakeUp
                            else -> null
                        } ?: return@awaitEachGesture
                    down.consume()
                    val start = if (handle == DialHandle.Bedtime) currentBed else currentWake
                    var last = start
                    drag(down.id) { change ->
                        change.consume()
                        val minutes = minutesAt(change.position - center)
                        if (minutes != last) {
                            last = minutes
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            when (handle) {
                                DialHandle.Bedtime -> draggedBed = minutes
                                DialHandle.WakeUp -> draggedWake = minutes
                            }
                        }
                    }
                    if (last != start) {
                        when (handle) {
                            DialHandle.Bedtime -> bedChange(last)
                            DialHandle.WakeUp -> wakeChange(last)
                        }
                    }
                }
            }

        DialCanvas(bed, wake, wakeAdjustable, ringWidthPx, Modifier.fillMaxSize().then(gesture))
        Text(
            text = sleepLengthText(sleepLength(bed, wake)),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        HandleSemantics(
            label = stringResource(R.string.bedtime_schedule_title),
            minutes = bed,
            ringRadius = ringRadius,
            ringWidthPx = ringWidthPx,
            onChange = onBedtimeChange,
        )
        HandleSemantics(
            label = stringResource(R.string.wakeup_title),
            minutes = wake,
            ringRadius = ringRadius,
            ringWidthPx = ringWidthPx,
            onChange = if (wakeAdjustable) onWakeUpChange else null,
        )
    }
}

@Composable
private fun DialCanvas(
    bed: Int,
    wake: Int,
    wakeAdjustable: Boolean,
    ringWidthPx: Float,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labels = hourLabels()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
    val moon = painterResource(R.drawable.ic_t_moon)
    val alarm = painterResource(R.drawable.ic_t_alarm)
    val iconSizePx = with(LocalDensity.current) { HandleIconSize.toPx() }
    val tickPx = with(LocalDensity.current) { TickRadius.toPx() }

    Canvas(modifier) {
        val ringRadius = size.minDimension / 2 - ringWidthPx / 2
        val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
        val ringSize = Size(ringRadius * 2, ringRadius * 2)
        drawCircle(
            color = colors.surfaceContainerHigh,
            radius = ringRadius,
            style = Stroke(ringWidthPx),
        )
        val tickRadius = ringRadius - ringWidthPx
        for (hour in 0 until HOURS_PER_DAY) {
            if (hour % LABEL_HOURS_APART == 0) {
                val layout = measurer.measure(labels[hour / LABEL_HOURS_APART], labelStyle)
                val at = handleCenter(center, tickRadius, hour * MINUTES_PER_HOUR)
                drawText(
                    layout,
                    topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f),
                )
            } else {
                drawCircle(
                    color = colors.outlineVariant,
                    radius = tickPx,
                    center = handleCenter(center, tickRadius, hour * MINUTES_PER_HOUR),
                )
            }
        }
        drawArc(
            color = colors.primary,
            startAngle = angleOf(bed),
            sweepAngle = sleepLength(bed, wake) * DEGREES / MINUTES_PER_DAY,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(ringWidthPx, cap = StrokeCap.Round),
        )
        drawHandle(
            moon,
            handleCenter(center, ringRadius, bed),
            ringWidthPx,
            iconSizePx,
            colors.onPrimary,
            colors.primaryContainer,
        )
        drawHandle(
            alarm,
            handleCenter(center, ringRadius, wake),
            ringWidthPx,
            iconSizePx,
            if (wakeAdjustable) colors.onPrimary else colors.onSurfaceVariant,
            colors.primaryContainer,
        )
    }
}

private fun DrawScope.drawHandle(
    icon: Painter,
    at: Offset,
    ringWidthPx: Float,
    iconSizePx: Float,
    tint: Color,
    fill: Color,
) {
    drawCircle(color = fill, radius = ringWidthPx / 2 - ringWidthPx / 10, center = at)
    translate(at.x - iconSizePx / 2, at.y - iconSizePx / 2) {
        with(icon) { draw(Size(iconSizePx, iconSizePx), colorFilter = ColorFilter.tint(tint)) }
    }
}

@Composable
private fun HandleSemantics(
    label: String,
    minutes: Int,
    ringRadius: Float,
    ringWidthPx: Float,
    onChange: ((Int) -> Unit)?,
) {
    val context = LocalContext.current
    val later = stringResource(R.string.bedtime_dial_later)
    val earlier = stringResource(R.string.bedtime_dial_earlier)
    val time = formatTime(context, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    val at = handleCenter(Offset.Zero, ringRadius, minutes)
    val sizeDp = with(LocalDensity.current) { ringWidthPx.toDp() }
    Box(
        Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
            .size(sizeDp)
            .semantics {
                contentDescription = label
                stateDescription = time
                if (onChange != null) {
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = minutes.toFloat(),
                            range = 0f..(MINUTES_PER_DAY - DIAL_STEP_MINUTES).toFloat(),
                            steps = MINUTES_PER_DAY / DIAL_STEP_MINUTES - 2,
                        )
                    setProgress { target ->
                        onChange(snapToStep(target))
                        true
                    }
                    customActions =
                        listOf(
                            CustomAccessibilityAction(later) {
                                onChange(wrapDay(minutes + DIAL_STEP_MINUTES))
                                true
                            },
                            CustomAccessibilityAction(earlier) {
                                onChange(wrapDay(minutes - DIAL_STEP_MINUTES))
                                true
                            },
                        )
                }
            }
    )
}

@Composable
private fun hourLabels(): List<String> {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val pattern =
        DateFormat.getBestDateTimePattern(
            locale,
            if (DateFormat.is24HourFormat(context)) "H" else "ha",
        )
    return remember(pattern, locale) {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        (0 until HOURS_PER_DAY step LABEL_HOURS_APART).map { LocalTime.of(it, 0).format(formatter) }
    }
}

@Composable
internal fun sleepLengthText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    val hoursText = pluralStringResource(R.plurals.bedtime_sleep_hours, hours, hours)
    return if (rest == 0) hoursText
    else
        stringResource(
            R.string.bedtime_sleep_length,
            hoursText,
            pluralStringResource(R.plurals.bedtime_sleep_minutes, rest, rest),
        )
}

private fun angleOf(minutes: Int): Float = minutes * DEGREES / MINUTES_PER_DAY + TOP_DEGREES

private fun handleCenter(center: Offset, radius: Float, minutes: Int): Offset {
    val radians = Math.toRadians(angleOf(minutes).toDouble())
    return Offset(
        center.x + radius * cos(radians).toFloat(),
        center.y + radius * sin(radians).toFloat(),
    )
}

private fun near(
    position: Offset,
    center: Offset,
    ringRadius: Float,
    minutes: Int,
    hitRadius: Float,
): Boolean {
    val handle = handleCenter(center, ringRadius, minutes)
    return hypot(position.x - handle.x, position.y - handle.y) <= hitRadius
}

private fun minutesAt(fromCenter: Offset): Int {
    val degrees =
        Math.toDegrees(atan2(fromCenter.y, fromCenter.x).toDouble()).toFloat() - TOP_DEGREES
    return snapToStep(((degrees % DEGREES) + DEGREES) % DEGREES * MINUTES_PER_DAY / DEGREES)
}
