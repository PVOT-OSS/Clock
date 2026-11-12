/*
 * SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.prauga.clock.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.prauga.clock.R
import org.prauga.clock.data.DataModel
import org.prauga.clock.data.StopwatchListener

/** Quick Settings tile that starts and pauses the stopwatch without opening the app. */
class StopwatchTileService : TileService() {

    private val listener = StopwatchListener { _, _ -> updateTile() }

    override fun onStartListening() {
        DataModel.getDataModel().addStopwatchListener(listener)
        updateTile()
    }

    override fun onStopListening() {
        DataModel.getDataModel().removeStopwatchListener(listener)
    }

    override fun onClick() {
        val model = DataModel.getDataModel()
        if (model.stopwatch.isRunning) model.pauseStopwatch() else model.startStopwatch()
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val stopwatch = DataModel.getDataModel().stopwatch
        tile.state = if (stopwatch.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle =
            getString(
                when {
                    stopwatch.isRunning -> R.string.tile_stopwatch_running
                    stopwatch.isPaused -> R.string.swn_paused
                    else -> R.string.tile_stopwatch_start
                }
            )
        tile.updateTile()
    }
}
