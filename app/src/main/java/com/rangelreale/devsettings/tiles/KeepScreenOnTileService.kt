package com.rangelreale.devsettings.tiles

import android.service.quicksettings.Tile
import com.rangelreale.devsettings.R
import com.rangelreale.devsettings.keepon.KeepScreenOnDuration
import com.rangelreale.devsettings.keepon.KeepScreenOnService
import com.rangelreale.devsettings.keepon.KeepScreenOnState
import com.rangelreale.devsettings.keepon.formatRemaining
import com.rangelreale.devsettings.keepon.labelRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Each tap moves to the next duration: 5 min, 15 min, 30 min, 1 hour, indefinitely, off.
 * Each step restarts the countdown from the tap.
 */
class KeepScreenOnTileService : BaseTileService() {

    override fun onListening(scope: CoroutineScope) {
        scope.launch {
            KeepScreenOnState.status.collectLatest { status ->
                updateTile()
                // Refresh the remaining time while the panel is open.
                while (status?.endsAtMillis != null && isActive) {
                    delay(1_000)
                    updateTile()
                }
            }
        }
    }

    override fun onToggle() {
        val next = KeepScreenOnDuration.next(KeepScreenOnState.status.value?.duration)
        try {
            KeepScreenOnService.apply(this, next)
        } catch (e: IllegalStateException) {
            // Android refused the foreground service start (ForegroundServiceStartNotAllowedException).
            openApp()
        }
    }

    override fun render(tile: Tile) {
        val status = KeepScreenOnState.status.value
        if (status == null) {
            renderOnOff(tile, false)
            return
        }
        val remaining = status.remainingMillis(System.currentTimeMillis())
        val subtitle = if (remaining == null) {
            getString(status.duration.labelRes)
        } else {
            getString(R.string.tile_time_left, formatRemaining(remaining))
        }
        renderOnOff(tile, true, subtitle)
    }
}
