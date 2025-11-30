package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.network.NetworkActions.sendMessage
import com.example.remote.shutdown.network.NetworkScanner.checkPcStatus
import com.example.remote.shutdown.network.NetworkScanner.infoRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

class ScanManager(
    private val networkRangeDetector: NetworkRangeDetector,
    private val timeout: Int,
    private val maxConcurrent: Int = 30
) {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // ---- Public flows ----

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _results = MutableStateFlow<List<Device>>(emptyList())
    val results: StateFlow<List<Device>> = _results

    private val _state = MutableStateFlow(ScanState.Idle)
    val state: StateFlow<ScanState> = _state

    // ---- Control ----
    private var currentScanJob: Job? = null

    fun startScan() {
        if (isRunning()) return  // avoid multiple scans

        _results.value = emptyList()
        _progress.value = 0
        _state.value = ScanState.Running

        val baseIp = networkRangeDetector.getScanSubnet()
        val localIp = networkRangeDetector.getLocalAddress()

        currentScanJob = scope.launch {
            val progressCounter = AtomicInteger(0)
            val devices = mutableListOf<Device>()
            val semaphore = Semaphore(maxConcurrent)

            try {
                val jobs = (1..254).map { i ->
                    async {
                        ensureActive()

                        val ip = "$baseIp.$i"
                        if (ip == localIp) return@async

                        semaphore.withPermit {
                            ensureActive()

                            val device = if (checkPcStatus(ip, timeout = timeout))
                                getInfoForIp(ip)
                            else
                                null

                            if (device != null) {
                                synchronized(devices) { devices.add(device) }
                                _results.value = devices.toList()
                            }

                            val p = progressCounter.incrementAndGet()
                            _progress.value = p
                        }
                    }
                }

                jobs.awaitAll()
                _state.value = ScanState.Finished

            } catch (_: CancellationException) {
                _state.value = ScanState.Cancelled
            } finally {
                currentScanJob = null
            }
        }
    }

    fun cancelScan() {
        currentScanJob?.cancel()
        currentScanJob = null
        _state.value = ScanState.Cancelled
    }

    fun isRunning() = currentScanJob != null

    private suspend fun getInfoForIp(ip: String): Device {
        val device = Device(ip, ip, "")
        val now = System.currentTimeMillis()
        val result = sendMessage(device, "INFO ${device.ip}", ::infoRequest, 1500)
        if (result != null) {
            device.name = result.second
            device.mac = result.third
        }
        Log.d(
            "scanLocalNetwork",
            "Took ${System.currentTimeMillis() - now} ms. success? ${result?.first ?: false}, Device -> $device"
        )
        return device
    }
}

enum class ScanState {
    Idle, Running, Cancelled, Finished
}
