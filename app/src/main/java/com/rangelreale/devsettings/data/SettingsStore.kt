package com.rangelreale.devsettings.data

/**
 * Minimal view of `Settings.Global` and the permission check.
 *
 * The production implementation is [AndroidSettingsStore]. Unit tests use a fake.
 */
interface SettingsStore {
    /** Returns the integer value of a global setting, or [default] when it is missing or unreadable. */
    fun getGlobalInt(key: String, default: Int): Int

    /** Writes a global setting. Returns false when the system rejects the write. */
    fun putGlobalInt(key: String, value: Int): Boolean

    /** True when the app holds `android.permission.WRITE_SECURE_SETTINGS`. */
    fun hasWriteSecureSettings(): Boolean
}
