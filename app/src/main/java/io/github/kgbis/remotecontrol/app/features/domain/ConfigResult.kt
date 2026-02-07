package io.github.kgbis.remotecontrol.app.features.domain

import io.github.kgbis.remotecontrol.app.core.model.Device

sealed class ConflictResult {
    object None : ConflictResult()
    data class MacConflict(val device: Device) : ConflictResult()
    data class IpConflict(val device: Device) : ConflictResult()
    data class PossibleDuplicate(val device: Device) : ConflictResult()
}
