package com.example.blueradar.ui.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.blueradar.data.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Splash Screen
 * Mengelola logic timer dan inisialisasi awal aplikasi sesuai pola MVVM
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val bleRepository: BleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    init {
        startSplashTimer()
    }

    private fun startSplashTimer() {
        viewModelScope.launch {
            // Check initial state jika diperlukan
            val isBtEnabled = bleRepository.isBluetoothEnabled()
            
            // Tunggu animasi splash 2 detik
            delay(2000)
            
            _uiState.update { 
                it.copy(isReadyToNavigate = true) 
            }
        }
    }
}

/**
 * State UI untuk Splash Screen
 */
data class SplashUiState(
    val isReadyToNavigate: Boolean = false
)
