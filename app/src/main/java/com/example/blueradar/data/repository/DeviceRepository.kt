package com.example.blueradar.data.repository

import com.example.blueradar.data.local.DeviceEntity
import com.example.blueradar.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface untuk local device persistence (Room)
 */
interface DeviceRepository {
    /**
     * Save device ke database
     * Jika sudah ada, akan di-update
     */
    suspend fun saveDevice(device: BleDevice)

    /**
     * Get all device history
     */
    fun getAllDevices(): Flow<List<DeviceEntity>>

    /**
     * Get device by MAC
     */
    suspend fun getDeviceByMac(mac: String): DeviceEntity?

    /**
     * Delete device
     */
    suspend fun deleteDevice(mac: String)

    /**
     * Clear all history
     */
    suspend fun clearHistory()

    /**
     * Search devices
     */
    fun searchDevices(query: String): Flow<List<DeviceEntity>>
}
