package com.example.blueradar.ui.screen.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueradar.data.ble.BluetoothStateReceiver
import com.example.blueradar.data.repository.BleRepository
import com.example.blueradar.domain.model.BleDevice
import com.example.blueradar.domain.util.SystemSettingsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Dashboard Scanner Screen
 * Manages BLE scanning state dan device list
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val bleRepository: BleRepository,
    private val bluetoothStateReceiver: BluetoothStateReceiver,
    private val systemSettingsHelper: SystemSettingsHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState = _uiState.asStateFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScannerUiState()
        )

    private var scanJob: Job? = null

    init {
        // Observe Bluetooth state changes
        viewModelScope.launch {
            bluetoothStateReceiver.observeBluetoothState()
                .collect { isEnabled ->
                    _uiState.update { it.copy(isBluetoothEnabled = isEnabled) }
                    
                    // Stop scanning if Bluetooth disabled
                    if (!isEnabled && _uiState.value.isScanning) {
                        stopScanning()
                        _uiState.update {
                            it.copy(error = "Bluetooth has been disabled")
                        }
                    }
                }
        }
        
        // Observe shared scanning state from repository (untuk sync antar screens)
        viewModelScope.launch {
            bleRepository.isScanning().collect { isScanning ->
                _uiState.update { it.copy(isScanning = isScanning) }
            }
        }
        
        // Check initial state
        checkSystemReadiness()
    }

    /**
     * Check apakah Bluetooth dan Location sudah enabled
     * Update UI state accordingly
     */
    private fun checkSystemReadiness() {
        viewModelScope.launch {
            val isBluetoothEnabled = systemSettingsHelper.isBluetoothEnabled()
            val isLocationEnabled = systemSettingsHelper.isLocationEnabled(context)
            
            _uiState.update {
                it.copy(
                    isBluetoothEnabled = isBluetoothEnabled,
                    isLocationEnabled = isLocationEnabled,
                    needsBluetoothEnable = !isBluetoothEnabled,
                    needsLocationEnable = !isLocationEnabled
                )
            }
        }
    }

    /**
     * Start BLE scanning
     * Will check system readiness first before starting
     */
    fun startScanning() {
        if (_uiState.value.isScanning) return
        
        // Check system readiness (Bluetooth & Location)
        val (isReady, errorMessage) = systemSettingsHelper.checkBleReadiness(context)
        
        if (!isReady) {
            // Update state untuk trigger enable request dari UI
            val needsBluetooth = !systemSettingsHelper.isBluetoothEnabled()
            val needsLocation = !systemSettingsHelper.isLocationEnabled(context)
            
            _uiState.update {
                it.copy(
                    needsBluetoothEnable = needsBluetooth,
                    needsLocationEnable = needsLocation,
                    error = errorMessage
                )
            }
            return
        }

        _uiState.update { it.copy(error = null) }
        // isScanning akan auto-update dari bleRepository.isScanning() flow

        // Buffer untuk collect device updates
        val deviceBuffer = mutableMapOf<String, BleDevice>()

        scanJob = viewModelScope.launch {
            // Launch coroutine untuk periodic UI update (setiap 300ms)
            launch {
                while (true) {
                    kotlinx.coroutines.delay(300) // Update UI setiap 300ms
                    if (deviceBuffer.isNotEmpty()) {
                        val currentDevices = _uiState.value.devices.toMutableMap()
                        currentDevices.putAll(deviceBuffer)
                        _uiState.update { it.copy(devices = currentDevices) }
                        deviceBuffer.clear()
                    }
                }
            }

            // Collect scan results dan buffer dulu
            bleRepository.startScanning()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isScanning = false,
                            error = exception.message ?: "Scan failed"
                        )
                    }
                }
                .collect { device ->
                    // Tambah ke buffer instead of direct update
                    deviceBuffer[device.mac] = device
                }
        }
    }

    /**
     * Callback ketika user sudah enable Bluetooth
     * Akan re-check system readiness dan auto-start scan jika ready
     */
    fun onBluetoothEnabled() {
        _uiState.update { 
            it.copy(
                isBluetoothEnabled = true,
                needsBluetoothEnable = false
            ) 
        }
        
        // Auto-start scan jika semua requirements terpenuhi
        if (systemSettingsHelper.isLocationEnabled(context)) {
            startScanning()
        }
    }

    /**
     * Callback ketika user sudah enable Location
     * Akan re-check system readiness dan auto-start scan jika ready
     */
    fun onLocationEnabled() {
        _uiState.update { 
            it.copy(
                isLocationEnabled = true,
                needsLocationEnable = false
            ) 
        }
        
        // Auto-start scan jika semua requirements terpenuhi
        if (systemSettingsHelper.isBluetoothEnabled()) {
            startScanning()
        }
    }

    /**
     * Mark bahwa enable request sudah di-handle
     * Reset flags untuk avoid multiple requests
     */
    fun onEnableRequestHandled() {
        _uiState.update { 
            it.copy(
                needsBluetoothEnable = false,
                needsLocationEnable = false
            ) 
        }
    }

    /**
     * Stop BLE scanning
     * Clear devices untuk realtime behavior (seperti Radar View)
     */
    fun stopScanning() {
        scanJob?.cancel()
        bleRepository.stopScanning()
        // isScanning akan auto-update dari bleRepository.isScanning() flow
        _uiState.update { 
            it.copy(
                devices = emptyMap() // Clear devices ketika scan stop
            ) 
        }
    }

    /**
     * Update search query untuk filter
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Update RSSI threshold untuk filter
     */
    fun updateRssiThreshold(threshold: Int) {
        _uiState.update { it.copy(rssiThreshold = threshold) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Toggle sort order between strongest and weakest
     */
    fun toggleSortOrder() {
        _uiState.update { it.copy(isSortAscending = !it.isSortAscending) }
    }

    override fun onCleared() {
        super.onCleared()
        // JANGAN auto-stop scanning
        // User bisa manual stop dari UI
        // Scan tetap jalan antar screen untuk continuity
    }
}

/**
 * UI State untuk Dashboard Scanner
 */
data class ScannerUiState(
    val isScanning: Boolean = false,
    val devices: Map<String, BleDevice> = emptyMap(),  // Key = MAC address
    val searchQuery: String = "",
    val rssiThreshold: Int = -100,  // Default: tampilkan semua
    val isSortAscending: Boolean = false, // false = strongest first, true = weakest first
    val isBluetoothEnabled: Boolean = true,
    val isLocationEnabled: Boolean = true,
    val needsBluetoothEnable: Boolean = false,  // Flag untuk trigger enable request
    val needsLocationEnable: Boolean = false,   // Flag untuk trigger enable request
    val isPermissionGranted: Boolean = false,
    val error: String? = null
) {
    /**
     * Get filtered & sorted device list
     * Cached untuk menghindari recalculation setiap recomposition
     */
    private var _cachedFilteredDevices: List<BleDevice>? = null
    private var _cacheKey: String = ""

    val filteredDevices: List<BleDevice>
        get() {
            // Cache key untuk detect changes
            val currentCacheKey = "${devices.hashCode()}-$searchQuery-$rssiThreshold-$isSortAscending"
            
            // Return cache jika tidak ada perubahan
            if (currentCacheKey == _cacheKey && _cachedFilteredDevices != null) {
                return _cachedFilteredDevices!!
            }

            // Calculate filtered list
            val list = devices.values
                .filter { device ->
                    device.rssi >= rssiThreshold
                }
                .filter { device ->
                    if (searchQuery.isBlank()) {
                        true
                    } else {
                        val query = searchQuery.lowercase()
                        device.name?.lowercase()?.contains(query) == true ||
                                device.mac.lowercase().contains(query)
                    }
                }
            
            val sorted = if (isSortAscending) {
                list.sortedBy { it.rssi }
            } else {
                list.sortedByDescending { it.rssi }
            }

            // Update cache
            _cachedFilteredDevices = sorted
            _cacheKey = currentCacheKey
            
            return sorted
        }
}
