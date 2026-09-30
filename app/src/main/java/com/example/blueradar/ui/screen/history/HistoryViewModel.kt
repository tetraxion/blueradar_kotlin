package com.example.blueradar.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueradar.data.local.DeviceEntity
import com.example.blueradar.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk History Screen
 * Menampilkan riwayat perangkat yang pernah terdeteksi
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    /**
     * Load device history from database
     */
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            deviceRepository.getAllDevices()
                .collect { devices ->
                    _uiState.update {
                        it.copy(
                            devices = devices,
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Clear all history
     */
    fun clearHistory() {
        viewModelScope.launch {
            deviceRepository.clearHistory()
        }
    }

    /**
     * Delete single device
     */
    fun deleteDevice(mac: String) {
        viewModelScope.launch {
            deviceRepository.deleteDevice(mac)
        }
    }
}

/**
 * UI State untuk History Screen
 */
data class HistoryUiState(
    val devices: List<DeviceEntity> = emptyList(),
    val isLoading: Boolean = false
)
