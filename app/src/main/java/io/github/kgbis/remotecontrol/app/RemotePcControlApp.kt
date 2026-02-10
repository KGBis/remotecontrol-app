package io.github.kgbis.remotecontrol.app

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.kgbis.remotecontrol.app.core.AppLifecycleObserver
import io.github.kgbis.remotecontrol.app.core.repository.DeviceRepository
import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepository

class RemotePcControlApp : Application() {

    lateinit var appLifecycleObserver: AppLifecycleObserver
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var devicesRepository: DeviceRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // setup repositories
        settingsRepository = SettingsRepository(this)
        devicesRepository = DeviceRepository(this)

        appLifecycleObserver = AppLifecycleObserver()
        ProcessLifecycleOwner
            .get()
            .lifecycle
            .addObserver(appLifecycleObserver)
    }
}
