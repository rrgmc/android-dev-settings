package com.rangelreale.devsettings.data

/**
 * The kind of device the app runs on. Some features behave differently on a Meta Quest headset.
 *
 * Tested on a Quest 3 with Horizon OS on Android 14:
 * - `development_settings_enabled` is not set, although USB and wireless debugging work.
 * - The proximity sensor puts the headset to sleep about 20 seconds after it is taken off.
 *   Neither `stay_on_while_plugged_in` nor a screen wake lock prevents this, but the
 *   `prox_close` broadcast in [ProximityOverride] does.
 */
enum class DevicePlatform(
    /** True when `development_settings_enabled` reflects whether the debugging settings take effect. */
    val usesDeveloperOptionsFlag: Boolean,
    /** True when "Stay awake while charging" keeps the device awake. */
    val supportsStayAwake: Boolean,
    /** True when keeping the device awake also needs [ProximityOverride]. */
    val needsProximityOverride: Boolean,
) {
    PHONE(usesDeveloperOptionsFlag = true, supportsStayAwake = true, needsProximityOverride = false),
    META_QUEST(usesDeveloperOptionsFlag = false, supportsStayAwake = false, needsProximityOverride = true),
    ;

    companion object {
        /** System feature that every Meta Quest headset reports. */
        const val QUEST_FEATURE = "oculus.hardware.standalone_vr"

        /** [hasSystemFeature] is usually `PackageManager::hasSystemFeature`. */
        fun detect(hasSystemFeature: (String) -> Boolean): DevicePlatform =
            if (hasSystemFeature(QUEST_FEATURE)) META_QUEST else PHONE
    }
}

/**
 * Broadcasts that override the Quest's proximity sensor. They come from Meta's testing tools and
 * need no permission. Tested on a Quest 3 from an ordinary app.
 */
object ProximityOverride {
    /** Makes the headset act as if it is worn, so it does not sleep when taken off. */
    const val ACTION_KEEP_AWAKE = "com.oculus.vrpowermanager.prox_close"

    /** Hands control back to the proximity sensor. */
    const val ACTION_RELEASE = "com.oculus.vrpowermanager.automation_disable"

    /**
     * How often to send [ACTION_KEEP_AWAKE] again while it is wanted. Meta Quest Developer Hub's
     * override lapses about 10 minutes after it stops renewing it, so renew well within that.
     */
    const val RENEW_INTERVAL_MILLIS = 60_000L

    fun action(keepAwake: Boolean): String = if (keepAwake) ACTION_KEEP_AWAKE else ACTION_RELEASE
}

/** True when the app should warn that Developer options are off. */
fun DevSettingsRepository.shouldWarnDeveloperOptionsOff(platform: DevicePlatform): Boolean =
    platform.usesDeveloperOptionsFlag && !developerOptionsEnabled()
