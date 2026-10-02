package com.example.blueradar.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable AppBar Actions Component
 * Menampilkan System Status (Bluetooth/Location) dan Settings icons
 * 
 * Usage: SystemStatusActions(isBluetoothEnabled, isLocationEnabled, onStatusClick, onSettingsClick)
 */
@Composable
fun SystemStatusActions(
    isBluetoothEnabled: Boolean,
    isLocationEnabled: Boolean,
    onStatusClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // System Status Icon (CheckCircle/Error)
    IconButton(onClick = onStatusClick) {
        Icon(
            imageVector = if (isBluetoothEnabled && isLocationEnabled) {
                Icons.Default.CheckCircle
            } else {
                Icons.Default.Error
            },
            contentDescription = "System Status",
            tint = if (isBluetoothEnabled && isLocationEnabled) {
                Color(0xFF10B981) // Green
            } else {
                Color(0xFFDC2626) // Red
            }
        )
    }
    
    // Settings Icon
    IconButton(onClick = onSettingsClick) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Reusable System Status Dialog
 * Menampilkan status Bluetooth & Location dengan tombol Enable
 * 
 * Usage: SystemStatusDialog(show, isBluetoothEnabled, isLocationEnabled, onDismiss, onEnableBluetooth, onEnableLocation)
 */
@Composable
fun SystemStatusDialog(
    show: Boolean,
    isBluetoothEnabled: Boolean,
    isLocationEnabled: Boolean,
    onDismiss: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onEnableLocation: () -> Unit,
    additionalInfo: String = "BLE Mode: High Speed LE Scan"
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("System Status", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Bluetooth Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• Bluetooth:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isBluetoothEnabled) "Enabled" else "Disabled",
                                fontWeight = FontWeight.Bold,
                                color = if (isBluetoothEnabled) Color(0xFF10B981) else Color(0xFFDC2626)
                            )
                            if (!isBluetoothEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onEnableBluetooth()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Enable", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Location Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• Location:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLocationEnabled) "Enabled" else "Disabled",
                                fontWeight = FontWeight.Bold,
                                color = if (isLocationEnabled) Color(0xFF10B981) else Color(0xFFDC2626)
                            )
                            if (!isLocationEnabled) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onEnableLocation()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Enable", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("• $additionalInfo", fontSize = 13.sp)
                    Text(
                        text = "• Status: ${if (isBluetoothEnabled && isLocationEnabled) "Ready to Scan" else "Not Ready"}",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
