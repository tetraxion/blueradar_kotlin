package com.example.blueradar.ui.screen.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blueradar.ui.util.isLandscape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAndFilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    rssiThreshold: Int,
    onRssiThresholdChange: (Int) -> Unit,
    isSortAscending: Boolean = false,
    onToggleSort: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val landscape = isLandscape()
    var selectedFilterChip by remember { mutableStateOf("ALL") }
    var showRssiSlider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = if (landscape) 1.dp else 2.dp
            )
    ) {
        // Search Input Bar - hide in landscape to save space
        if (!landscape) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                placeholder = {
                    Text(
                        "Search device name or MAC...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Horizontal Filter Chips Row (compact in landscape)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!landscape) {
                // RSSI Threshold Chip Selector - only in portrait
                FilterChip(
                    selected = showRssiSlider,
                    onClick = { showRssiSlider = !showRssiSlider },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "RSSI: $rssiThreshold dBm",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // "All" Filter Chip
            FilterChip(
                selected = selectedFilterChip == "ALL",
                onClick = {
                    selectedFilterChip = "ALL"
                    onRssiThresholdChange(-100)
                },
                label = { Text("All", fontSize = if (landscape) 11.sp else 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            // "Near" Filter Chip
            FilterChip(
                selected = selectedFilterChip == "NEAR",
                onClick = {
                    selectedFilterChip = "NEAR"
                    onRssiThresholdChange(-70)
                },
                label = { Text("Near", fontSize = if (landscape) 11.sp else 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            // "Strong" Filter Chip
            FilterChip(
                selected = selectedFilterChip == "STRONG",
                onClick = {
                    selectedFilterChip = "STRONG"
                    onRssiThresholdChange(-60)
                },
                label = { Text("Strong", fontSize = if (landscape) 11.sp else 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )
            
            if (landscape) {
                // Sort toggle chip in landscape
                FilterChip(
                    selected = false,
                    onClick = onToggleSort,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Sort",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sort", fontSize = 11.sp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Expandable RSSI Slider Box - only in portrait
        if (showRssiSlider && !landscape) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RSSI Threshold",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "≥ $rssiThreshold dBm",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = rssiThreshold.toFloat(),
                        onValueChange = { onRssiThresholdChange(it.toInt()) },
                        valueRange = -100f..-30f,
                        steps = 69,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        if (!landscape) {
            Spacer(modifier = Modifier.height(6.dp))

            // Sort Header Bar - only in portrait
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSortAscending) "Sorted: Weakest First" else "Sorted: Strongest First",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onToggleSort() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOGGLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
