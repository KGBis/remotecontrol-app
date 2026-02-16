package io.github.kgbis.remotecontrol.app.core

import androidx.lifecycle.DefaultLifecycleObserver
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AppLifecycleObserver : DefaultLifecycleObserver {

    val visibilityEvents: SharedFlow<AppVisibilityEvent>

    val isForegroundFlow: StateFlow<Boolean>
}