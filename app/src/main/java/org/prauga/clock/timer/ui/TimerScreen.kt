/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.timer.ui

import android.os.SystemClock
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.picker.PvotDurationPicker
import com.prauga.pvot.designsystem.components.progress.PvotLinearProgress
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.prauga.clock.R
import org.prauga.clock.common.ScreenAction
import org.prauga.clock.common.screenPadding
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.Timer
import org.prauga.clock.timer.data.TimerPreset
import org.prauga.clock.timer.data.TimerPresetsRepository

@Composable
fun TimerScreen(
    padding: PaddingValues,
    listState: LazyListState,
    setupRequests: Int,
    viewModel: TimerViewModel = timerViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = (state as? UiState.Success)?.data ?: return
    var adding by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(setupRequests) { if (setupRequests > 0) adding = true }

    Box(Modifier.fillMaxSize()) {
        if (content.timers.isEmpty() || adding) {
            TimerSetup(
                presets = content.presets,
                padding = padding,
                canCancel = content.timers.isNotEmpty(),
                onCancel = { adding = false },
                onStart = { length, label ->
                    viewModel.onEvent(TimerEvent.Create(length, label))
                    adding = false
                },
                onEvent = viewModel::onEvent,
            )
        } else {
            TimerList(
                content.timers,
                listState,
                padding,
                viewModel::onEvent,
                onAdd = { adding = true },
            )
        }
    }
}

@Composable
fun timerViewModel(expiredOnly: Boolean = false): TimerViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel {
        TimerViewModel(DataModel.getDataModel(), TimerPresetsRepository(context), expiredOnly)
    }
}

@Composable
private fun TimerSetup(
    presets: List<TimerPreset>,
    padding: PaddingValues,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onStart: (Long, String) -> Unit,
    onEvent: (TimerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var duration by rememberSaveable(stateSaver = DurationSaver) { mutableStateOf(Duration.ZERO) }
    var chosenPreset by rememberSaveable { mutableStateOf<Long?>(null) }
    var actionsFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    val label =
        presets
            .firstOrNull {
                it.id == chosenPreset && it.lengthMillis == duration.inWholeMilliseconds
            }
            ?.name
            .orEmpty()
    if (duration > Duration.ZERO) {
        ScreenAction(R.drawable.ic_t_player_play, R.string.timer_start) {
            onStart(duration.inWholeMilliseconds, label)
            duration = Duration.ZERO
            chosenPreset = null
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(PresetChipSize),
        state = rememberLazyGridState(),
        contentPadding = screenPadding(padding),
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = KEY_PICKER, span = { GridItemSpan(maxLineSpan) }) {
            PvotDurationPicker(
                duration = duration,
                onDurationChange = { duration = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (canCancel) {
            item(key = KEY_CANCEL, span = { GridItemSpan(maxLineSpan) }) {
                Box(contentAlignment = Alignment.Center) {
                    PvotButton(
                        text = stringResource(android.R.string.cancel),
                        onClick = onCancel,
                        style = PvotButtonStyle.Text,
                    )
                }
            }
        }
        item(key = KEY_SAVED, span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.timer_saved),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = PvotSpacing.M),
            )
        }
        items(presets, key = { it.id }) { preset ->
            PresetChip(
                preset = preset,
                onClick = {
                    duration = preset.lengthMillis.milliseconds
                    chosenPreset = preset.id
                },
                onLongClick = { actionsFor = preset.id },
            )
        }
        item(key = KEY_ADD) {
            AddPresetChip(
                onClick = {
                    editing = null
                    editorOpen = true
                }
            )
        }
    }

    PresetActionsSheet(
        preset = presets.firstOrNull { it.id == actionsFor },
        onDismiss = { actionsFor = null },
        onEdit = {
            actionsFor = null
            editing = it.id
            editorOpen = true
        },
        onDelete = {
            actionsFor = null
            onEvent(TimerEvent.DeletePreset(it.id))
        },
    )
    PresetEditorDialog(
        preset = presets.firstOrNull { it.id == editing },
        visible = editorOpen,
        onDismiss = { editorOpen = false },
        onSave = { name, length ->
            onEvent(TimerEvent.SavePreset(editing, name, length))
            editorOpen = false
        },
    )
}

@Composable
private fun TimerList(
    timers: List<TimerItem>,
    listState: LazyListState,
    padding: PaddingValues,
    onEvent: (TimerEvent) -> Unit,
    onAdd: () -> Unit,
) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val ticking = timers.any { it.isTicking }
    LaunchedEffect(ticking) {
        while (ticking) {
            withFrameMillis { now = SystemClock.elapsedRealtime() }
        }
    }
    ScreenAction(R.drawable.ic_t_plus, R.string.new_timer, onAdd)
    Box(Modifier.fillMaxSize()) {
        PvotScreen(state = listState, contentPadding = screenPadding(padding)) {
            itemsIndexed(timers, key = { _, item -> item.id }) { index, item ->
                TimerCard(item, now, onEvent, Modifier.pvotReveal(index))
            }
        }
    }
}

