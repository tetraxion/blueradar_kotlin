package com.example.blueradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.blueradar.ui.navigation.AppNavHost
import com.example.blueradar.ui.theme.BlueRadarTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
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
                
                // Handle configuration changes (rotation)
                DisposableEffect(Unit) {
                    onDispose {
                        // Cleanup if needed
                    }
                }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}