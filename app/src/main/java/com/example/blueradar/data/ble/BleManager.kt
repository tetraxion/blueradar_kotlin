package com.example.blueradar.data.ble

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager untuk mengatur scan state
 * Handles start/stop scanning dan state management
 */
@Singleton
class BleManager @Inject constructor(
    private val bleScanner: BleScanner
) {
    private val _isScanning = MutableStateFlow(false)
    val isScanning = _isScanning.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bleScanner.isBluetoothEnabled())
    val isBluetoothEnabled = _isBluetoothEnabled.asStateFlow()

    private val _scanFlow: MutableStateFlow<Flow<com.example.blueradar.domain.model.BleDevice>?> = MutableStateFlow(null)

    var currentScanJob: Job? = null

    /**
     * Start BLE scanning
     */
    fun startScanning(): Flow<com.example.blueradar.domain.model.BleDevice> {
        _isScanning.value = true
        _isBluetoothEnabled.value = bleScanner.isBluetoothEnabled()
        return bleScanner.startScan()
    }

    /**
     * Stop BLE scanning
     */
    fun stopScanning() {
        _isScanning.value = false
        currentScanJob?.cancel()
    }

    /**
     * Update Bluetooth enabled state (biasanya dipanggil saat ada broadcast dari system)
     */
    fun updateBluetoothState(enabled: Boolean) {
        _isBluetoothEnabled.value = enabled
    }
}
