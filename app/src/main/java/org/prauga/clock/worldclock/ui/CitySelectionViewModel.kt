/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.worldclock.ui

import androidx.lifecycle.ViewModel
import com.prauga.pvot.coreui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.prauga.clock.Utils
import org.prauga.clock.data.City
import org.prauga.clock.data.DataModel

class CitySelectionViewModel(private val model: DataModel) : ViewModel() {

    private var original: List<City> = model.selectedCities
    private var checked: Set<City> = original.toSet()
    private var query = ""

    private val content = MutableStateFlow<UiState<CitySelectionContent>>(UiState.Success(build()))
    val state: StateFlow<UiState<CitySelectionContent>> = content.asStateFlow()

    fun onEvent(event: CitySelectionEvent) {
        when (event) {
            is CitySelectionEvent.Search -> query = event.query
            is CitySelectionEvent.Toggle ->
                checked = if (event.city in checked) checked - event.city else checked + event.city
            CitySelectionEvent.ToggleSort -> {
                save()
                model.toggleCitySort()
                reload()
            }
            is CitySelectionEvent.AddPlace -> {
                save()
                model.addCustomPlace(event.name, event.zoneId)
                reload()
            }
            CitySelectionEvent.Save -> save()
        }
        content.value = UiState.Success(build())
    }

    private fun save() = model.setSelectedCities(checked.toList())

    private fun reload() {
        original = model.selectedCities
        checked = original.toSet()
    }

    private fun build(): CitySelectionContent {
        val text = City.removeSpecialCharacters(query.trim().uppercase())
        val sections =
            if (text.isNotEmpty()) {
                listOf(CitySection("", model.unselectedCities.filter { it.matches(text) }))
            } else {
                buildList {
                    if (original.isNotEmpty()) add(CitySection(SELECTED_INDEX, original, true))
                    model.unselectedCities.groupBy(::sectionOf).forEach { (label, cities) ->
                        add(CitySection(label, cities))
                    }
                }
            }
        return CitySelectionContent(
            query = query,
            sections = sections,
            checked = checked,
            sortedByName = model.citySort == DataModel.CitySort.NAME,
        )
    }

    private fun sectionOf(city: City): String =
        when (model.citySort) {
            DataModel.CitySort.UTC_OFFSET -> Utils.getGMTHourOffset(city.timeZone, false)
            DataModel.CitySort.NAME -> city.indexString
        }

    private companion object {
        const val SELECTED_INDEX = "+"
    }
}
