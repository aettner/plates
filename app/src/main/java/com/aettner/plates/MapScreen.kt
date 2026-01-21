package com.aettner.plates

import androidx.compose.runtime.Composable

@Composable
fun MapScreen(licensePlates: List<LicensePlate>, seenPlates: Set<String>, useDarkTheme: Boolean) {
    GermanyMap(licensePlates = licensePlates, seenPlates = seenPlates, useDarkTheme = useDarkTheme)
}
