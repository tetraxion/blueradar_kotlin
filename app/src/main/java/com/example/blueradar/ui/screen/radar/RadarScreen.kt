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
import com.example.blueradar.ui.components.BlueRadarBottomNavBar
import com.example.blueradar.ui.components.NavTab
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "360° Live Radar Scope",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
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
                currentTab = NavTab.RADAR,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.SCANNER -> onNavigateBack()
                        NavTab.RADAR -> {}
                        NavTab.HISTORY -> onNavigateBack()
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
                // Scope Stats Header Banner
                RadarScopeHeaderBanner(
                    deviceCount = activeDevices.size,
                    totalCount = uiState.allDevices.size
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

            // Bluetooth Status Dialog
            if (showBluetoothDialog) {
                AlertDialog(
                    onDismissRequest = { showBluetoothDialog = false },
                    title = { Text("Bluetooth Status", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("• Adapter: Enabled & Operational")
                            Text("• Radar Mode: 360° Multi-Target Scope")
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
    totalCount: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "• REALTIME SWEEP",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$deviceCount Devices on Scope",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "SWEEP: 1.2s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Spatial360RadarScope(devices: List<BleDevice>) {
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

    Box(
        modifier = Modifier.size(250.dp),
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = signalColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = signalColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = device.name ?: "Unknown Peripheral",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${device.mac} • ${device.rssi} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = RssiUtils.formatDistance(device.estimatedDistance),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onTrackDetail,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
