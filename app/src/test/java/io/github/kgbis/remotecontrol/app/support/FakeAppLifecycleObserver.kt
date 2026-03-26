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
