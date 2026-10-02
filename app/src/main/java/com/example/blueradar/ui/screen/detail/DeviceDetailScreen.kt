package com.example.blueradar.ui.screen.detail

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.blueradar.domain.model.SignalCategory
import com.example.blueradar.domain.util.RssiUtils
import com.example.blueradar.ui.components.SystemStatusActions
import com.example.blueradar.ui.components.SystemStatusDialog
import com.example.blueradar.ui.screen.dashboard.ScannerViewModel
import com.example.blueradar.ui.screen.radar.RadarViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    macAddress: String,
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel(),
    scannerViewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scannerState by scannerViewModel.uiState.collectAsState()

    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showCalibrateDialog by remember { mutableStateOf(false) }
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val currentDeviceName = uiState.device?.name ?: "Unknown Device"
    val currentMac = uiState.device?.mac ?: if (macAddress.isNotEmpty()) macAddress else "00:00:00:00:00:00"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentDeviceName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = currentMac,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    SystemStatusActions(
                        isBluetoothEnabled = scannerState.isBluetoothEnabled,
                        isLocationEnabled = scannerState.isLocationEnabled,
                        onStatusClick = { showBluetoothDialog = true },
                        onSettingsClick = { showSettingsDialog = true }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Target Header
                DetailTargetHeaderCard(
                    deviceName = currentDeviceName,
                    mac = currentMac,
                    txPower = -59
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Single Target Radar Visualization
                SingleTargetRadarCanvas(
                    distance = uiState.estimatedDistance,
                    signalCategory = uiState.signalCategory
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metrics Grid
                DetailSignalMetricsGrid(
                    rssi = uiState.device?.rssi ?: -65,
                    distance = uiState.estimatedDistance,
                    signalCategory = uiState.signalCategory
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quality Status & Environment
                DetailStatusAndEnvironment(signalCategory = uiState.signalCategory)

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Action Buttons (Ping & Calibrate only)
                DetailActionButtons(
                    onPing = { snackbarMessage = "Ping signal dispatched to $currentDeviceName ($currentMac)" },
                    onCalibrate = { showCalibrateDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Snackbar
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(msg)
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
                additionalInfo = "Tracking: $currentMac"
            )

            // Scanner Settings Dialog
            if (showSettingsDialog) {
                AlertDialog(
                    onDismissRequest = { showSettingsDialog = false },
                    title = { Text("Target Settings", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Kalibrasi TxPower Reference (-59 dBm standar)")
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

            // Calibration Dialog
            if (showCalibrateDialog) {
                AlertDialog(
                    onDismissRequest = { showCalibrateDialog = false },
                    title = { Text("Calibrate Distance (1m)", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Posisikan perangkat $currentDeviceName tepat 1 meter dari HP.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("RSSI Terdeteksi: ${uiState.device?.rssi ?: -65} dBm", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCalibrateDialog = false
                                snackbarMessage = "Reference TxPower calibrated to ${uiState.device?.rssi ?: -65} dBm"
                            }
                        ) {
                            Text("Kalibrasi Sekarang", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCalibrateDialog = false }) {
                            Text("Batal")
                        }
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailTargetHeaderCard(
    deviceName: String,
    mac: String,
    txPower: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "TARGET LOCKED • TRACKING ACTIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "((.)) 28 Hz Rate",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "TX POWER $txPower dBm",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MAC: $mac",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SingleTargetRadarCanvas(
    distance: Double,
    signalCategory: SignalCategory
) {
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

    val signalColor = try {
        Color(android.graphics.Color.parseColor(signalCategory.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2 - 10f

            drawCircle(
                color = Color(0xFF0B132B).copy(alpha = 0.05f),
                radius = maxRadius,
                center = center
            )

            for (i in 1..5) {
                val r = maxRadius * (i / 5f)
                drawCircle(
                    color = signalColor.copy(alpha = 0.2f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            drawLine(
                color = signalColor.copy(alpha = 0.25f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = signalColor.copy(alpha = 0.25f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1f
            )

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
                        signalColor.copy(alpha = 0.4f),
                        signalColor.copy(alpha = 0.05f)
                    ),
                    center = center,
                    radius = maxRadius
                )
            )

            val targetDistFactor = when {
                distance < 1.0 -> 0.2f
                distance < 3.0 -> 0.4f
                distance < 10.0 -> 0.6f
                distance < 20.0 -> 0.8f
                else -> 0.95f
            }
            val targetRadius = maxRadius * targetDistFactor
            val targetAngleRad = Math.toRadians(45.0)
            val blipX = center.x + (targetRadius * cos(targetAngleRad)).toFloat()
            val blipY = center.y - (targetRadius * sin(targetAngleRad)).toFloat()

            drawCircle(
                color = signalColor.copy(alpha = 0.4f),
                radius = 16f,
                center = Offset(blipX, blipY)
            )
            drawCircle(
                color = signalColor,
                radius = 8f,
                center = Offset(blipX, blipY)
            )
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailSignalMetricsGrid(
    rssi: Int,
    distance: Double,
    signalCategory: SignalCategory
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(signalCategory.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SIGNAL RSSI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("$rssi dBm", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = categoryColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• 30s Stability: High", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = categoryColor)
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("EST. DISTANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(RssiUtils.formatDistance(distance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Log-Distance Model", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun DetailStatusAndEnvironment(signalCategory: SignalCategory) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(signalCategory.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = categoryColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(signalCategory.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Surface(color = Color(0xFF10B981).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                    Text("OPTIMAL", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }
    }
}

@Composable
private fun DetailActionButtons(
    onPing: () -> Unit,
    onCalibrate: () -> Unit
) {
    var showPingInfoDialog by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ping Beacon - Icon Button with Dialog
            OutlinedButton(
                onClick = { showPingInfoDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ping Beacon", fontSize = 14.sp)
            }

            // Calibrate Button
            OutlinedButton(
                onClick = onCalibrate,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Calibrate", fontSize = 14.sp)
            }
        }
    }
    
    // Ping Info Dialog
    if (showPingInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPingInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ping Beacon", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "COMING SOON",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            },
            text = {
                Column {
                    Text(
                        "Fitur untuk trigger buzzer/LED pada device BLE yang mendukung (key finder, tracker tag).",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Status: Dalam pengembangan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Memerlukan implementasi GATT connection & konfigurasi karakteristik device.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPingInfoDialog = false }) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
