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
package io.github.kgbis.remotecontrol.app.support

import android.app.Application
import io.github.kgbis.remotecontrol.app.core.AppLifecycleObserver
import io.github.kgbis.remotecontrol.app.core.network.NetworkMonitor
import io.github.kgbis.remotecontrol.app.core.repository.DeviceRepository
import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepository
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlin.time.Duration

class TestDevicesViewModel(
    application: Application = mockk(),
    deviceRepository: DeviceRepository = FakeDeviceRepository(),
    settingsRepository: SettingsRepository = FakeSettingsRepository(),
    networkMonitor: NetworkMonitor = FakeNetworkMonitor(),
    appLifecycleObserver: AppLifecycleObserver = FakeAppLifecycleObserver(),
    dispatcher: CoroutineDispatcher
) : DevicesViewModel(
    application,
    deviceRepository,
    settingsRepository,
    networkMonitor,
    appLifecycleObserver,
    dispatcher
) {
    var probeCalls = 0
        private set

    override fun probeDevices() {
        probeCalls++
        super.probeDevices()
    }

    var tickerEmits = 1

    override fun tickerFlow(period: Duration) = flow {
        repeat(tickerEmits) { emit(Unit) }
    }.onStart {
        // In general, the first extra emission is quite common with combine + StateFlow
        // so reset counter to fix it for tests
        probeCalls = 0
    }

}
