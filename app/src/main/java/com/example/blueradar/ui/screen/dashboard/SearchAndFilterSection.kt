package com.example.blueradar.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    rssiThreshold: Int,
    onRssiThresholdChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or MAC address") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // RSSI Filter
        Text(
            text = "RSSI Threshold: ${rssiThreshold} dBm",
            style = MaterialTheme.typography.labelLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("-100", style = MaterialTheme.typography.labelSmall)
            
            Slider(
                value = rssiThreshold.toFloat(),
                onValueChange = { onRssiThresholdChange(it.toInt()) },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                valueRange = -100f..-30f,
                steps = 69  // -100 to -30 = 70 values, minus 2 endpoints = 68 steps
            )
            
            Text("-30", style = MaterialTheme.typography.labelSmall)
        }

        Text(
            text = "Only show devices with signal ≥ ${rssiThreshold} dBm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
