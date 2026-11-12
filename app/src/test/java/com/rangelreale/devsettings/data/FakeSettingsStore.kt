package com.rangelreale.devsettings.data

/** In-memory [SettingsStore] for tests. */
class FakeSettingsStore(
    var permissionGranted: Boolean = true,
) : SettingsStore {
    val values = mutableMapOf<String, Int>()

    /** When set, writes succeed but store this value instead (simulates the system overriding a write). */
    var overrideWrite: Int? = null

    /** When true, every write is rejected. */
    var rejectWrites: Boolean = false

    val writes = mutableListOf<Pair<String, Int>>()

    override fun getGlobalInt(key: String, default: Int): Int = values[key] ?: default

    override fun putGlobalInt(key: String, value: Int): Boolean {
        if (!permissionGranted) throw SecurityException("WRITE_SECURE_SETTINGS not granted")
        if (rejectWrites) return false
        writes += key to value
        values[key] = overrideWrite ?: value
        return true
    }

    override fun hasWriteSecureSettings(): Boolean = permissionGranted
}
