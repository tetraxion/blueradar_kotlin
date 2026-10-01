package com.example.blueradar.ui.screen.dashboard

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blueradar.ui.components.BlueRadarBottomNavBar
import com.example.blueradar.ui.components.NavTab
import com.example.blueradar.ui.util.isLandscape
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

    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNoTargetDialog by remember { mutableStateOf(false) }

    val landscape = isLandscape()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "BlueRadar",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Live Scanner Badge
                        Surface(
                            color = if (uiState.isScanning) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (uiState.isScanning) "• Scanner" else "• Idle",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isScanning) Color(0xFF10B981) else Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showBluetoothDialog = true }) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth Status", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            BlueRadarBottomNavBar(
                currentTab = NavTab.SCANNER,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.SCANNER -> {}
                        NavTab.RADAR -> {
                            if (uiState.filteredDevices.isNotEmpty()) {
                                onNavigateToRadar(uiState.filteredDevices.first().mac)
                            } else {
                                showNoTargetDialog = true
                            }
                        }
                        NavTab.HISTORY -> onNavigateToHistory()
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!permissionsState.allPermissionsGranted) {
                PermissionRationale(onRequestPermission = { permissionsState.launchMultiplePermissionRequest() })
            } else if (!uiState.isBluetoothEnabled) {
                BluetoothDisabledWarning()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Scanning Control & Stats Card
                    ScanningStatsHeaderCard(
                        isScanning = uiState.isScanning,
                        deviceCount = uiState.filteredDevices.size,
                        onToggleScan = {
                            if (uiState.isScanning) viewModel.stopScanning() else viewModel.startScanning()
                        }
                    )

                    // Search & Filter Header
                    SearchAndFilterSection(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        rssiThreshold = uiState.rssiThreshold,
                        onRssiThresholdChange = viewModel::updateRssiThreshold,
                        isSortAscending = uiState.isSortAscending,
                        onToggleSort = viewModel::toggleSortOrder
                    )

                    if (uiState.isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Device List
                    if (uiState.filteredDevices.isEmpty()) {
                        EmptyDeviceList(isScanning = uiState.isScanning)
                    } else {
                        DeviceList(
                            devices = uiState.filteredDevices,
                            onDeviceClick = { device -> onNavigateToRadar(device.mac) },
                            useGrid = landscape
                        )
                    }
                }
            }

            // Bluetooth Status Dialog
            if (showBluetoothDialog) {
                AlertDialog(
                    onDismissRequest = { showBluetoothDialog = false },
                    title = { Text("Bluetooth Status", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("• Adapter: Enabled & Operational")
                            Text("• BLE Mode: High Speed LE Scan")
                            Text("• Status: ${if (uiState.isBluetoothEnabled) "Active" else "Disabled"}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBluetoothDialog = false }) {
                            Text("Tutup", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Scanner Settings Dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Scanner Settings", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Ubah Filter Cutoff Signal (RSSI):", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cutoff Saat Ini: ${uiState.rssiThreshold} dBm", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Slider(
                                value = uiState.rssiThreshold.toFloat(),
                                onValueChange = { viewModel.updateRssiThreshold(it.toInt()) },
                                valueRange = -100f..-40f
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSettingsDialog = false }) {
                            Text("Simpan", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // No Target Selected Dialog for RADAR VIEW
            if (showNoTargetDialog) {
                AlertDialog(
                    onDismissRequest = { showNoTargetDialog = false },
                    title = { Text("Target Radar Belum Ada", fontWeight = FontWeight.Bold) },
                    text = { Text("Tidak ada perangkat BLE yang ditemukan. Silakan jalankan 'Scanning' dan pilih perangkat dari daftar untuk ditrack pada Radar.") },
                    confirmButton = {
                        TextButton(onClick = { showNoTargetDialog = false }) {
                            Text("Mengerti", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun ScanningStatsHeaderCard(
    isScanning: Boolean,
    deviceCount: Int,
    onToggleScan: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active Scan Button Pill
                Button(
                    onClick = onToggleScan,
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) Color(0xFF059669) else Color(0xFFDC2626)
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = if (isScanning) "• Scanning Active..." else "• Scanning Stopped",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleScan,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats 3-Grid (TARGETS Live, SWEEP RATE, SAMPLING)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                HeaderStatBox(title = "TARGETS", value = "$deviceCount Live")
                HeaderStatBox(title = "SWEEP RATE", value = "1.2 sec")
                HeaderStatBox(title = "SAMPLING", value = "48 KH")
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(6.dp))

            // Hardware info bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bluetooth LE: Ready",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "RTS 512",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderStatBox(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PermissionRationale(onRequestPermission: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bluetooth & Location Required",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "BlueRadar needs Bluetooth and Location permissions to scan for nearby BLE devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Grant Permissions", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BluetoothDisabledWarning() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Please enable Bluetooth in your device settings to start scanning for BLE devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyDeviceList(isScanning: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isScanning) "Scanning for nearby BLE devices..." else "Tap 'Start Scan' to discover nearby devices",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
