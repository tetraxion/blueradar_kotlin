package com.example.blueradar.ui.screen.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.blueradar.domain.model.BleDevice
import com.example.blueradar.domain.util.RssiUtils

@Composable
fun DeviceList(
    devices: List<BleDevice>,
    onDeviceClick: (BleDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = devices,
            key = { it.mac }
        ) { device ->
            DeviceItem(
                device = device,
                onClick = { onDeviceClick(device) }
            )
        }
    }
}

@Composable
fun DeviceItem(
    device: BleDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bluetooth Icon dengan warna kategori sinyal
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(device.signalCategory.colorHex)),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Device Info
            Column(modifier = Modifier.weight(1f)) {
                // Device Name
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // MAC Address
                Text(
                    text = device.mac,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Signal Category
                Surface(
                    color = Color(android.graphics.Color.parseColor(device.signalCategory.colorHex)).copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = device.signalCategory.label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(android.graphics.Color.parseColor(device.signalCategory.colorHex))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // RSSI & Distance
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = RssiUtils.formatDistance(device.estimatedDistance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
