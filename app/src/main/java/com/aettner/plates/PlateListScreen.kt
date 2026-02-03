
package com.aettner.plates

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlateListScreen(
    licensePlates: List<LicensePlate>,
    seenPlates: Set<String>,
    onPlateClicked: (LicensePlate) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(modifier = modifier, state = listState) {
        items(items = licensePlates, key = { it.code }) { plate ->
            PlateListItem(
                plate = plate,
                isSeen = plate.code in seenPlates,
                onPlateClicked = { onPlateClicked(plate) }
            )
        }
    }
}

@Composable
fun PlateListItem(
    plate: LicensePlate,
    isSeen: Boolean,
    onPlateClicked: () -> Unit
) {
    val headlineColor = if (isSeen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    val supportColor = if (isSeen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${plate.code}: ${plate.city}", color = headlineColor)
            Text(text = plate.state, color = supportColor)
        }
        IconButton(onClick = onPlateClicked) {
            Icon(
                imageVector = if (isSeen) Icons.Default.Check else Icons.Default.Add,
                contentDescription = "Toggle seen"
            )
        }
    }
}
