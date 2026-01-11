package io.github.kgbis.remotecontrol.app.core

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLifecycleObserver : DefaultLifecycleObserver {

    private val _visibilityEvents =
        MutableSharedFlow<AppVisibilityEvent>(extraBufferCapacity = 1)

    val visibilityEvents = _visibilityEvents.asSharedFlow()

    private val _isForeground = MutableStateFlow(false)
    val isForegroundFlow = _isForeground.asStateFlow()

    override fun onStart(owner: LifecycleOwner) {
        _isForeground.value = true
        _visibilityEvents.tryEmit(AppVisibilityEvent.Foreground)
    }

    override fun onStop(owner: LifecycleOwner) {
        _isForeground.value = false
        _visibilityEvents.tryEmit(AppVisibilityEvent.Background)
    }
}