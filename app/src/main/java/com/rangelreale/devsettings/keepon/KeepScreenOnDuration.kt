package com.rangelreale.devsettings.keepon

import java.util.concurrent.TimeUnit

/** The fixed durations offered for "Keep screen on". [minutes] is null for "indefinitely". */
enum class KeepScreenOnDuration(val minutes: Long?) {
    MIN_5(5),
    MIN_15(15),
    MIN_30(30),
    HOUR_1(60),
    INDEFINITE(null);

    val millis: Long? get() = minutes?.let { TimeUnit.MINUTES.toMillis(it) }

    companion object {
        /**
         * The next step of the tile cycle: off, 5 min, 15 min, 30 min, 1 hour, indefinitely, off.
         * Null means off.
         */
        fun next(current: KeepScreenOnDuration?): KeepScreenOnDuration? = when (current) {
            null -> entries.first()
            entries.last() -> null
            else -> entries[current.ordinal + 1]
        }
    }
}

/** A running keep-screen-on session. [endsAtMillis] is wall-clock time, null when indefinite. */
data class KeepScreenOnStatus(val duration: KeepScreenOnDuration, val endsAtMillis: Long?) {
    fun remainingMillis(nowMillis: Long): Long? = endsAtMillis?.let { (it - nowMillis).coerceAtLeast(0) }
}

/** Formats a remaining time as `h:mm:ss` or `m:ss`. */
fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis + 999) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
