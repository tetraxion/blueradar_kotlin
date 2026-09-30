package com.example.blueradar.ui.screen.radar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.blueradar.domain.model.SignalCategory
import com.example.blueradar.domain.util.RssiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    macAddress: String,
    onNavigateBack: () -> Unit,
    viewModel: RadarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Radar View") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.device == null && !uiState.isTracking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Waiting for device signal...")
                    Text(
                        text = macAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Device Info
                uiState.device?.let { device ->
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.mac,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Radar Visualization
                RadarVisualization(
                    signalCategory = uiState.signalCategory,
                    distance = uiState.estimatedDistance
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Signal Info Cards
                SignalInfoCards(
                    rssi = uiState.device?.rssi ?: -100,
                    distance = uiState.estimatedDistance,
                    signalCategory = uiState.signalCategory
                )
            }
        }
    }
}

@Composable
fun RadarVisualization(
    signalCategory: SignalCategory,
    distance: Double
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val color = Color(android.graphics.Color.parseColor(signalCategory.colorHex))

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2

            // Draw concentric circles (zones)
            for (i in 1..5) {
                val radius = maxRadius * i / 5
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.2f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }

            // Draw target point based on distance
            val targetRadius = when {
                distance < 1 -> maxRadius * 0.2f
                distance < 3 -> maxRadius * 0.4f
                distance < 10 -> maxRadius * 0.6f
                distance < 20 -> maxRadius * 0.8f
                else -> maxRadius
            }

            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = targetRadius,
                center = center
            )

            drawCircle(
                color = color,
                radius = 20f,
                center = center
            )
        }

        // Center icon
        Text(
            text = "📱",
            style = MaterialTheme.typography.displayMedium
        )
    }
}

@Composable
fun SignalInfoCards(
    rssi: Int,
    distance: Double,
    signalCategory: SignalCategory
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RSSI Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(android.graphics.Color.parseColor(signalCategory.colorHex)).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Signal Strength",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$rssi dBm",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(signalCategory.colorHex))
                )
            }
        }

        // Distance Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Estimated Distance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = RssiUtils.formatDistance(distance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Category Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Signal Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = signalCategory.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(signalCategory.colorHex))
                )
                Text(
                    text = signalCategory.estimatedDistanceLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
