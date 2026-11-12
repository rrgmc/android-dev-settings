package com.rangelreale.devsettings.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePlatformTest {
    private val store = FakeSettingsStore()
    private val repo = DevSettingsRepository(store)

    @Test
    fun questFeatureMeansMetaQuest() {
        val platform = DevicePlatform.detect { it == "oculus.hardware.standalone_vr" }
        assertEquals(DevicePlatform.META_QUEST, platform)
    }

    @Test
    fun otherFeaturesMeanPhone() {
        assertEquals(DevicePlatform.PHONE, DevicePlatform.detect { false })
        assertEquals(DevicePlatform.PHONE, DevicePlatform.detect { it == "android.hardware.telephony" })
    }

    @Test
    fun onlyPhoneSupportsStayAwake() {
        assertTrue(DevicePlatform.PHONE.supportsStayAwake)
        assertFalse(DevicePlatform.META_QUEST.supportsStayAwake)
    }

    @Test
    fun onlyQuestNeedsProximityOverride() {
        assertFalse(DevicePlatform.PHONE.needsProximityOverride)
        assertTrue(DevicePlatform.META_QUEST.needsProximityOverride)
    }

    @Test
    fun proximityOverrideActions() {
        assertEquals("com.oculus.vrpowermanager.prox_close", ProximityOverride.action(keepAwake = true))
        assertEquals("com.oculus.vrpowermanager.automation_disable", ProximityOverride.action(keepAwake = false))
    }

    @Test
    fun proximityOverrideRenewsWellBeforeItLapses() {
        assertTrue(ProximityOverride.RENEW_INTERVAL_MILLIS in 1..5 * 60_000L)
    }

    @Test
    fun phoneWarnsWhenDeveloperOptionsAreOff() {
        assertTrue(repo.shouldWarnDeveloperOptionsOff(DevicePlatform.PHONE))
        store.values["development_settings_enabled"] = 1
        assertFalse(repo.shouldWarnDeveloperOptionsOff(DevicePlatform.PHONE))
    }

    @Test
    fun questNeverWarnsAboutDeveloperOptions() {
        assertFalse(repo.shouldWarnDeveloperOptionsOff(DevicePlatform.META_QUEST))
    }
}
