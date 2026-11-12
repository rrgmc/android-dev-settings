package com.rangelreale.devsettings.data

/**
 * The kind of device the app runs on. Some features behave differently on a Meta Quest headset.
 *
 * Tested on a Quest 3 with Horizon OS on Android 14:
 * - `development_settings_enabled` is not set, although USB and wireless debugging work.
 * - The proximity sensor puts the headset to sleep about 20 seconds after it is taken off.
 *   Neither `stay_on_while_plugged_in` nor a screen wake lock prevents this.
 */
enum class DevicePlatform(
    /** True when `development_settings_enabled` reflects whether the debugging settings take effect. */
    val usesDeveloperOptionsFlag: Boolean,
    /** True when "Stay awake while charging" and "Keep screen on" keep the device awake. */
    val supportsKeepAwake: Boolean,
) {
    PHONE(usesDeveloperOptionsFlag = true, supportsKeepAwake = true),
    META_QUEST(usesDeveloperOptionsFlag = false, supportsKeepAwake = false),
    ;

    companion object {
        /** System feature that every Meta Quest headset reports. */
        const val QUEST_FEATURE = "oculus.hardware.standalone_vr"

        /** [hasSystemFeature] is usually `PackageManager::hasSystemFeature`. */
        fun detect(hasSystemFeature: (String) -> Boolean): DevicePlatform =
            if (hasSystemFeature(QUEST_FEATURE)) META_QUEST else PHONE
    }
}

/** True when the app should warn that Developer options are off. */
fun DevSettingsRepository.shouldWarnDeveloperOptionsOff(platform: DevicePlatform): Boolean =
    platform.usesDeveloperOptionsFlag && !developerOptionsEnabled()
