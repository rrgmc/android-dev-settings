package com.rangelreale.devsettings.keepon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live state of [KeepScreenOnService]. Null when the service is not running. */
object KeepScreenOnState {
    private val mutable = MutableStateFlow<KeepScreenOnStatus?>(null)
    val status: StateFlow<KeepScreenOnStatus?> = mutable.asStateFlow()

    internal fun set(value: KeepScreenOnStatus?) {
        mutable.value = value
    }
}
