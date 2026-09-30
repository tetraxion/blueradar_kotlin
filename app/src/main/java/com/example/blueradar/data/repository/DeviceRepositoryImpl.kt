package com.example.blueradar.data.repository

import com.example.blueradar.data.local.DeviceDao
import com.example.blueradar.data.local.DeviceEntity
import com.example.blueradar.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation Device Repository
 * Handle persistence BLE device ke Room database
 */
@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override suspend fun saveDevice(device: BleDevice) {
        // Check apakah device sudah ada
        val existing = deviceDao.getDeviceByMac(device.mac)

        val entity = if (existing != null) {
            // Update existing device
            existing.copy(
                name = device.name ?: existing.name,
                lastRssi = device.rssi,
                lastDistance = device.estimatedDistance,
                lastSignalCategory = device.signalCategory.label,
                lastSeenAt = device.lastScanTime,
                scanCount = existing.scanCount + 1
            )
        } else {
            // Create new device entry
            DeviceEntity(
                mac = device.mac,
                name = device.name,
                lastRssi = device.rssi,
                lastDistance = device.estimatedDistance,
                lastSignalCategory = device.signalCategory.label,
                firstSeenAt = device.lastScanTime,
                lastSeenAt = device.lastScanTime,
                scanCount = 1
            )
        }

        deviceDao.insertOrUpdate(entity)
    }

    override fun getAllDevices(): Flow<List<DeviceEntity>> {
        return deviceDao.getAllDevices()
    }

    override suspend fun getDeviceByMac(mac: String): DeviceEntity? {
        return deviceDao.getDeviceByMac(mac)
    }

    override suspend fun deleteDevice(mac: String) {
        deviceDao.deleteDevice(mac)
    }

    override suspend fun clearHistory() {
        deviceDao.deleteAll()
    }

    override fun searchDevices(query: String): Flow<List<DeviceEntity>> {
        return deviceDao.searchDevices(query)
    }
}
