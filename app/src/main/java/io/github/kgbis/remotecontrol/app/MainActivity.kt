package io.github.kgbis.remotecontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kgbis.remotecontrol.app.core.ViewModelFactory
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel
import io.github.kgbis.remotecontrol.app.navigation.NavGraph
import io.github.kgbis.remotecontrol.app.ui.theme.RemotePcControlTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

const val MIN_VERSION = "2026.01.3"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as RemotePcControlApp

            val dispatcher: CoroutineDispatcher = Dispatchers.Main

            val settingsVm: SettingsViewModel = viewModel(
                factory = ViewModelFactory {
                    SettingsViewModel(app, app.settingsRepository)
                }
            )

            val devicesVm: DevicesViewModel = viewModel(
                factory = ViewModelFactory {
                    DevicesViewModel(
                        application = app,
                        deviceRepository = app.devicesRepository,
                        settingsRepository = app.settingsRepository,
                        networkMonitor = app.networkMonitor,
                        appLifecycleObserver = app.appLifecycleObserver,
                        dispatcher = dispatcher
                    )
                }
            )

            val themeMode by settingsVm.colorScheme.collectAsState()

            RemotePcControlTheme(themeMode) {
                NavGraph(settingsVm, devicesVm)
            }
        }
    }
}
