package com.rangelreale.devsettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rangelreale.devsettings.data.AndroidSettingsStore
import com.rangelreale.devsettings.data.DevSettingsRepository
import com.rangelreale.devsettings.ui.DevSettingsTheme
import com.rangelreale.devsettings.ui.MainScreen

/**
 * The main screen. Tiles open it on long press, and on tap when the permission is missing.
 * The setup card sits at the top of the screen whenever the permission is missing.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = DevSettingsRepository(AndroidSettingsStore(applicationContext))
        setContent {
            DevSettingsTheme {
                MainScreen(repository)
            }
        }
    }
}
