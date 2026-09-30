package com.example.blueradar.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object untuk operasi database perangkat BLE
 */
@Dao
interface DeviceDao {
    /**
     * Insert atau update device (upsert)
     * Jika MAC sudah ada, akan di-replace
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: DeviceEntity)

    /**
     * Get semua device yang pernah terdeteksi
     * Sorted by lastSeenAt descending (terbaru di atas)
     */
    @Query("SELECT * FROM device_history ORDER BY lastSeenAt DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    /**
     * Get device by MAC address
     */
    @Query("SELECT * FROM device_history WHERE mac = :mac")
    suspend fun getDeviceByMac(mac: String): DeviceEntity?

    /**
     * Delete device by MAC
     */
    @Query("DELETE FROM device_history WHERE mac = :mac")
    suspend fun deleteDevice(mac: String)

    /**
     * Delete all history
     */
    @Query("DELETE FROM device_history")
    suspend fun deleteAll()

    /**
     * Get device count
     */
    @Query("SELECT COUNT(*) FROM device_history")
    suspend fun getDeviceCount(): Int

    /**
     * Search devices by name or MAC
     */
    @Query("SELECT * FROM device_history WHERE name LIKE '%' || :query || '%' OR mac LIKE '%' || :query || '%' ORDER BY lastSeenAt DESC")
    fun searchDevices(query: String): Flow<List<DeviceEntity>>
}
