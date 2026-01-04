package io.github.kgbis.remotecontrol.app.core.model

import java.time.Instant

sealed class PendingAction {
    data object None : PendingAction()

    data class ShutdownScheduled(
        val scheduledAt: Instant,
        val executeAt: Instant,
        val cancellable: Boolean
    ) : PendingAction()
}
