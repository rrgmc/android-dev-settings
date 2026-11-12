package com.rangelreale.devsettings.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.rangelreale.devsettings.MainActivity
import com.rangelreale.devsettings.R
import com.rangelreale.devsettings.data.AndroidSettingsStore
import com.rangelreale.devsettings.data.DevSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * Shared tile behaviour.
 *
 * Without the permission, the tile shows "Setup needed" and a tap opens the app on the setup card.
 * `Tile.STATE_UNAVAILABLE` would grey the tile out, but Android does not deliver taps to
 * unavailable tiles, so the tile uses the inactive state with a subtitle instead.
 *
 * Long press opens the app through the `QS_TILE_PREFERENCES` intent filter on [MainActivity].
 */
abstract class BaseTileService : TileService() {

    protected val repository by lazy { DevSettingsRepository(AndroidSettingsStore(applicationContext)) }

    /** `Settings.Global` keys whose changes refresh the tile while it is visible. */
    protected open val observedKeys: List<String> = emptyList()

    private var scope: CoroutineScope? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        observedKeys.forEach {
            contentResolver.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }
        scope = MainScope().also { onListening(it) }
        updateTile()
    }

    override fun onStopListening() {
        contentResolver.unregisterContentObserver(observer)
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (!repository.hasPermission()) {
            openApp()
            return
        }
        onToggle()
        updateTile()
    }

    /** Called while the tile is visible, for subclasses that observe more than settings. */
    protected open fun onListening(scope: CoroutineScope) = Unit

    /** Toggles the setting. Called only when the permission is held. */
    protected abstract fun onToggle()

    /** Sets state, subtitle and description for a tile whose permission is held. */
    protected abstract fun render(tile: Tile)

    protected fun updateTile() {
        val tile = qsTile ?: return
        if (repository.hasPermission()) {
            render(tile)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.tile_setup_needed)
            tile.stateDescription = getString(R.string.tile_setup_needed)
        }
        tile.updateTile()
    }

    protected fun renderOnOff(tile: Tile, on: Boolean, onSubtitle: String = getString(R.string.status_on)) {
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (on) onSubtitle else getString(R.string.status_off)
        tile.stateDescription = tile.subtitle
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    protected fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
