package com.example.blueradar.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Reusable BlueRadar TopAppBar
 * Digunakan di semua screen dengan logo & badge yang konsisten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueRadarAppBar(
    title: String,
    badgeText: String,
    badgeColor: Color,
    isActive: Boolean = false,
    isBluetoothEnabled: Boolean,
    isLocationEnabled: Boolean,
    onStatusClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo Sistem (konsisten di semua screen)
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
                    title,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Status Badge
                Surface(
                    color = if (isActive) badgeColor.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isActive) "• $badgeText" else "• Idle",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) badgeColor else Color.Gray
                    )
                }
            }
        },
        actions = {
            SystemStatusActions(
                isBluetoothEnabled = isBluetoothEnabled,
                isLocationEnabled = isLocationEnabled,
                onStatusClick = onStatusClick,
                onSettingsClick = onSettingsClick
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * Simplified AppBar for History (tanpa badge)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueRadarAppBarSimple(
    title: String,
    isBluetoothEnabled: Boolean,
    isLocationEnabled: Boolean,
    onStatusClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
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
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        actions = {
            SystemStatusActions(
                isBluetoothEnabled = isBluetoothEnabled,
                isLocationEnabled = isLocationEnabled,
                onStatusClick = onStatusClick,
                onSettingsClick = onSettingsClick
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}
