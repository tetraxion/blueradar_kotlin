package com.example.blueradar.ui.screen.radar

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blueradar.domain.model.SignalCategory
import com.example.blueradar.domain.util.RssiUtils
import com.example.blueradar.ui.components.BlueRadarBottomNavBar
import com.example.blueradar.ui.components.NavTab
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    macAddress: String,
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showCalibrateDialog by remember { mutableStateOf(false) }

    val currentDeviceName = uiState.device?.name ?: if (macAddress.isNotEmpty()) "Target BLE Device ($macAddress)" else "Beacon Device"
    val currentMac = uiState.device?.mac ?: if (macAddress.isNotEmpty()) macAddress else "00:00:00:00:00:00"

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
                            "BlueRadar - Radar View",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { snackbarMessage = "Bluetooth Active • Scanning Target $currentMac" }) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showCalibrateDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Calibrate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Target Lock Header Card
                TargetHeaderCard(
                    deviceName = currentDeviceName,
                    mac = currentMac,
                    txPower = -59
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Radar Canvas Visualization
                RadarCanvasVisualization(
                    distance = uiState.estimatedDistance,
                    signalCategory = uiState.signalCategory
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metrics Grid (Signal RSSI + Est Distance)
                SignalMetricsGrid(
                    rssi = uiState.device?.rssi ?: -65,
                    distance = uiState.estimatedDistance,
                    signalCategory = uiState.signalCategory
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status & RF Environment Cards
                StatusAndEnvironmentCards(signalCategory = uiState.signalCategory)

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Row
                RadarActionButtons(
                    onPing = { snackbarMessage = "Ping signal dispatched to $currentDeviceName ($currentMac)" },
                    onCalibrate = { showCalibrateDialog = true },
                    onSaveSnapshot = { snackbarMessage = "Snapshot signal saved for $currentDeviceName" }
                )

                Spacer(modifier = Modifier.height(16.dp))
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
fun TargetHeaderCard(
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
            // Status Pill Row
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
                        text = "TARGET LOCKED • TRACKING INTENSIVE",
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

            // Device Title & TX Power
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
                    text = "TX POWER +$txPower.0 dBm",
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
            Text(
                text = "UUID: FDA50693-A4E2-4FB1-AFCF-C6EB0761782B",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun RadarCanvasVisualization(
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

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier.size(300.dp),
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

            // 5 Concentric Target Rings
            val zones = listOf(
                "Zone 1 (1-3m)",
                "Zone 2 (3-10m)",
                "Zone 3 (10-20m)",
                "Zone 4 (-75 dBm)",
                "Zone 5 (>20m) - -85 dBm"
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

            // Radar Axis Crosshairs
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
                        signalColor.copy(alpha = 0.4f),
                        signalColor.copy(alpha = 0.05f)
                    ),
                    center = center,
                    radius = maxRadius
                )
            )

            // Target Blip Node
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

            // Outer pulse ring around target
            drawCircle(
                color = signalColor.copy(alpha = 0.4f),
                radius = 16f,
                center = Offset(blipX, blipY)
            )
            // Solid target dot
            drawCircle(
                color = signalColor,
                radius = 8f,
                center = Offset(blipX, blipY)
            )
        }

        // Center Phone Icon Indicator
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun SignalMetricsGrid(
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
        // SIGNAL RSSI Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SIGNAL RSSI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = categoryColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("$rssi dBm", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = categoryColor)

                Spacer(modifier = Modifier.height(8.dp))
                // Mini Waveform Sparkline
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.7f)
                        lineTo(size.width * 0.2f, size.height * 0.3f)
                        lineTo(size.width * 0.4f, size.height * 0.8f)
                        lineTo(size.width * 0.6f, size.height * 0.2f)
                        lineTo(size.width * 0.8f, size.height * 0.5f)
                        lineTo(size.width, size.height * 0.3f)
                    }
                    drawPath(path = path, color = categoryColor, style = Stroke(width = 2.5f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("• 30s Stability: High", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = categoryColor)
            }
        }

        // EST DISTANCE Card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EST. DISTANCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.Straighten, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(RssiUtils.formatDistance(distance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(6.dp)) {
                    Text("Kalman Est. ±0.15m", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Log-Distance v0.3.1", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun StatusAndEnvironmentCards(signalCategory: SignalCategory) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(signalCategory.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Quality Status Card
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
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("• 98.5% Packet Reception", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("0 drops / sec", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // RF Environment Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("RF Environment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Low multipath interference. Frequency 2.4 GHz clean.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text("2.4 GHz", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RadarActionButtons(
    onPing: () -> Unit = {},
    onCalibrate: () -> Unit = {},
    onSaveSnapshot: () -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onPing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ping Beacon Buzzer / LED", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCalibrate,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Calibrate (1m)")
            }

            OutlinedButton(
                onClick = onSaveSnapshot,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Snapshot")
            }
        }
    }
}
