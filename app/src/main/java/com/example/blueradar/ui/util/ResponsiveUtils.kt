package com.example.blueradar.ui.util

import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberWindowWidthSizeClass(width: Dp): WindowWidthSizeClass {
    return when {
        width < 600.dp -> WindowWidthSizeClass.COMPACT
        width < 840.dp -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }
}

@Composable
fun isLandscape(): Boolean {
    return LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
}

@Composable
fun ResponsiveLayout(
    portraitContent: @Composable () -> Unit,
    landscapeContent: @Composable () -> Unit
) {
    BoxWithConstraints {
        val landscape = isLandscape() || maxWidth >= 600.dp
        if (landscape) {
            landscapeContent()
        } else {
            portraitContent()
        }
    }
}
