package com.example.blueradar.data.repository

import com.example.blueradar.data.ble.BleManager
import com.example.blueradar.domain.model.BleDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
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
    
    // StateFlow untuk share scanned devices antar ViewModels
    private val _scannedDevices = MutableStateFlow<Map<String, BleDevice>>(emptyMap())
    
    // Buffer untuk batch save updates
    private val deviceBuffer = mutableMapOf<String, BleDevice>()
    
    // Track new devices (for immediate save)
    private val knownDevices = mutableSetOf<String>()
    
    private var batchSaveJob: Job? = null

    override fun startScanning(): Flow<BleDevice> {
        // Start batch save job (save updates ke database setiap 10 detik)
        startBatchSaveJob()
        
        val scanFlow = bleManager.startScanning()
            .onEach { device ->
                // Update shared state untuk devices yang sedang di-scan
                val currentDevices = _scannedDevices.value.toMutableMap()
                currentDevices[device.mac] = device
                _scannedDevices.value = currentDevices
                
                // Database save logic (check & add to buffer outside suspend)
                val isNewDevice = synchronized(deviceBuffer) {
                    val isNew = !knownDevices.contains(device.mac)
                    if (isNew) {
                        knownDevices.add(device.mac)
                    } else {
                        // Device sudah ada, tambahkan ke buffer untuk batch update
                        deviceBuffer[device.mac] = device
                    }
                    isNew
                }
                
                // Save immediately untuk device baru (outside synchronized)
                if (isNewDevice) {
                    CoroutineScope(Dispatchers.IO).launch {
                        deviceRepository.saveDevice(device)
                    }
                }
            }
        lastScanFlow = scanFlow
        return scanFlow
    }

    private fun startBatchSaveJob() {
        batchSaveJob?.cancel()
        batchSaveJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10000) // Batch save setiap 10 detik
                
                // Ambil snapshot dari buffer
                val devicesToSave = synchronized(deviceBuffer) {
                    deviceBuffer.values.toList().also {
                        deviceBuffer.clear()
                    }
                }
                
                // Batch update ke database
                if (devicesToSave.isNotEmpty()) {
                    devicesToSave.forEach { device ->
                        deviceRepository.saveDevice(device)
                    }
                }
            }
        }
    }

    override fun stopScanning() {
        bleManager.stopScanning()
        
        // Cancel batch save job
        batchSaveJob?.cancel()
        
        // Save remaining buffer sebelum stop (final update)
        CoroutineScope(Dispatchers.IO).launch {
            val devicesToSave = synchronized(deviceBuffer) {
                deviceBuffer.values.toList().also {
                    deviceBuffer.clear()
                }
            }
            devicesToSave.forEach { device ->
                deviceRepository.saveDevice(device)
            }
        }
        
        // Clear scanned devices state
        _scannedDevices.value = emptyMap()
        
        // Clear known devices untuk scan session berikutnya
        knownDevices.clear()
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

    override fun getScannedDevices(): StateFlow<Map<String, BleDevice>> {
        return _scannedDevices.asStateFlow()
    }
}
