package com.rangelreale.devsettings.keepon

import androidx.annotation.StringRes
import com.rangelreale.devsettings.R

@get:StringRes
val KeepScreenOnDuration.labelRes: Int
    get() = when (this) {
        KeepScreenOnDuration.MIN_5 -> R.string.duration_5_min
        KeepScreenOnDuration.MIN_15 -> R.string.duration_15_min
        KeepScreenOnDuration.MIN_30 -> R.string.duration_30_min
        KeepScreenOnDuration.HOUR_1 -> R.string.duration_1_hour
        KeepScreenOnDuration.INDEFINITE -> R.string.duration_indefinite
    }
