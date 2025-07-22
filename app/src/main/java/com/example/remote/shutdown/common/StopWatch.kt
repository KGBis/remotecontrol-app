package com.example.remote.shutdown.common

import android.os.SystemClock

class StopWatch private constructor() {

    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private var running: Boolean = false

    fun start() {
        if (!running) {
            startTime = SystemClock.elapsedRealtime()
            running = true
        }
    }

    fun stop() {
        if (running) {
            elapsedTime += SystemClock.elapsedRealtime() - startTime
            running = false
        }
    }

    fun reset() {
        startTime = 0L
        elapsedTime = 0L
        running = false
    }

    fun restart() {
        reset()
        start()
    }

    fun getElapsedTimeMillis(): Long {
        return if (running) {
            elapsedTime + (SystemClock.elapsedRealtime() - startTime)
        } else {
            elapsedTime
        }
    }

    fun isRunning(): Boolean = running

    companion object {
        /** Creates a new stopped stopwatch */
        fun create(): StopWatch = StopWatch()

        /** Creates a new started stopwatch */
        fun createStarted(): StopWatch {
            val sw = StopWatch()
            sw.start()
            return sw
        }
    }
}
