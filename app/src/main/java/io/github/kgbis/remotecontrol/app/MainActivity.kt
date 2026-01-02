package io.github.kgbis.remotecontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.kgbis.remotecontrol.app.navigation.NavGraph
import io.github.kgbis.remotecontrol.app.ui.theme.RemotePcControlTheme

const val MIN_VERSION = "2025.12.18"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemotePcControlTheme {
                NavGraph()
            }
        }
    }
}
