package com.rangelreale.devsettings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Light theme. Uses the wallpaper colours (dynamic color, Android 12+) when available. */
@Composable
fun DevSettingsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        dynamicLightColorScheme(context)
    } else {
        lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
