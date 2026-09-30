package com.example.blueradar.ui.screen.dashboard

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    onNavigateToRadar: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Request permissions
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BlueRadar Scanner") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "History")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (permissionsState.allPermissionsGranted) {
                        if (uiState.isScanning) {
                            viewModel.stopScanning()
                        } else {
                            viewModel.startScanning()
                        }
                    } else {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                }
            ) {
                Icon(
                    imageVector = if (uiState.isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isScanning) "Stop Scan" else "Start Scan"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Permission check
            if (!permissionsState.allPermissionsGranted) {
                PermissionRationale(
                    onRequestPermission = {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                )
            } else if (!uiState.isBluetoothEnabled) {
                // Bluetooth disabled warning
                BluetoothDisabledWarning()
            } else {
                // Search & Filter Section
                SearchAndFilterSection(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    rssiThreshold = uiState.rssiThreshold,
                    onRssiThresholdChange = viewModel::updateRssiThreshold
                )

                Divider()

                // Device List
                if (uiState.isScanning) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.filteredDevices.isEmpty()) {
                    EmptyDeviceList(isScanning = uiState.isScanning)
                } else {
                    DeviceList(
                        devices = uiState.filteredDevices,
                        onDeviceClick = { device ->
                            onNavigateToRadar(device.mac)
                        }
                    )
                }

                // Error message
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = viewModel::clearError) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRationale(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bluetooth & Location Permission Required",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "BlueRadar needs Bluetooth and Location permissions to scan for nearby BLE devices.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permissions")
        }
    }
}

@Composable
fun BluetoothDisabledWarning() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bluetooth is Disabled",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please enable Bluetooth in your device settings to start scanning for BLE devices.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun EmptyDeviceList(isScanning: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isScanning) "Scanning for devices..." else "Tap the play button to start scanning",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
