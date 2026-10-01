package com.example.blueradar

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.blueradar.domain.util.SystemSettingsHelper
import com.example.blueradar.ui.navigation.AppNavHost
import com.example.blueradar.ui.theme.BlueRadarTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var systemSettingsHelper: SystemSettingsHelper

    // State flows untuk communicate dengan ViewModels
    private val _bluetoothEnableResult = MutableStateFlow<Boolean?>(null)
    val bluetoothEnableResult: StateFlow<Boolean?> = _bluetoothEnableResult.asStateFlow()

    private val _locationEnableResult = MutableStateFlow<Boolean?>(null)
    val locationEnableResult: StateFlow<Boolean?> = _locationEnableResult.asStateFlow()

    // Launchers for enable requests
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<android.content.Intent>
    private lateinit var enableLocationLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Bluetooth enable launcher
        enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val isEnabled = result.resultCode == Activity.RESULT_OK
            _bluetoothEnableResult.value = isEnabled
            
            // Reset setelah 1 detik untuk allow retry
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                _bluetoothEnableResult.value = null
            }
        }

        // Setup Location enable launcher
        enableLocationLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            val isEnabled = result.resultCode == Activity.RESULT_OK
            _locationEnableResult.value = isEnabled
            
            // Reset setelah 1 detik untuk allow retry
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                _locationEnableResult.value = null
            }
        }
        
        // Observe lifecycle to handle app going to background
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // App is in foreground
                // Scanning will continue
            }
        }
        
        setContent {
            BlueRadarTheme {
                val navController = rememberNavController()
                
                // Provide activity context to Composables
                CompositionLocalProvider(
                    LocalMainActivity provides this
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(
                            navController = navController,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    /**
     * Request enable Bluetooth from ViewModel/Composable
     */
    fun requestEnableBluetooth() {
        val intent = systemSettingsHelper.getEnableBluetoothIntent()
        enableBluetoothLauncher.launch(intent)
    }

    /**
     * Request enable Location from ViewModel/Composable
     */
    fun requestEnableLocation() {
        lifecycleScope.launch {
            systemSettingsHelper.requestEnableLocation(
                context = this@MainActivity,
                locationSettingsLauncher = enableLocationLauncher
            )
        }
    }
}

/**
 * CompositionLocal untuk provide MainActivity instance to Composables
 */
val LocalMainActivity = compositionLocalOf<MainActivity> {
    error("MainActivity not provided")
}