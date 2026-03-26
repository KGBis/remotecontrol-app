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

import io.github.kgbis.remotecontrol.app.core.AppLifecycleObserver
import io.github.kgbis.remotecontrol.app.core.AppVisibilityEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAppLifecycleObserver : AppLifecycleObserver {

    private val _isForeground = MutableStateFlow(false)
    override val isForegroundFlow: StateFlow<Boolean> = _isForeground

    private val _visibilityEvents = MutableSharedFlow<AppVisibilityEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val visibilityEvents: SharedFlow<AppVisibilityEvent> = _visibilityEvents

    fun setForeground(foreground: Boolean) {
        _isForeground.value = foreground
    }

    suspend fun sendVisibilityEvent(event: AppVisibilityEvent) {
        _visibilityEvents.emit(event)
    }
}
