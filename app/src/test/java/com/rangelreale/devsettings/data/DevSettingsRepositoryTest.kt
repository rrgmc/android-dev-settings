package com.rangelreale.devsettings.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevSettingsRepositoryTest {
    private val store = FakeSettingsStore()
    private val repo = DevSettingsRepository(store)

    @Test
    fun missingValuesReadAsOff() {
        DevSetting.entries.forEach { assertFalse(repo.isEnabled(it)) }
        assertFalse(repo.developerOptionsEnabled())
    }

    @Test
    fun readsDeveloperOptionsFlag() {
        store.values["development_settings_enabled"] = 1
        assertTrue(repo.developerOptionsEnabled())
    }

    @Test
    fun readsEachSettingFromItsKey() {
        store.values["adb_enabled"] = 1
        assertTrue(repo.isEnabled(DevSetting.USB_DEBUGGING))
        assertFalse(repo.isEnabled(DevSetting.WIRELESS_DEBUGGING))
        store.values["adb_wifi_enabled"] = 1
        assertTrue(repo.isEnabled(DevSetting.WIRELESS_DEBUGGING))
    }

    @Test
    fun stayAwakeWithPartialMaskReadsAsOn() {
        store.values["stay_on_while_plugged_in"] = 1
        assertTrue(repo.isEnabled(DevSetting.STAY_AWAKE))
    }

    @Test
    fun stayAwakeOnWritesAllChargingSources() {
        assertEquals(WriteResult.SUCCESS, repo.setEnabled(DevSetting.STAY_AWAKE, true))
        assertEquals(7, store.values["stay_on_while_plugged_in"])
    }

    @Test
    fun stayAwakeOffWritesZero() {
        store.values["stay_on_while_plugged_in"] = 7
        assertEquals(WriteResult.SUCCESS, repo.setEnabled(DevSetting.STAY_AWAKE, false))
        assertEquals(0, store.values["stay_on_while_plugged_in"])
    }

    @Test
    fun debuggingSettingsWriteOneAndZero() {
        repo.setEnabled(DevSetting.USB_DEBUGGING, true)
        repo.setEnabled(DevSetting.WIRELESS_DEBUGGING, true)
        assertEquals(1, store.values["adb_enabled"])
        assertEquals(1, store.values["adb_wifi_enabled"])
        repo.setEnabled(DevSetting.WIRELESS_DEBUGGING, false)
        assertEquals(0, store.values["adb_wifi_enabled"])
    }

    @Test
    fun toggleFlipsCurrentValue() {
        store.values["adb_enabled"] = 1
        assertEquals(WriteResult.SUCCESS, repo.toggle(DevSetting.USB_DEBUGGING))
        assertEquals(0, store.values["adb_enabled"])
        repo.toggle(DevSetting.USB_DEBUGGING)
        assertEquals(1, store.values["adb_enabled"])
    }

    @Test
    fun writeWithoutPermissionIsRefusedWithoutTouchingTheStore() {
        store.permissionGranted = false
        assertFalse(repo.hasPermission())
        assertEquals(WriteResult.MISSING_PERMISSION, repo.setEnabled(DevSetting.USB_DEBUGGING, true))
        assertTrue(store.writes.isEmpty())
    }

    @Test
    fun rejectedWriteReportsFailure() {
        store.rejectWrites = true
        assertEquals(WriteResult.FAILED, repo.setEnabled(DevSetting.USB_DEBUGGING, true))
    }

    @Test
    fun writeThatDoesNotStickReportsFailure() {
        store.overrideWrite = 0
        assertEquals(WriteResult.FAILED, repo.setEnabled(DevSetting.WIRELESS_DEBUGGING, true))
    }

    @Test
    fun readsAreNotCached() {
        assertFalse(repo.isEnabled(DevSetting.USB_DEBUGGING))
        store.values["adb_enabled"] = 1 // changed outside the app, e.g. in system Settings
        assertTrue(repo.isEnabled(DevSetting.USB_DEBUGGING))
    }

    @Test
    fun grantCommandContainsPackageName() {
        assertEquals(
            "adb shell pm grant com.example.app android.permission.WRITE_SECURE_SETTINGS",
            grantCommand("com.example.app"),
        )
    }
}
