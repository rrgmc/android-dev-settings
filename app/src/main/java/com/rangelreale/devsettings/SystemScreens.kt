package com.rangelreale.devsettings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings

/** Intents into the system Settings app. */
object SystemScreens {

    /** Settings key of the wireless debugging row inside Developer options. */
    private const val WIRELESS_DEBUGGING_PREF_KEY = "toggle_adb_wireless"
    private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
    private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

    fun openDeveloperOptions(context: Context) =
        launch(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))

    /**
     * Android has no public intent for the wireless debugging screen. This opens Developer options
     * scrolled to the wireless debugging row, highlighted. Tap the row title to reach pairing.
     */
    fun openWirelessDebugging(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .putExtra(EXTRA_FRAGMENT_ARG_KEY, WIRELESS_DEBUGGING_PREF_KEY)
            .putExtra(
                EXTRA_SHOW_FRAGMENT_ARGUMENTS,
                Bundle().apply { putString(EXTRA_FRAGMENT_ARG_KEY, WIRELESS_DEBUGGING_PREF_KEY) },
            )
        launch(context, intent)
    }

    fun openAboutPhone(context: Context) = launch(context, Intent(Settings.ACTION_DEVICE_INFO_SETTINGS))

    private fun launch(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
