package io.github.kgbis.remotecontrol.app

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModelFactory
import io.github.kgbis.remotecontrol.app.navigation.NavGraph
import io.github.kgbis.remotecontrol.app.ui.theme.RemotePcControlTheme

const val MIN_VERSION = "2026.01.3"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = LocalContext.current.applicationContext as Application

            val settingsVm: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(
                    app = app,
                    repo = (app as RemotePcControlApp).settingsRepository
                )
            )
            val themeMode by settingsVm.colorScheme.collectAsState()

            RemotePcControlTheme(themeMode) {
                NavGraph(settingsVm)
            }
        }
    }
}
