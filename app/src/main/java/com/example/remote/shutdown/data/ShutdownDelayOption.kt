package com.example.remote.shutdown.data

import com.example.remote.shutdown.R
import java.util.concurrent.TimeUnit

data class ShutdownDelayOption(
    val labelRes: Int,
    val amount: Long,
    val unit: TimeUnit
)

val shutdownDelayOptions = listOf(
    ShutdownDelayOption(R.string.delay_now, 0, TimeUnit.SECONDS),
    ShutdownDelayOption(R.string.delay_15s, 15, TimeUnit.SECONDS),
    ShutdownDelayOption(R.string.delay_1m, 1, TimeUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_30m, 30, TimeUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_1h, 1, TimeUnit.HOURS),
)
