package io.github.kgbis.remotecontrol.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.network.scanner.MDNSDiscovery
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DiscoveryState(
    val isDiscovering: Boolean = false,
    val deviceCount: Int = 0,
    val error: String? = null
)

data class DiscoveredDevice(
    val name: String,
    val ip: String,
    val port: Int,
    val type: String,
    val txtRecords: Map<String, String> = emptyMap()
)

class DiscoveryViewModel : ViewModel() {

    private val _discoveryState = MutableStateFlow(DiscoveryState())
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private lateinit var mdnsDiscovery: MDNSDiscovery

    init {
        viewModelScope.launch {
            initializeDiscovery()
        }
    }

    private fun initializeDiscovery() {
        // La inicialización real se haría en un Composable con rememberMDNSDiscovery
        // y se pasaría al ViewModel
    }

    fun startDiscovery() {
        viewModelScope.launch {
            try {
                _discoveryState.update { it.copy(isDiscovering = true, error = null) }

                // Usar el discovery desde el contexto de UI
                // En realidad esto se conectaría con el rememberMDNSDiscovery
                simulateDiscovery()

            } catch (e: Exception) {
                _discoveryState.update {
                    it.copy(error = e.message, isDiscovering = false)
                }
            }
        }
    }

    fun stopDiscovery() {
        _discoveryState.update { it.copy(isDiscovering = false) }
        // Aquí llamarías a mdnsDiscovery.stopDiscovery()
    }

    private fun simulateDiscovery() {
        // Simulación - en realidad usarías el MDNSDiscovery real
        viewModelScope.launch {
            delay(2000) // Simular búsqueda

            val mockDevices = listOf(
                DiscoveredDevice(
                    name = "MiPC-Windows",
                    ip = "192.168.1.100",
                    port = 6800,
                    type = "_remotecontrol._tcp",
                    txtRecords = mapOf("os" to "Windows", "version" to "1.0")
                ),
                DiscoveredDevice(
                    name = "Linux-Mint",
                    ip = "192.168.1.101",
                    port = 22,
                    type = "_ssh._tcp"
                ),
                DiscoveredDevice(
                    name = "Chromecast-Sala",
                    ip = "192.168.1.102",
                    port = 8009,
                    type = "_googlecast._tcp"
                )
            )

            _discoveredDevices.value = mockDevices
            _discoveryState.update {
                it.copy(
                    isDiscovering = false,
                    deviceCount = mockDevices.size
                )
            }
        }
    }

    fun addDiscoveredDevice(device: DiscoveredDevice) {
        _discoveredDevices.update { currentList ->
            if (currentList.none { it.ip == device.ip && it.port == device.port }) {
                currentList + device
            } else {
                currentList
            }
        }

        _discoveryState.update {
            it.copy(deviceCount = _discoveredDevices.value.size)
        }
    }

    fun clearDevices() {
        _discoveredDevices.value = emptyList()
        _discoveryState.update { it.copy(deviceCount = 0) }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}