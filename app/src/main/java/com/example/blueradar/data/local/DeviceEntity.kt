package com.example.blueradar.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity untuk menyimpan riwayat perangkat BLE yang pernah terdeteksi
 */
@Entity(tableName = "device_history")
data class DeviceEntity(
    @PrimaryKey
    val mac: String,                    // MAC address sebagai primary key
    val name: String?,                  // Nama perangkat (nullable)
    val lastRssi: Int,                  // RSSI terakhir yang tercatat
    val lastDistance: Double,           // Jarak terakhir yang tercatat
    val lastSignalCategory: String,     // Kategori sinyal terakhir (saved as String)
    val firstSeenAt: Long,              // Timestamp pertama kali terdeteksi
    val lastSeenAt: Long,               // Timestamp terakhir terdeteksi
    val scanCount: Int = 1              // Jumlah kali device ini di-scan
)
