/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.common

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.list.PvotListRow
import org.prauga.clock.R

@Composable
fun ChoiceSheet(
    visible: Boolean,
    title: String,
    entries: List<String>,
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    PvotBottomSheet(visible = visible, onDismissRequest = onDismiss, title = title) {
        val state = rememberLazyListState()
        LaunchedEffect(visible) { if (visible && selected > 0) state.scrollToItem(selected) }
        val maxHeight =
            with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } *
                MAX_HEIGHT_FRACTION
        LazyColumn(state = state, modifier = Modifier.heightIn(max = maxHeight)) {
            itemsIndexed(entries) { index, entry ->
                PvotListRow(
                    title = entry,
                    selected = index == selected,
                    trailing =
                        if (index == selected) {
                            {
                                Icon(
                                    painterResource(R.drawable.ic_t_check),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                    onClick = { onSelect(index) },
                )
            }
        }
    }
}

private const val MAX_HEIGHT_FRACTION = 0.6f
