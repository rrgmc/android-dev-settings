package com.rangelreale.devsettings.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/** [SettingsStore] backed by the real `Settings.Global` table. */
class AndroidSettingsStore(context: Context) : SettingsStore {
    private val context = context.applicationContext

    override fun getGlobalInt(key: String, default: Int): Int =
        try {
            Settings.Global.getInt(context.contentResolver, key, default)
        } catch (e: SecurityException) {
            default
        }

    override fun putGlobalInt(key: String, value: Int): Boolean =
        try {
            Settings.Global.putInt(context.contentResolver, key, value)
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

    override fun hasWriteSecureSettings(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED
}
