package com.rangelreale.devsettings.keepon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeepScreenOnDurationTest {
    @Test
    fun tileCycleVisitsEveryDurationThenTurnsOff() {
        val seen = mutableListOf<KeepScreenOnDuration?>()
        var current: KeepScreenOnDuration? = null
        repeat(6) {
            current = KeepScreenOnDuration.next(current)
            seen += current
        }
        assertEquals(
            listOf(
                KeepScreenOnDuration.MIN_5,
                KeepScreenOnDuration.MIN_15,
                KeepScreenOnDuration.MIN_30,
                KeepScreenOnDuration.HOUR_1,
                KeepScreenOnDuration.INDEFINITE,
                null,
            ),
            seen,
        )
    }

    @Test
    fun durationsInMillis() {
        assertEquals(300_000L, KeepScreenOnDuration.MIN_5.millis)
        assertEquals(3_600_000L, KeepScreenOnDuration.HOUR_1.millis)
        assertNull(KeepScreenOnDuration.INDEFINITE.millis)
    }

    @Test
    fun remainingTimeNeverNegative() {
        val status = KeepScreenOnStatus(KeepScreenOnDuration.MIN_5, endsAtMillis = 1_000)
        assertEquals(400L, status.remainingMillis(600))
        assertEquals(0L, status.remainingMillis(5_000))
        assertNull(KeepScreenOnStatus(KeepScreenOnDuration.INDEFINITE, null).remainingMillis(0))
    }

    @Test
    fun formatsRemainingTime() {
        assertEquals("5:00", formatRemaining(300_000))
        assertEquals("0:01", formatRemaining(1))
        assertEquals("1:00:00", formatRemaining(3_600_000))
        assertEquals("14:59", formatRemaining(899_000))
    }
}
