package com.example.blueradar.ui.screen.radar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blueradar.domain.model.BleDevice
import com.example.blueradar.domain.model.SignalCategory
import com.example.blueradar.domain.util.RssiUtils
import com.example.blueradar.ui.components.BlueRadarAppBar
import com.example.blueradar.ui.components.BlueRadarBottomNavBar
import com.example.blueradar.ui.components.NavTab
import com.example.blueradar.ui.components.SystemStatusDialog
import com.example.blueradar.ui.util.isLandscape
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel(),
    scannerViewModel: com.example.blueradar.ui.screen.dashboard.ScannerViewModel = hiltViewModel(),
    navController: androidx.navigation.NavHostController? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannerState by scannerViewModel.uiState.collectAsState()

    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var selectedRangeFilter by remember { mutableStateOf("ALL") }

    val activeDevices = remember(uiState.allDevices, selectedRangeFilter) {
        when (selectedRangeFilter) {
            "NEAR" -> uiState.allDevices.filter { it.estimatedDistance < 3.0 }
            "MID" -> uiState.allDevices.filter { it.estimatedDistance in 3.0..10.0 }
            "FAR" -> uiState.allDevices.filter { it.estimatedDistance > 10.0 }
            else -> uiState.allDevices
        }
    }

    Scaffold(
        topBar = {
            BlueRadarAppBar(
                title = "Radar View",
                badgeText = "360° Live",
                badgeColor = Color(0xFF8B5CF6),
                isActive = scannerState.isScanning,
                isBluetoothEnabled = scannerState.isBluetoothEnabled,
                isLocationEnabled = scannerState.isLocationEnabled,
                onStatusClick = { showBluetoothDialog = true },
                onSettingsClick = { showSettingsDialog = true }
            )
        },
        bottomBar = {
            BlueRadarBottomNavBar(
                currentTab = NavTab.RADAR,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.SCANNER -> {
                            if (navController != null) {
                                navController.navigate("dashboard") {
                                    popUpTo("dashboard") { inclusive = false }
                                    launchSingleTop = true
                                }
                            } else {
                                onNavigateBack()
                            }
                        }
                        NavTab.RADAR -> {}
                        NavTab.HISTORY -> {
                            if (navController != null) {
                                navController.navigate("history") {
                                    launchSingleTop = true
                                }
                            }
                        }
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Scope Stats Header Banner dengan Scan Control
                RadarScopeHeaderBanner(
                    deviceCount = activeDevices.size,
                    totalCount = uiState.allDevices.size,
                    isScanning = scannerState.isScanning,
                    onToggleScan = {
                        if (scannerState.isScanning) {
                            scannerViewModel.stopScanning()
                        } else {
                            scannerViewModel.startScanning()
                        }
                    }
                )

                // Range Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedRangeFilter == "ALL",
                        onClick = { selectedRangeFilter = "ALL" },
                        label = { Text("All Range (${uiState.allDevices.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedRangeFilter == "NEAR",
                        onClick = { selectedRangeFilter = "NEAR" },
                        label = { Text("Near (< 3m)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedRangeFilter == "MID",
                        onClick = { selectedRangeFilter = "MID" },
                        label = { Text("Mid (3 - 10m)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                    FilterChip(
                        selected = selectedRangeFilter == "FAR",
                        onClick = { selectedRangeFilter = "FAR" },
                        label = { Text("Far (> 10m)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Center 360 Spatial Radar Scope Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Spatial360RadarScope(devices = activeDevices)
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // List of Detected Beacons on Scope
                Text(
                    text = "DISCOVERED BEACONS ON SCOPE",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (activeDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No devices detected in current radar scope range",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = activeDevices, key = { it.mac }) { device ->
                            ScopeDeviceItemCard(
                                device = device,
                                onTrackDetail = { onNavigateToDetail(device.mac) }
                            )
                        }
                    }
                }
            }

            // System Status Dialog (Reusable)
            val mainActivity = com.example.blueradar.LocalMainActivity.current
            SystemStatusDialog(
                show = showBluetoothDialog,
                isBluetoothEnabled = scannerState.isBluetoothEnabled,
                isLocationEnabled = scannerState.isLocationEnabled,
                onDismiss = { showBluetoothDialog = false },
                onEnableBluetooth = { mainActivity.requestEnableBluetooth() },
                onEnableLocation = { mainActivity.requestEnableLocation() },
                additionalInfo = "Radar Mode: 360° Multi-Target"
            )

            // Scanner Settings Dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Radar Scope Settings", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Pengaturan Frekuensi Radar Sweep & Filter Range Sinyal")
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
        }
    }
}

@Composable
fun RadarScopeHeaderBanner(
    deviceCount: Int,
    totalCount: Int,
    isScanning: Boolean,
    onToggleScan: () -> Unit
) {
    val landscape = isLandscape()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = if (landscape) 2.dp else 4.dp
            )
    ) {
        // Clean Scan Control Button (tata letak berbeda dari Scanner)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stats Info
            Column {
                Text(
                    text = "DETECTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = if (landscape) 9.sp else 10.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$deviceCount",
                        style = if (landscape) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "devices",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = if (landscape) 10.sp else 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scan Toggle Button (icon-only, clean design)
            FloatingActionButton(
                onClick = onToggleScan,
                modifier = Modifier.size(if (landscape) 48.dp else 56.dp),
                containerColor = if (isScanning) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.surfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(
                    imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isScanning) "Stop Scan" else "Start Scan",
                    tint = if (isScanning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(if (landscape) 20.dp else 24.dp)
                )
            }
        }

        if (!landscape) {
            Spacer(modifier = Modifier.height(8.dp))

            // Minimal Status Bar - only in portrait
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isScanning) Color(0xFF8B5CF6) else Color.Gray,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScanning) "Scanning" else "Idle",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Sweep: 1.2s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            // Landscape: just status dot inline
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (isScanning) Color(0xFF8B5CF6) else Color.Gray,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isScanning) "Scanning" else "Idle",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Spatial360RadarScope(devices: List<BleDevice>) {
    val landscape = isLandscape()
    
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val radarSize = if (landscape) 180.dp else 250.dp

    Box(
        modifier = Modifier.size(radarSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 - 10f

            // Background Circle
            drawCircle(
                color = Color(0xFF0B132B).copy(alpha = 0.05f),
                radius = maxRadius,
                center = center
            )

            // 4 Concentric Radar Rings
            for (i in 1..4) {
                val r = maxRadius * (i / 4f)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // Radar Axis Crosshairs
            drawLine(
                color = primaryColor.copy(alpha = 0.25f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1f
            )

            // Animated Rotating Sweep Cone
            val sweepPath = Path().apply {
                moveTo(center.x, center.y)
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        center.x - maxRadius,
                        center.y - maxRadius,
                        center.x + maxRadius,
                        center.y + maxRadius
                    ),
                    startAngleDegrees = sweepAngle - 45f,
                    sweepAngleDegrees = 45f,
                    forceMoveTo = false
                )
                close()
            }

            drawPath(
                path = sweepPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.05f)
                    ),
                    center = center,
                    radius = maxRadius
                )
            )

            // Plot all active discovered devices as spatial blip nodes
            if (devices.isNotEmpty()) {
                val total = devices.size
                devices.forEachIndexed { index, dev ->
                    val d = dev.estimatedDistance
                    val factor = when {
                        d < 1.0 -> 0.2f
                        d < 3.0 -> 0.4f
                        d < 10.0 -> 0.65f
                        d < 20.0 -> 0.85f
                        else -> 0.95f
                    }
                    val r = maxRadius * factor
                    val angleDeg = (index * (360.0 / total)) + 30.0
                    val angleRad = Math.toRadians(angleDeg)
                    val bx = center.x + (r * cos(angleRad)).toFloat()
                    val by = center.y - (r * sin(angleRad)).toFloat()

                    val blipColor = try {
                        Color(android.graphics.Color.parseColor(dev.signalCategory.colorHex))
                    } catch (e: Exception) {
                        primaryColor
                    }

                    // Outer pulse ring
                    drawCircle(
                        color = blipColor.copy(alpha = 0.35f),
                        radius = 12f,
                        center = Offset(bx, by)
                    )
                    // Solid blip dot
                    drawCircle(
                        color = blipColor,
                        radius = 7f,
                        center = Offset(bx, by)
                    )
                }
            }
        }

        // Center Phone Icon Indicator
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun ScopeDeviceItemCard(
    device: BleDevice,
    onTrackDetail: () -> Unit
) {
    val signalColor = try {
        Color(android.graphics.Color.parseColor(device.signalCategory.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Clean minimal card design (berbeda dari Scanner)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Minimal signal indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(signalColor, CircleShape)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Distance badge (minimal)
                Text(
                    text = RssiUtils.formatDistance(device.estimatedDistance),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = signalColor
                )

                // Minimal track button
                IconButton(
                    onClick = onTrackDetail,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Track",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
