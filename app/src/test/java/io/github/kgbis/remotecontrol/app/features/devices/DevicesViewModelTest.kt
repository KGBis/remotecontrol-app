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
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.kgbis.remotecontrol.app.features.devices

import android.util.Log
import io.github.kgbis.remotecontrol.app.core.AppVisibilityEvent
import io.github.kgbis.remotecontrol.app.core.network.NetworkInfo
import io.github.kgbis.remotecontrol.app.support.FakeAppLifecycleObserver
import io.github.kgbis.remotecontrol.app.support.FakeNetworkMonitor
import io.github.kgbis.remotecontrol.app.support.FakeSettingsRepository
import io.github.kgbis.remotecontrol.app.support.TestDevicesViewModel
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock for the Log static class
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(String::class), any(String::class)) } returns 0
        every { Log.e(any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial devices are loaded from repository`() = runTest {
        val vm = TestDevicesViewModel(dispatcher = testDispatcher)

        advanceUntilIdle()

        assertEquals(3, vm.devices.value.size)
    }

    @Test
    fun `network status is refreshed only in foreground`() = runTest {
        val viewModel = TestDevicesViewModel(dispatcher = testDispatcher)
        val fakeNetMon = viewModel.networkMonitor as FakeNetworkMonitor
        val fakeLifecycle = viewModel.appLifecycleObserver as FakeAppLifecycleObserver

        advanceUntilIdle()

        // Background -> foreground
        fakeLifecycle.setForeground(true)
        fakeLifecycle.sendVisibilityEvent(AppVisibilityEvent.Foreground)

        advanceUntilIdle()

        assertEquals(1, fakeNetMon.refreshCalls)
    }

    @Test
    fun `network status is NOT refreshed if app never goes to foreground`() = runTest {
        val viewModel = TestDevicesViewModel(dispatcher = testDispatcher)
        val fakeNetMon = viewModel.networkMonitor as FakeNetworkMonitor

        advanceUntilIdle()

        // DO NOT change to  AppVisibilityEvent.Foreground
        // DO NOT change to lifecycle.setForeground(true)

        advanceUntilIdle()

        assertEquals(0, fakeNetMon.refreshCalls)
    }

    @Test
    fun `device auto-refresh runs only when all conditions are met`() = runTest {
        val viewModel = TestDevicesViewModel(dispatcher = testDispatcher)

        val fakeNetMon = viewModel.networkMonitor as FakeNetworkMonitor
        val fakeLifecycle = viewModel.appLifecycleObserver as FakeAppLifecycleObserver
        val fakeSettingsRepo = viewModel.settingsRepository as FakeSettingsRepository


        // Initial state: nothing should happen
        advanceUntilIdle()
        assertEquals(0, viewModel.probeCalls)

        // 1️⃣ App in foreground
        fakeLifecycle.setForeground(true)

        // 2️⃣ Main screen visible
        viewModel.setMainScreenVisible(true)

        // 3️⃣ in Local network
        fakeNetMon.setNetworkInfo(NetworkInfo.Local(subnet = "192.168.1"))

        // 4️⃣ Auto refresh active
        fakeSettingsRepo.saveAutorefreshEnabled(true)

        advanceUntilIdle()

        assertTrue(viewModel.probeCalls > 0)
        assertEquals(1, viewModel.probeCalls)
    }

    @Test
    fun `device auto-refresh runs multiple times with multiple ticks`() = runTest {
        val viewModel = TestDevicesViewModel(dispatcher = testDispatcher)

        // We set conditions for auto-refresh to run
        val fakeLifecycle = viewModel.appLifecycleObserver as FakeAppLifecycleObserver
        val fakeNM = viewModel.networkMonitor as FakeNetworkMonitor
        val fakeSettings = viewModel.settingsRepository as FakeSettingsRepository

        fakeLifecycle.setForeground(true)
        viewModel.setMainScreenVisible(true)
        fakeNM.setNetworkInfo(NetworkInfo.Local(subnet = "192.168.1"))
        fakeSettings.saveAutorefreshEnabled(true)

        // three ticker "ticks" (three refresh cycles)
        viewModel.tickerEmits = 3

        advanceUntilIdle()

        println(viewModel.probeCalls)

        // Assert: probeDevices called three times
        assertEquals(3, viewModel.probeCalls)
    }


}