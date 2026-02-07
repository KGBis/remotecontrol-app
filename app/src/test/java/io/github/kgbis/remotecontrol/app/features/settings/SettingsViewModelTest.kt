package io.github.kgbis.remotecontrol.app.features.settings

import android.app.Application
import io.github.kgbis.remotecontrol.app.features.devices.model.shutdownDelayOptions
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `shutdown delay and unit are updated eagerly`() = runTest {
        // Given
        val fakeRepo = FakeSettingsRepository()
        val app = mockk<Application>(relaxed = true)

        val viewModel = SettingsViewModel(
            application = app,
            settingsRepo = fakeRepo
        )

        // Initial
        assertEquals(shutdownDelayOptions[0].amount, viewModel.shutdownDelay.value)
        assertEquals(shutdownDelayOptions[0].unit, viewModel.shutdownUnit.value)

        // When
        viewModel.changeDelay(10)
        viewModel.changeUnit(ChronoUnit.MINUTES)
        advanceUntilIdle()

        // Then
        assertEquals(10, viewModel.shutdownDelay.value)
        assertEquals(ChronoUnit.MINUTES, viewModel.shutdownUnit.value)
    }
}
