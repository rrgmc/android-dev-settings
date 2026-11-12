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
    fun onlyPhoneSupportsKeepAwake() {
        assertTrue(DevicePlatform.PHONE.supportsKeepAwake)
        assertFalse(DevicePlatform.META_QUEST.supportsKeepAwake)
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
