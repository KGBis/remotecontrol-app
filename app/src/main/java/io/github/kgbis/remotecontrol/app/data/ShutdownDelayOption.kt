package io.github.kgbis.remotecontrol.app.data

import io.github.kgbis.remotecontrol.app.R
import java.time.temporal.ChronoUnit

data class ShutdownDelayOption(
    val labelRes: Int,
    val amount: Long,
    val unit: ChronoUnit
)

val shutdownDelayOptions = listOf(
    ShutdownDelayOption(R.string.delay_now, 0, ChronoUnit.SECONDS),
    ShutdownDelayOption(R.string.delay_15s, 15, ChronoUnit.SECONDS),
    ShutdownDelayOption(R.string.delay_30s, 30, ChronoUnit.SECONDS),
    ShutdownDelayOption(R.string.delay_1m, 1, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_15m, 15, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_30m, 30, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_1h, 1, ChronoUnit.HOURS),
)
