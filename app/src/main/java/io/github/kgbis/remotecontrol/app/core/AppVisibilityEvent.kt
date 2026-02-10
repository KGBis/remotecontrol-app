package io.github.kgbis.remotecontrol.app.core

sealed class AppVisibilityEvent {
    object Foreground : AppVisibilityEvent()
    object Background : AppVisibilityEvent()
}
