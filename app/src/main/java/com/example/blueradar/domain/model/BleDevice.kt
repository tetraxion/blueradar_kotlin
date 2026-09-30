package com.example.blueradar.domain.model

/**
 * Domain model untuk perangkat BLE
 * Digunakan di seluruh aplikasi (UI, ViewModel, Repository)
 */
data class BleDevice(
    val mac: String,           // Unique identifier (MAC Address)
    val name: String?,         // Nama perangkat (nullable, tidak semua BLE device punya nama)
    val rssi: Int,             // Signal strength in dBm (negative value)
    val estimatedDistance: Double,  // Jarak perkiraan dalam meter
    val signalCategory: SignalCategory,  // Kategori sinyal
    val lastScanTime: Long = System.currentTimeMillis()  // Waktu terakhir di-scan
) {
    companion object {
        /**
         * Generate device identifier dari MAC address
         */
        fun deviceId(mac: String): String = mac.replace(":", "")
    }
}

/**
 * Enum untuk kategori sinyal berdasarkan RSSI
 * Mengikuti spesifikasi di PRD
 */
enum class SignalCategory(
    val label: String,
    val colorHex: String,
    val minRssi: Int,
    val maxRssi: Int,
    val estimatedDistanceLabel: String
) {
    VERY_STRONG("Sangat Kuat (Sangat Dekat)", "#00E676", -30, 0, "< 1 meter"),
    STRONG("Kuat (Dekat)", "#76FF03", -50, -30, "1 – 3 meter"),
    GOOD("Cukup / Baik", "#FFD740", -70, -50, "3 – 10 meter"),
    WEAK("Lemah", "#FF6D00", -80, -70, "10 – 20 meter"),
    VERY_WEAK("Sangat Lemah / Putus-putus", "#FF1744", -90, -80, "> 20 meter"),
    LOST("Sinyal Hilang (Lost)", "#616161", Int.MIN_VALUE, -90, "Terputus");

    companion object {
        /**
         * Tentukan kategori sinyal dari nilai RSSI
         */
        fun fromRssi(rssi: Int): SignalCategory {
            return when {
                rssi >= -30 -> VERY_STRONG
                rssi >= -50 -> STRONG
                rssi >= -70 -> GOOD
                rssi >= -80 -> WEAK
                rssi >= -90 -> VERY_WEAK
                else -> LOST
            }
        }
    }
}
