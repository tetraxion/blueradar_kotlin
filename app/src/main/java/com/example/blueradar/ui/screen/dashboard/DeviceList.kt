package com.example.blueradar.ui.screen.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blueradar.domain.model.BleDevice
import com.example.blueradar.domain.util.RssiUtils

@Composable
fun DeviceList(
    devices: List<BleDevice>,
    onDeviceClick: (BleDevice) -> Unit,
    modifier: Modifier = Modifier,
    useGrid: Boolean = false,
    headerContent: (@Composable () -> Unit)? = null
) {
    if (useGrid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (headerContent != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    headerContent()
                }
            }
            items(
                items = devices,
                key = { device -> device.mac },
                contentType = { "device_item" }
            ) { device ->
                DeviceItem(
                    device = device,
                    onTrackClick = { onDeviceClick(device) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (headerContent != null) {
                item {
                    Column {
                        headerContent()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
            items(
                items = devices,
                key = { device -> device.mac },
                contentType = { "device_item" }
            ) { device ->
                DeviceItem(
                    device = device,
                    onTrackClick = { onDeviceClick(device) }
                )
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: BleDevice,
    onTrackClick: () -> Unit
) {
    val signalColor = try {
        Color(android.graphics.Color.parseColor(device.signalCategory.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val icon: ImageVector = when {
        device.name?.contains("WH-1000", ignoreCase = true) == true ||
        device.name?.contains("Headphone", ignoreCase = true) == true -> Icons.Default.Headset
        device.name?.contains("Watch", ignoreCase = true) == true -> Icons.Default.Watch
        device.name?.contains("Sensor", ignoreCase = true) == true ||
        device.name?.contains("ESP32", ignoreCase = true) == true -> Icons.Default.Sensors
        device.name?.contains("Beacon", ignoreCase = true) == true -> Icons.Default.CellTower
        else -> Icons.Default.Bluetooth
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTrackClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon + Name/MAC + RSSI Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Device Icon Container
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = signalColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = signalColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & MAC
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.mac,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // RSSI & Category Badge (Right Column)
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = signalColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${device.rssi} dBm",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = signalColor
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = device.signalCategory.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = signalColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            // Specs 2x2 Grid (Distance Est, Tx Power Ref, Adv Packet, Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "Distance Est.", value = RssiUtils.formatDistance(device.estimatedDistance))
                MetricItem(label = "Tx Power Ref.", value = "-59 dBm")
                MetricItem(label = "Adv Packet", value = if (device.name?.contains("Beacon", ignoreCase = true) == true) "iBeacon" else "Standard")
                MetricItem(label = "Signal Status", value = device.signalCategory.label.take(10))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Blue Track Action Button
            Button(
                onClick = onTrackClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Track",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Track on Radar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
