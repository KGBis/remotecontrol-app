package io.github.kgbis.remotecontrol.app.core

import android.util.Log
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

    override fun onCreate(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onCreate")
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onStart")
        _isForeground.value = true
        _visibilityEvents.tryEmit(AppVisibilityEvent.Foreground)
    }

    override fun onResume(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onResume")
    }

    override fun onPause(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onStop")
        _isForeground.value = false
        _visibilityEvents.tryEmit(AppVisibilityEvent.Background)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Log.d("AppLifecycleObserver", "onDestroy")
    }
}