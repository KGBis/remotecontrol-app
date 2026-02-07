package io.github.kgbis.remotecontrol.app.features.domain

data class MatchConfig(
    val idExact: Int = 100,
    val macWeight: Int = 60,
    val hostnameExact: Int = 30,
    val hostnameFuzzyHigh: Int = 20,
    val hostnameFuzzyLow: Int = 10,
    val osMatch: Int = 10,
    val ipMatch: Int = 10,
    val osVersion: Int = 5,
    val interfaceSize: Int = 5,
    val threshold: Int = 55,
    val macConflictPenalty: Int = 40
)

