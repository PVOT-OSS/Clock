/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock

import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import com.prauga.pvot.designsystem.theme.PvotSpacing
import java.util.Calendar
import java.util.TimeZone
import org.prauga.clock.R
import org.prauga.clock.Utils
import org.prauga.clock.data.City
import org.prauga.clock.data.DataModel
import org.prauga.clock.ui.ChoiceSheet
import org.prauga.clock.ui.SubScreen

/** Picks the cities shown as world clocks. */
class CitySelectionActivity : PvotBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPvotContent { PvotAppTheme { CitySelectionScreen(onBack = ::finish) } }
    }
}

/** Cities shown together on one card, under [title]; [index] is the fast scroller's label. */
private class Section(val title: String?, val index: String, val cities: List<City>)

@Composable
private fun CitySelectionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val model = DataModel.getDataModel()
    // Bumped when the list itself changes: sorting, or a place added.
    var version by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    // The cities chosen when the screen opened stay on top; checks are saved when it pauses.
    val original = remember(version) { model.selectedCities }
    val checked = remember(version) { original.toMutableStateList() }
    val save = { model.setSelectedCities(checked.toList()) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { save() }
    var addingPlace by remember { mutableStateOf(false) }

    val selectedLabel = stringResource(R.string.selected_cities_label)
    val sections =
        remember(version, query, original) {
            val text = City.removeSpecialCharacters(query.trim().uppercase())
            if (text.isNotEmpty()) {
                listOf(Section(null, "", model.unselectedCities.filter { it.matches(text) }))
            } else {
                buildList {
                    if (original.isNotEmpty()) add(Section(selectedLabel, "+", original))
                    model.unselectedCities
                        .groupBy { sectionOf(model, it) }
                        .forEach { (label, cities) -> add(Section(label, label, cities)) }
                }
            }
        }
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
            if (model.citySort == DataModel.CitySort.NAME) R.string.menu_item_sort_by_gmt_offset
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
                onClick = {
                    save()
                    model.toggleCitySort()
                    version++
                },
            )
            PvotTopBarAction(
                icon = painterResource(R.drawable.ic_t_map_pin_plus),
                contentDescription = stringResource(R.string.add_place),
                onClick = { addingPlace = true },
            )
        },
        listOverlay = {
            if (query.isBlank()) {
                PvotFastScroller(
                    state = state,
                    sections = scrollSections,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        },
    ) {
        item(key = "search") {
            PvotTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.search_cities),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(bottom = PvotSpacing.M),
            )
        }
        items(sections, key = { it.title ?: "results" }) { section ->
            PvotListGroup(
                title = section.title,
                modifier = Modifier.padding(bottom = PvotSpacing.L),
            ) {
                section.cities.forEach { city ->
                    item {
                        val on = city in checked
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
                            onClick = { if (on) checked.remove(city) else checked.add(city) },
                        )
                    }
                }
            }
        }
    }

    AddPlaceDialog(
        visible = addingPlace,
        onDismiss = { addingPlace = false },
        onAdd = { name, zone ->
            save()
            model.addCustomPlace(name, zone)
            addingPlace = false
            version++
        },
    )
}

private fun sectionOf(model: DataModel, city: City): String =
    when (model.citySort) {
        DataModel.CitySort.UTC_OFFSET -> Utils.getGMTHourOffset(city.timeZone, false)
        else -> city.indexString
    }

private fun timeIn(zone: TimeZone, twentyFour: Boolean): String {
    val calendar = Calendar.getInstance(zone)
    val skeleton = if (twentyFour) "Hm" else "hma"
    val pattern = DateFormat.getBestDateTimePattern(java.util.Locale.getDefault(), skeleton)
    return DateFormat.format(pattern, calendar).toString()
}

/** "Add place": a world clock with any name in any time zone, for places not in the list. */
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
