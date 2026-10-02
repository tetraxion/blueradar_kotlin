package com.example.blueradar.domain.util

import kotlin.math.pow

/**
 * Utility untuk konversi RSSI ke jarak dan kategori sinyal
 */
object RssiUtils {
    /**
     * TX Power standar untuk BLE (usually -59 dBm at 1 meter)
     * Bisa di-adjust berdasarkan kalibrasi perangkat
     */
    private const val TX_POWER = -59

    /**
     * Kalkulasi estimasi jarak dari nilai RSSI menggunakan Log-Distance Path Loss Model
     *
     * Formula: distance = 10^((txPower - rssi) / (10 * n))
     * n = path loss exponent (typically 2.0 for free space, up to 4.0 for obstacles)
     *
     * @param rssi Signal strength in dBm (negative value)
     * @param txPower TX Power at 1 meter (default: -59 dBm)
     * @return Estimated distance in meters
     */
    fun estimateDistance(rssi: Int, txPower: Int = TX_POWER): Double {
        if (rssi == 0) return -1.0  // Invalid RSSI
        
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            // Empirical formula untuk akurasi yang lebih baik
            (0.89976 * ratio.pow(7.7095) + 0.111)
        }
    }

    /**
     * Format distance untuk display di UI
     * @param distance Distance in meters
     * @return Formatted string (e.g., "2.5 m" or "< 1 m")
     */
    fun formatDistance(distance: Double): String {
        return when {
            distance < 0 -> "Unknown Device"
            distance < 1.0 -> "< 1 m"
            else -> "%.1f m".format(distance)
        }
    }

    /**
     * Cek apakah RSSI valid
     */
    fun isValidRssi(rssi: Int): Boolean {
        return rssi < 0 && rssi > -120  // Typical BLE RSSI range
    }
}
