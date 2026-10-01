package com.example.blueradar.ui.screen.radar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueradar.data.repository.BleRepository
import com.example.blueradar.data.repository.DeviceRepository
import com.example.blueradar.domain.model.BleDevice
import com.example.blueradar.domain.model.SignalCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Radar View Screen
 * Track spatial multi-device radar dengan visualisasi real-time
 */
@HiltViewModel
class RadarViewModel @Inject constructor(
    private val bleRepository: BleRepository,
    private val deviceRepository: DeviceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialMac: String = savedStateHandle.get<String>("macAddress") ?: ""

    private val _uiState = MutableStateFlow(RadarUiState(selectedMac = initialMac))
    val uiState = _uiState.asStateFlow()

    init {
        observeAllDevices()
        if (initialMac.isNotEmpty()) {
            trackDevice(initialMac)
        }
    }

    private fun observeAllDevices() {
        viewModelScope.launch {
            deviceRepository.getAllDevices().collect { entities ->
                val devices = entities.map { entity ->
                    val cat = SignalCategory.fromRssi(entity.lastRssi)
                    val dist = com.example.blueradar.domain.util.RssiUtils.estimateDistance(entity.lastRssi)
                    BleDevice(
                        mac = entity.mac,
                        name = entity.name,
                        rssi = entity.lastRssi,
                        estimatedDistance = dist,
                        signalCategory = cat,
                        lastScanTime = entity.lastSeenAt
                    )
                }
                _uiState.update { state ->
                    val selected = devices.find { it.mac == state.selectedMac } ?: devices.firstOrNull()
                    state.copy(
                        allDevices = devices,
                        device = selected ?: state.device,
                        signalCategory = selected?.signalCategory ?: SignalCategory.LOST,
                        estimatedDistance = selected?.estimatedDistance ?: 0.0
                    )
                }
            }
        }
    }

    fun selectTargetDevice(mac: String) {
        _uiState.update { it.copy(selectedMac = mac) }
        trackDevice(mac)
    }

    private fun trackDevice(mac: String) {
        if (mac.isBlank()) return
        _uiState.update { it.copy(isTracking = true, error = null) }

        viewModelScope.launch {
            bleRepository.observeDevice(mac)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isTracking = false,
                            error = exception.message ?: "Failed to track device"
                        )
                    }
                }
                .collect { device ->
                    _uiState.update {
                        it.copy(
                            device = device,
                            signalCategory = device.signalCategory,
                            estimatedDistance = device.estimatedDistance,
                            isTracking = true
                        )
                    }
                }
        }
    }

    fun stopTracking() {
        bleRepository.stopScanning()
        _uiState.update { it.copy(isTracking = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTracking()
    }
}

/**
 * UI State untuk Radar View
 */
data class RadarUiState(
    val device: BleDevice? = null,
    val selectedMac: String = "",
    val allDevices: List<BleDevice> = emptyList(),
    val signalCategory: SignalCategory = SignalCategory.LOST,
    val estimatedDistance: Double = 0.0,
    val isTracking: Boolean = false,
    val error: String? = null
)
