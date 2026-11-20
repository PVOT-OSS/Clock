/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.alarms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.adaptive.PvotTwoPane
import com.prauga.pvot.designsystem.adaptive.rememberPvotWindowLayout
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotScreenDefaults
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.clock.R
import org.prauga.clock.alarms.AlarmEditorActivity
import org.prauga.clock.alarms.data.AlarmRepository
import org.prauga.clock.common.ScreenAction
import org.prauga.clock.common.screenPadding
import org.prauga.clock.provider.Alarm

@Composable
fun AlarmsScreen(
    padding: PaddingValues,
    listState: LazyListState,
    createRequests: Int,
    shownAlarm: Long,
    viewModel: AlarmsViewModel = alarmsViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val alarms = (state as? UiState.Success)?.data
    val context = LocalContext.current
    val twoPane = rememberPvotWindowLayout().twoPane
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var session by rememberSaveable { mutableIntStateOf(0) }
    val open: (Long) -> Unit = { id ->
        if (twoPane) {
            selected = id
            session++
        } else {
            context.startActivity(AlarmEditorActivity.intent(context, id))
        }
    }
    LaunchedEffect(createRequests) {
        if (viewModel.isNewCreateRequest(createRequests)) open(Alarm.INVALID_ID)
    }
    var scrollTarget by remember { mutableLongStateOf(Alarm.INVALID_ID) }
    LaunchedEffect(shownAlarm, alarms) {
        val known = alarms?.all?.any { it.id == shownAlarm } == true
        if (known && viewModel.isNewShownAlarm(shownAlarm)) {
            if (twoPane) open(shownAlarm)
            scrollTarget = shownAlarm
        }
    }
    LaunchedEffect(twoPane) {
        val id = selected
        if (!twoPane && id != null) {
            selected = null
            context.startActivity(AlarmEditorActivity.intent(context, id))
        }
    }

    ScreenAction(R.drawable.ic_t_plus, R.string.new_alarm) { open(Alarm.INVALID_ID) }

    var groupId by rememberSaveable { mutableStateOf<Long?>(null) }
    var groupDialog by rememberSaveable { mutableStateOf(GroupDialog.None) }
    val group = alarms?.groups?.firstOrNull { it.group.id == groupId }
    val lastGroup = remember { mutableStateOf<AlarmGroupSection?>(null) }
    if (group != null) lastGroup.value = group
    val shownGroup = lastGroup.value
    val closeGroupDialog = { groupDialog = GroupDialog.None }

    val list =
        @Composable {
            AlarmList(
                alarms = alarms,
                padding = padding,
                listState = listState,
                selected = selected.takeIf { twoPane },
                scrollTarget = scrollTarget,
                onScrolled = { scrollTarget = Alarm.INVALID_ID },
                onOpen = open,
                onEvent = viewModel::onEvent,
                onGroupLongPress = { id ->
                    groupId = id
                    groupDialog = GroupDialog.Actions
                },
            )
        }
    if (twoPane) {
        PvotTwoPane(
            first = list,
            second = {
                val id = selected
                if (id == null) {
                    AlarmEditorPlaceholder(Modifier.fillMaxSize().padding(padding))
                } else {
                    key(session) {
                        AlarmEditorPane(
                            alarmId = id,
                            session = session,
                            contentPadding = screenPadding(padding),
                            onClose = { selected = null },
                        )
                    }
                }
            },
        )
    } else {
        list()
    }

    if (shownGroup != null) {
        val id = shownGroup.group.id
        GroupActionsSheet(
            visible = group != null && groupDialog == GroupDialog.Actions,
            name = shownGroup.group.name,
            onDismiss = closeGroupDialog,
            onRename = { groupDialog = GroupDialog.Rename },
            onDelete = { groupDialog = GroupDialog.Delete },
        )
        GroupNameDialog(
            visible = group != null && groupDialog == GroupDialog.Rename,
            title = stringResource(R.string.alarm_group_rename_title),
            initial = shownGroup.group.name,
            onDismiss = closeGroupDialog,
            onSave = {
                viewModel.onEvent(AlarmEvent.RenameGroup(id, it))
                closeGroupDialog()
            },
        )
        DeleteGroupDialog(
            visible = group != null && groupDialog == GroupDialog.Delete,
            name = shownGroup.group.name,
            alarmCount = shownGroup.alarms.size,
            onDismiss = closeGroupDialog,
            onDelete = {
                viewModel.onEvent(AlarmEvent.DeleteGroup(id, it))
                closeGroupDialog()
            },
        )
    }
}

