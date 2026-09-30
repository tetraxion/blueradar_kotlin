package com.example.blueradar.data.repository

import com.example.blueradar.data.ble.BleManager
import com.example.blueradar.domain.model.BleDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi BLE Repository
 * Mengkoordinasikan antara BLE Scanner dan data persistence
 */
@Singleton
class BleRepositoryImpl @Inject constructor(
    private val bleManager: BleManager,
    private val deviceRepository: DeviceRepository
) : BleRepository {
    
    // Simpan device terakhir yang di-scan untuk observing specific device
    private var lastScanFlow: Flow<BleDevice>? = null

    override fun startScanning(): Flow<BleDevice> {
        val scanFlow = bleManager.startScanning()
            .onEach { device ->
                // Auto-save setiap device yang terdeteksi ke database
                CoroutineScope(Dispatchers.IO).launch {
                    deviceRepository.saveDevice(device)
                }
            }
        lastScanFlow = scanFlow
        return scanFlow
    }

    override fun stopScanning() {
        bleManager.stopScanning()
    }

    override fun isScanning(): Flow<Boolean> {
        return bleManager.isScanning
    }

    override fun isBluetoothEnabled(): Flow<Boolean> {
        return bleManager.isBluetoothEnabled
    }

    override fun observeDevice(macAddress: String): Flow<BleDevice> {
        // Filter scan results untuk hanya emit device dengan MAC tertentu
        return lastScanFlow?.filter { device ->
            device.mac == macAddress
        } ?: throw IllegalStateException("Scan not started. Call startScanning() first.")
    }

    override fun updateBluetoothState(enabled: Boolean) {
        bleManager.updateBluetoothState(enabled)
    }
}
