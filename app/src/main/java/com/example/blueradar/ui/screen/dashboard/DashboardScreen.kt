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
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
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
import com.example.blueradar.LocalMainActivity
import com.example.blueradar.ui.components.BlueRadarAppBar
import com.example.blueradar.ui.components.BlueRadarBottomNavBar
import com.example.blueradar.ui.components.NavTab
import com.example.blueradar.ui.components.SystemStatusDialog
import com.example.blueradar.ui.util.isLandscape
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    onNavigateToRadar: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Get MainActivity reference untuk trigger enable requests
    val mainActivity = com.example.blueradar.LocalMainActivity.current

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

    // Observe enable request flags dan trigger launcher
    LaunchedEffect(uiState.needsBluetoothEnable) {
        if (uiState.needsBluetoothEnable) {
            mainActivity.requestEnableBluetooth()
            viewModel.onEnableRequestHandled()
        }
    }

    LaunchedEffect(uiState.needsLocationEnable) {
        if (uiState.needsLocationEnable) {
            mainActivity.requestEnableLocation()
            viewModel.onEnableRequestHandled()
        }
    }

    // Observe enable results dari MainActivity
    val bluetoothEnableResult by mainActivity.bluetoothEnableResult.collectAsState()
    val locationEnableResult by mainActivity.locationEnableResult.collectAsState()

    LaunchedEffect(bluetoothEnableResult) {
        bluetoothEnableResult?.let { enabled ->
            if (enabled) {
                viewModel.onBluetoothEnabled()
            }
        }
    }

    LaunchedEffect(locationEnableResult) {
        locationEnableResult?.let { enabled ->
            if (enabled) {
                viewModel.onLocationEnabled()
            }
        }
    }

    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNoTargetDialog by remember { mutableStateOf(false) }

    val landscape = isLandscape()

    Scaffold(
        topBar = {
            BlueRadarAppBar(
                title = "BlueRadar",
                badgeText = "Scanner",
                badgeColor = Color(0xFF10B981),
                isActive = uiState.isScanning,
                isBluetoothEnabled = uiState.isBluetoothEnabled,
                isLocationEnabled = uiState.isLocationEnabled,
                onStatusClick = { showBluetoothDialog = true },
                onSettingsClick = { showSettingsDialog = true }
            )
        },
        bottomBar = {
            BlueRadarBottomNavBar(
                currentTab = NavTab.SCANNER,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.SCANNER -> {}
                        NavTab.RADAR -> onNavigateToRadar()
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
                            onDeviceClick = { device -> onNavigateToDetail(device.mac) },
                            useGrid = landscape
                        )
                    }
                }
            }

            // System Status Dialog (Reusable)
            val mainActivity = LocalMainActivity.current
            SystemStatusDialog(
                show = showBluetoothDialog,
                isBluetoothEnabled = uiState.isBluetoothEnabled,
                isLocationEnabled = uiState.isLocationEnabled,
                onDismiss = { showBluetoothDialog = false },
                onEnableBluetooth = { mainActivity.requestEnableBluetooth() },
                onEnableLocation = { mainActivity.requestEnableLocation() },
                additionalInfo = "BLE Mode: High Speed LE Scan"
            )

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
    val landscape = isLandscape()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = if (landscape) 2.dp else 3.dp
            ),
        shape = RoundedCornerShape(if (landscape) 12.dp else 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(
            horizontal = if (landscape) 10.dp else 12.dp,
            vertical = if (landscape) 6.dp else 10.dp
        )) {
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
                    contentPadding = PaddingValues(
                        vertical = if (landscape) 2.dp else 4.dp,
                        horizontal = 12.dp
                    )
                ) {
                    Text(
                        text = if (isScanning) "• Scanning..." else "• Stopped",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (landscape) 11.sp else 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleScan,
                    modifier = Modifier.size(if (landscape) 32.dp else 36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (landscape) 16.dp else 18.dp)
                    )
                }
            }

            if (!landscape) {
                // Stats - only show in portrait
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    HeaderStatBox(title = "TARGETS", value = "$deviceCount Live")
                    HeaderStatBox(title = "SWEEP", value = "1.2 sec")
                    HeaderStatBox(title = "MODE", value = "BLE")
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
            } else {
                // Landscape: show compact stats inline
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$deviceCount Targets",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "BLE Mode",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun BluetoothDisabledWarning(onEnableBluetooth: () -> Unit) {
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
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Bluetooth is Disabled",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "BlueRadar needs Bluetooth to scan for nearby BLE devices. Please enable it to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onEnableBluetooth,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Enable Bluetooth", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LocationDisabledWarning(onEnableLocation: () -> Unit) {
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
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Location is Disabled",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Android requires Location to be enabled for Bluetooth scanning. Please enable it to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onEnableLocation,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Enable Location", fontWeight = FontWeight.Bold)
            }
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
