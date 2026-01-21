package com.aettner.plates

import androidx.compose.runtime.Composable

@Composable
fun MapScreen(completedStates: Set<String>) {
    GermanyMap(completedStates = completedStates)
}