@Composable
private fun TimerCard(
    item: TimerItem,
    now: Long,
    onEvent: (TimerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = item.remainingAt(now)
    PvotCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(PvotSpacing.L)) {
            Text(
                text = item.label.ifEmpty { stringResource(R.string.timer_notification_label) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = format(remaining),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Light,
                color =
                    if (item.state == Timer.State.EXPIRED) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
            )
            PvotLinearProgress(
                progress = {
                    (1f - remaining.toFloat() / item.length.coerceAtLeast(1)).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = PvotSpacing.S),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PvotButton(
                    text = stringResource(R.string.timer_plus_1_min),
                    onClick = { onEvent(TimerEvent.AddMinute(item.id)) },
                    style = PvotButtonStyle.Tonal,
                )
                when {
                    item.state == Timer.State.RUNNING ->
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_t_player_pause),
                            contentDescription = stringResource(R.string.timer_pause),
                            onClick = { onEvent(TimerEvent.Pause(item.id)) },
                        )
                    item.state == Timer.State.EXPIRED ->
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_t_x),
                            contentDescription = stringResource(R.string.timer_stop),
                            onClick = { onEvent(TimerEvent.Stop(item.id)) },
                        )
                    else ->
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_t_player_play),
                            contentDescription = stringResource(R.string.timer_start),
                            onClick = { onEvent(TimerEvent.Start(item.id)) },
                        )
                }
                if (item.state == Timer.State.PAUSED) {
                    PvotIconButton(
                        icon = painterResource(R.drawable.ic_t_refresh),
                        contentDescription = stringResource(R.string.timer_reset),
                        onClick = { onEvent(TimerEvent.Reset(item.id)) },
                    )
                }
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_t_trash),
                    contentDescription = stringResource(R.string.timer_delete),
                    onClick = { onEvent(TimerEvent.Delete(item.id)) },
                )
            }
        }
    }
}

private fun format(remaining: Long): String {
    val seconds =
        if (remaining >= 0) (remaining + DateUtils.SECOND_IN_MILLIS - 1) / 1000
        else remaining / 1000
    val total = abs(seconds)
    val text =
        if (total >= 3600) "%d:%02d:%02d".format(total / 3600, total / 60 % 60, total % 60)
        else "%d:%02d".format(total / 60, total % 60)
    return if (seconds < 0) "−$text" else text
}

private const val KEY_PICKER = "picker"
private const val KEY_CANCEL = "cancel"
private const val KEY_SAVED = "saved"
private const val KEY_ADD = "add"

private val DurationSaver =
    androidx.compose.runtime.saveable.Saver<Duration, Long>(
        save = { it.inWholeSeconds },
        restore = { it.seconds },
    )
