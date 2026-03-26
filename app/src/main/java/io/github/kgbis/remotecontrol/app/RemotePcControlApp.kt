/*
 * Remote PC Control
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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
