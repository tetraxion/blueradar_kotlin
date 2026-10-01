package com.example.blueradar.data.repository

import com.example.blueradar.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface untuk BLE operations
 * Abstraction layer antara data source dan business logic
 */
interface BleRepository {
    /**
     * Mulai scanning perangkat BLE
     * @return Flow yang emit BleDevice setiap kali ada scan result
     */
    fun startScanning(): Flow<BleDevice>

    /**
     * Stop scanning
     */
    fun stopScanning()

    /**
     * Check apakah sedang scanning
     */
    fun isScanning(): Flow<Boolean>

    /**
     * Check status Bluetooth
     */
    fun isBluetoothEnabled(): Flow<Boolean>

    /**
     * Observe satu device tertentu dari scan results
     * @param macAddress MAC address device
     */
    fun observeDevice(macAddress: String): Flow<BleDevice>

    /**
     * Update Bluetooth state
     */
    fun updateBluetoothState(enabled: Boolean)

    /**
     * Get current scanned devices as StateFlow
     * Untuk share scan results antar ViewModels
     */
    fun getScannedDevices(): StateFlow<Map<String, BleDevice>>
}
