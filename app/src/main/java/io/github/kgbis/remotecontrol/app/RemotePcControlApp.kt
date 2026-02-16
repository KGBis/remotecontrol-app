package io.github.kgbis.remotecontrol.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.kgbis.remotecontrol.app.core.AppLifecycleObserverImpl
import io.github.kgbis.remotecontrol.app.core.network.NetworkMonitor
import io.github.kgbis.remotecontrol.app.core.network.NetworkMonitorImpl
import io.github.kgbis.remotecontrol.app.core.network.NetworkRangeDetector
import io.github.kgbis.remotecontrol.app.core.repository.DeviceRepositoryImpl
import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class RemotePcControlApp : Application() {

    lateinit var appLifecycleObserver: AppLifecycleObserverImpl
        private set

    lateinit var settingsRepository: SettingsRepositoryImpl
        private set

    lateinit var devicesRepository: DeviceRepositoryImpl
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    lateinit var appScope: CoroutineScope
        private set

    override fun onCreate() {
        super.onCreate()

        // setup repositories
        settingsRepository = SettingsRepositoryImpl(this)
        devicesRepository = DeviceRepositoryImpl(this)

        // Network Monitor
        appScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        )

        networkMonitor = NetworkMonitorImpl(
            context = this,
            scope = appScope,
            networkRangeDetector = NetworkRangeDetector()
        )

        // Lifecycle observer
        appLifecycleObserver = AppLifecycleObserverImpl()
        ProcessLifecycleOwner
            .get()
            .lifecycle
            .addObserver(appLifecycleObserver)
    }
}
