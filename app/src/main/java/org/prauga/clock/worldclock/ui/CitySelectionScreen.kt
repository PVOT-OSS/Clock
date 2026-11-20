/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.list.PvotFastScrollSection
import com.prauga.pvot.designsystem.components.list.PvotFastScroller
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.components.list.PvotPreferenceRow
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.util.Calendar
import java.util.TimeZone
import org.prauga.clock.R
import org.prauga.clock.common.CenteredContent
import org.prauga.clock.common.ChoiceSheet
import org.prauga.clock.common.SubScreen
import org.prauga.clock.data.DataModel

@Composable
fun CitySelectionScreen(
    onBack: () -> Unit,
    viewModel: CitySelectionViewModel = viewModel {
        CitySelectionViewModel(DataModel.getDataModel())
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onEvent(CitySelectionEvent.Save) }
    val content = (state as? UiState.Success)?.data ?: return
    CitySelectionBody(content, onBack, viewModel::onEvent)
}

@Composable
private fun CitySelectionBody(
    content: CitySelectionContent,
    onBack: () -> Unit,
    onEvent: (CitySelectionEvent) -> Unit,
) {
    val context = LocalContext.current
    var addingPlace by remember { mutableStateOf(false) }
    val selectedLabel = stringResource(R.string.selected_cities_label)
    val query = content.query
    val sections = content.sections
    // The search field is item 0, so section n is list item n + 1.
    val scrollSections =
        remember(sections) {
            sections.mapIndexed { index, section ->
                PvotFastScrollSection(section.index, index + 1)
            }
        }
    val state = rememberLazyListState()
    val twentyFour = DateFormat.is24HourFormat(context)
    val sortLabel =
        stringResource(
            if (content.sortedByName) R.string.menu_item_sort_by_gmt_offset
            else R.string.menu_item_sort_by_name
        )

    SubScreen(
        title = stringResource(R.string.cities_activity_title),
        onBack = onBack,
        verticalArrangement = Arrangement.Top,
        state = state,
        actions = {
            PvotTopBarAction(
                icon = painterResource(R.drawable.ic_t_arrows_sort),
                contentDescription = sortLabel,
                onClick = { onEvent(CitySelectionEvent.ToggleSort) },
            )
            PvotTopBarAction(
                icon = painterResource(R.drawable.ic_t_map_pin_plus),
                contentDescription = stringResource(R.string.add_place),
                onClick = { addingPlace = true },
            )
        },
        listOverlay = {
            if (query.isBlank()) {
                CenteredContent(Modifier.fillMaxHeight()) {
                    Box(Modifier.fillMaxSize()) {
                        PvotFastScroller(
                            state = state,
                            sections = scrollSections,
                            modifier = Modifier.align(Alignment.CenterEnd),
                        )
                    }
                }
            }
        },
    ) {
        item(key = "search") {
            CenteredContent {
                PvotTextField(
                    value = query,
                    onValueChange = { onEvent(CitySelectionEvent.Search(it)) },
                    placeholder = stringResource(R.string.search_cities),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().padding(bottom = PvotSpacing.M),
                )
            }
        }
        items(sections, key = { if (content.query.isBlank()) it.index else "results" }) { section ->
            CenteredContent {
                PvotListGroup(
                    title =
                        when {
                            query.isNotBlank() -> null
                            section.selected -> selectedLabel
                            else -> section.index
                        },
                    modifier = Modifier.padding(bottom = PvotSpacing.L),
                ) {
                    section.cities.forEach { city ->
                        item {
                            val on = city in content.checked
                            PvotListRow(
                                title = city.name,
                                supportingText = timeIn(city.timeZone, twentyFour),
                                selected = on,
                                trailing =
                                    if (on) {
                                        {
                                            Icon(
                                                painterResource(R.drawable.ic_t_check),
                                                contentDescription = null,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                onClick = { onEvent(CitySelectionEvent.Toggle(city)) },
                            )
                        }
                    }
                }
            }
        }
    }

    AddPlaceDialog(
        visible = addingPlace,
        onDismiss = { addingPlace = false },
        onAdd = { name, zone ->
            onEvent(CitySelectionEvent.AddPlace(name, zone))
            addingPlace = false
        },
    )
}

private fun timeIn(zone: TimeZone, twentyFour: Boolean): String {
    val calendar = Calendar.getInstance(zone)
    val skeleton = if (twentyFour) "Hm" else "hma"
    val pattern = DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), skeleton)
    return DateFormat.format(pattern, calendar).toString()
}

@Composable
private fun AddPlaceDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
) {
    val zones = remember {
        TimeZone.getAvailableIDs()
            .filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
            .sortedWith(compareBy({ TimeZone.getTimeZone(it).rawOffset }, { it }))
    }
    val labels = remember(zones) { zones.map { "${offset(it)}  ${it.replace('_', ' ')}" } }
    var name by remember(visible) { mutableStateOf("") }
    var zone by
        remember(visible) {
            mutableIntStateOf(zones.indexOf(TimeZone.getDefault().id).coerceAtLeast(0))
        }
    var picking by remember { mutableStateOf(false) }
    PvotDialog(
        visible = visible && !picking,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.add_place),
        content = {
            Column {
                PvotTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.add_place_name),
                    singleLine = true,
                )
                PvotPreferenceRow(
                    title = stringResource(R.string.add_place_zone),
                    value = labels[zone],
                    onClick = { picking = true },
                )
            }
        },
        actions = {
            PvotDialogAction(
                label = stringResource(R.string.add_place_add),
                onClick = { if (name.isNotBlank()) onAdd(name.trim(), zones[zone]) },
                style = PvotDialogActionStyle.Primary,
            )
            PvotDialogAction(label = stringResource(android.R.string.cancel), onClick = onDismiss)
        },
    )
    ChoiceSheet(
        visible = visible && picking,
        title = stringResource(R.string.add_place_zone),
        entries = labels,
        selected = zone,
        onDismiss = { picking = false },
        onSelect = {
            zone = it
            picking = false
        },
    )
}

private fun offset(zone: String): String {
    val minutes = TimeZone.getTimeZone(zone).rawOffset / 60_000
    val sign = if (minutes < 0) "−" else "+"
    return "GMT$sign%d:%02d".format(Math.abs(minutes) / 60, Math.abs(minutes) % 60)
}
