package com.rangelreale.devsettings.data

/**
 * A developer setting stored as an integer in `Settings.Global`.
 *
 * The keys are literal strings so the logic runs in plain JVM unit tests.
 * They match the `Settings.Global` constants named in each comment.
 */
enum class DevSetting(val key: String, val onValue: Int) {
    /** `Settings.Global.ADB_ENABLED`. */
    USB_DEBUGGING("adb_enabled", 1),

    /** `adb_wifi_enabled` (hidden constant `Settings.Global.ADB_WIFI_ENABLED`). */
    WIRELESS_DEBUGGING("adb_wifi_enabled", 1),

    /**
     * `Settings.Global.STAY_ON_WHILE_PLUGGED_IN`. The value is a bit mask:
     * 1 = AC, 2 = USB, 4 = wireless. "On" sets all three (7).
     */
    STAY_AWAKE("stay_on_while_plugged_in", 7),
}

object GlobalKeys {
    /** `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`. */
    const val DEVELOPMENT_SETTINGS_ENABLED = "development_settings_enabled"
}

const val WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS"

/** The command that grants the permission from a computer. */
fun grantCommand(packageName: String): String =
    "adb shell pm grant $packageName $WRITE_SECURE_SETTINGS"
