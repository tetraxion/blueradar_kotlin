package com.example.blueradar.ui.screen.radar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueradar.data.repository.BleRepository
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
 * Track single device dengan visualisasi real-time
 */
@HiltViewModel
class RadarViewModel @Inject constructor(
    private val bleRepository: BleRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val macAddress: String = checkNotNull(savedStateHandle["macAddress"])

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startTracking()
    }

    /**
     * Mulai tracking device tertentu
     */
    private fun startTracking() {
        _uiState.update { it.copy(isTracking = true, error = null) }

        viewModelScope.launch {
            bleRepository.observeDevice(macAddress)
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

    /**
     * Stop tracking
     */
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
    val signalCategory: SignalCategory = SignalCategory.LOST,
    val estimatedDistance: Double = 0.0,
    val isTracking: Boolean = false,
    val error: String? = null
)
