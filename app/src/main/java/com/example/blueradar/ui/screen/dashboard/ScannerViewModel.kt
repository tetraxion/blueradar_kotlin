package com.example.blueradar.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueradar.data.repository.BleRepository
import com.example.blueradar.domain.model.BleDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Dashboard Scanner Screen
 * Manages BLE scanning state dan device list
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bleRepository: BleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState = _uiState.asStateFlow()

    private var scanJob: Job? = null

    /**
     * Start BLE scanning
     */
    fun startScanning() {
        if (_uiState.value.isScanning) return

        _uiState.update { it.copy(isScanning = true, error = null) }

        scanJob = viewModelScope.launch {
            bleRepository.startScanning()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            error = exception.message ?: "Scan failed"
                        )
                    }
                }
                .collect { device ->
                    // Update atau tambah device ke list
                    val currentDevices = _uiState.value.devices.toMutableMap()
                    currentDevices[device.mac] = device

                    _uiState.update {
                        it.copy(devices = currentDevices)
                    }
                }
        }
    }

    /**
     * Stop BLE scanning
     */
    fun stopScanning() {
        scanJob?.cancel()
        bleRepository.stopScanning()
        _uiState.update { it.copy(isScanning = false) }
    }

    /**
     * Update search query untuk filter
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Update RSSI threshold untuk filter
     */
    fun updateRssiThreshold(threshold: Int) {
        _uiState.update { it.copy(rssiThreshold = threshold) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }
}

/**
 * UI State untuk Dashboard Scanner
 */
data class ScannerUiState(
    val isScanning: Boolean = false,
    val devices: Map<String, BleDevice> = emptyMap(),  // Key = MAC address
    val searchQuery: String = "",
    val rssiThreshold: Int = -100,  // Default: tampilkan semua
    val isBluetoothEnabled: Boolean = true,
    val isPermissionGranted: Boolean = false,
    val error: String? = null
) {
    /**
     * Get filtered & sorted device list
     * - Filter by search query (name atau MAC)
     * - Filter by RSSI threshold
     * - Sort by RSSI descending (strongest signal first)
     */
    val filteredDevices: List<BleDevice>
        get() = devices.values
            .filter { device ->
                // Filter by RSSI threshold
                device.rssi >= rssiThreshold
            }
            .filter { device ->
                // Filter by search query
                if (searchQuery.isBlank()) {
                    true
                } else {
                    val query = searchQuery.lowercase()
                    device.name?.lowercase()?.contains(query) == true ||
                            device.mac.lowercase().contains(query)
                }
            }
            .sortedByDescending { it.rssi }  // Strongest signal first
}