private enum class GroupDialog {
    None,
    Actions,
    Rename,
    Delete,
}

@Composable
private fun AlarmList(
    alarms: AlarmsContent?,
    padding: PaddingValues,
    listState: LazyListState,
    selected: Long?,
    scrollTarget: Long,
    onScrolled: () -> Unit,
    onOpen: (Long) -> Unit,
    onEvent: (AlarmEvent) -> Unit,
    onGroupLongPress: (Long) -> Unit,
) {
    when {
        alarms == null -> Unit
        alarms.isEmpty ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                PvotEmptyContent(
                    message = stringResource(R.string.no_alarms),
                    icon = {
                        Icon(
                            painterResource(R.drawable.ic_t_alarm),
                            contentDescription = null,
                            modifier = Modifier.size(PvotIconButtonDefaults.Size),
                        )
                    },
                )
            }
        else ->
            BoxWithConstraints {
                val width = maxWidth - PvotScreenDefaults.HorizontalPadding * 2
                val columns =
                    ((width + CardGap) / (CardMinWidth + CardGap)).toInt().coerceAtLeast(1)
                val entries = alarmListEntries(alarms, columns)
                val card =
                    @Composable { item: AlarmItem, large: Boolean, modifier: Modifier ->
                        AlarmCard(
                            item = item,
                            large = large,
                            selected = item.id == selected,
                            onClick = { onOpen(item.id) },
                            onEvent = onEvent,
                            modifier = modifier,
                        )
                    }
                LaunchedEffect(scrollTarget, columns) {
                    if (scrollTarget == Alarm.INVALID_ID) return@LaunchedEffect
                    val index = entries.indexOfAlarm(scrollTarget)
                    if (index >= 0) listState.animateScrollToItem(index)
                    onScrolled()
                }
                PvotScreen(
                    state = listState,
                    contentPadding = screenPadding(padding),
                    horizontalAlignment = Alignment.Start,
                ) {
                    itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                        val reveal = Modifier.pvotReveal(index)
                        when (entry) {
                            is AlarmListEntry.Title ->
                                SectionTitle(
                                    stringResource(
                                        when (entry.section) {
                                            AlarmListSection.WakeUp ->
                                                R.string.alarm_section_wake_up
                                            AlarmListSection.Others -> R.string.alarm_section_others
                                        }
                                    )
                                )
                            is AlarmListEntry.WakeUp -> card(entry.item, true, reveal)
                            is AlarmListEntry.GroupHeader ->
                                AlarmGroupHeader(
                                    section = entry.section,
                                    onEvent = onEvent,
                                    onLongPress = { onGroupLongPress(entry.section.group.id) },
                                    modifier = reveal,
                                )
                            is AlarmListEntry.Cards ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(CardGap),
                                    modifier = reveal,
                                ) {
                                    entry.items.forEach { card(it, false, Modifier.weight(1f)) }
                                    repeat(columns - entry.items.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                        }
                    }
                }
            }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = PvotSpacing.L).semantics { heading() },
    )
}

@Composable
private fun alarmsViewModel(): AlarmsViewModel {
    val context = LocalContext.current.applicationContext
    return viewModel { AlarmsViewModel(AlarmRepository(context)) }
}

private val CardMinWidth = 160.dp
private val CardGap = PvotSpacing.M
