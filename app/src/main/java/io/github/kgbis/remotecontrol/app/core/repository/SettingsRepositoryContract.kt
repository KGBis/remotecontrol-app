package io.github.kgbis.remotecontrol.app.core.repository

import io.github.kgbis.remotecontrol.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import java.time.temporal.ChronoUnit

interface SettingsRepositoryContract {

    val shutdownDelayFlow: Flow<Int>
    val shutdownUnitFlow: Flow<ChronoUnit>

    val theme: Flow<ThemeMode>

    val autoRefreshEnabledFlow: Flow<Boolean>

    val autoRefreshIntervalFlow: Flow<Int>

    suspend fun saveShutdownDelay(delay: Int)
    suspend fun saveShutdownUnit(unit: ChronoUnit)

    suspend fun saveTheme(theme: ThemeMode)

    suspend fun saveAutorefreshEnabled(status: Boolean)

    suspend fun saveAutorefreshDelay(delay: Float)
}
