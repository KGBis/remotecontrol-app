package io.github.kgbis.remotecontrol.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.kgbis.remotecontrol.app.network.NetworkRangeDetector
import io.github.kgbis.remotecontrol.app.network.ScanManager

class ScanViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val networkRangeDetector = NetworkRangeDetector()

    // ---- Scan Manager ----
    private val scanManager by lazy {
        ScanManager(
            networkRangeDetector = networkRangeDetector,
            timeout = 23, //socketTimeout.value,
            maxConcurrent = 30
        )
    }

    val scanProgress = scanManager.progress
    val scanResults = scanManager.results
    val scanState = scanManager.state

    fun startScan() = scanManager.startScan()
    fun cancelScan() = scanManager.cancelScan()
}
