package com.rangelreale.devsettings.tiles

import android.service.quicksettings.Tile
import com.rangelreale.devsettings.R
import com.rangelreale.devsettings.data.DevSetting

/** A tile that toggles one [DevSetting]. */
abstract class SettingTileService(private val setting: DevSetting) : BaseTileService() {

    override val observedKeys = listOf(setting.key)

    /** Debugging tiles ask for an unlock first, so a locked phone cannot open an adb port. */
    protected open val requiresUnlock = false

    override fun onToggle() {
        if (requiresUnlock && isLocked) {
            unlockAndRun {
                repository.toggle(setting)
                updateTile()
            }
        } else {
            repository.toggle(setting)
        }
    }

    override fun render(tile: Tile) = renderOnOff(tile, repository.isEnabled(setting))
}

class UsbDebuggingTileService : SettingTileService(DevSetting.USB_DEBUGGING) {
    override val requiresUnlock = true
}

class WirelessDebuggingTileService : SettingTileService(DevSetting.WIRELESS_DEBUGGING) {
    override val requiresUnlock = true
}

class StayAwakeTileService : SettingTileService(DevSetting.STAY_AWAKE) {
    override fun render(tile: Tile) =
        renderOnOff(tile, repository.isEnabled(DevSetting.STAY_AWAKE), getString(R.string.stay_awake_on_detail))
}
