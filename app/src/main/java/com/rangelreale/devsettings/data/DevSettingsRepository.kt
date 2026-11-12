package com.rangelreale.devsettings.data

enum class WriteResult { SUCCESS, MISSING_PERMISSION, FAILED }

/** Reads and writes the developer settings. Every read goes to the store; nothing is cached. */
class DevSettingsRepository(private val store: SettingsStore) {

    fun hasPermission(): Boolean = store.hasWriteSecureSettings()

    fun developerOptionsEnabled(): Boolean =
        store.getGlobalInt(GlobalKeys.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0

    /** Any non-zero value counts as on. For stay awake, this includes partial masks such as 1 (AC only). */
    fun isEnabled(setting: DevSetting): Boolean = store.getGlobalInt(setting.key, 0) != 0

    fun setEnabled(setting: DevSetting, enabled: Boolean): WriteResult {
        if (!hasPermission()) return WriteResult.MISSING_PERMISSION
        val value = if (enabled) setting.onValue else 0
        if (!store.putGlobalInt(setting.key, value)) return WriteResult.FAILED
        if (!setting.verifyWrite) return WriteResult.SUCCESS
        return if (store.getGlobalInt(setting.key, -1) == value) WriteResult.SUCCESS else WriteResult.FAILED
    }

    fun toggle(setting: DevSetting): WriteResult = setEnabled(setting, !isEnabled(setting))
}
