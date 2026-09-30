package com.example.blueradar.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import com.example.blueradar.domain.model.BleDevice
import com.example.blueradar.domain.model.SignalCategory
import com.example.blueradar.domain.util.RssiUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * BLE Scanner wrapper yang menggunakan Android's native BLE API
 * Emit hasil scan sebagai Flow<BleDevice>
 */
class BleScanner @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?
) {
    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    /**
     * Mulai scanning dan emit BleDevice secara real-time
     * Flow ini akan emit setiap kali ada device baru terdeteksi atau RSSI berubah
     */
    @SuppressLint("MissingPermission")
    fun startScan(): Flow<BleDevice> = callbackFlow {
        if (bleScanner == null) {
            close(Exception("Bluetooth LE Scanner not available"))
            return@callbackFlow
        }

        // Tracking untuk deduplikasi dan update RSSI
        val scannedDevices = mutableMapOf<String, BleDevice>()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let {
                    val mac = it.device.address
                    val rssi = it.rssi
                    val name = it.device.name ?: it.scanRecord?.deviceName

                    // Validasi RSSI
                    if (!RssiUtils.isValidRssi(rssi)) return@let

                    val distance = RssiUtils.estimateDistance(rssi)
                    val category = SignalCategory.fromRssi(rssi)

                    val device = BleDevice(
                        mac = mac,
                        name = name,
                        rssi = rssi,
                        estimatedDistance = distance,
                        signalCategory = category
                    )

                    // Update map dan emit
                    scannedDevices[mac] = device
                    trySend(device)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    val mac = result.device.address
                    val rssi = result.rssi
                    val name = result.device.name ?: result.scanRecord?.deviceName

                    if (RssiUtils.isValidRssi(rssi)) {
                        val distance = RssiUtils.estimateDistance(rssi)
                        val category = SignalCategory.fromRssi(rssi)

                        val device = BleDevice(
                            mac = mac,
                            name = name,
                            rssi = rssi,
                            estimatedDistance = distance,
                            signalCategory = category
                        )

                        scannedDevices[mac] = device
                        trySend(device)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("Scan failed with error code: $errorCode"))
            }
        }

        // Start scan
        try {
            bleScanner.startScan(scanCallback)
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        // Cleanup: stop scan ketika Flow ini ditutup
        awaitClose {
            try {
                bleScanner.stopScan(scanCallback)
            } catch (e: Exception) {
                // Ignore exception saat cleanup
            }
        }
    }

    /**
     * Check apakah Bluetooth LE supported
     */
    fun isBluetoothLeSupported(): Boolean {
        return bluetoothAdapter != null
    }

    /**
     * Check apakah Bluetooth enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
}
