
package com.aettner.plates

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlateListScreen(
    licensePlates: List<LicensePlate>,
    seenPlates: Map<String, Long>,
    onPlateClicked: (LicensePlate) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(modifier = modifier, state = listState) {
        items(items = licensePlates, key = { it.code }) { plate ->
            PlateListItem(
                plate = plate,
                seenTimestamp = seenPlates[plate.code],
                onPlateClicked = { onPlateClicked(plate) }
            )
        }
    }
}

@Composable
fun PlateListItem(
    plate: LicensePlate,
    seenTimestamp: Long?,
    onPlateClicked: () -> Unit
) {
    val animatedProgress = remember { Animatable(0f) }
    var showProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val isSeen = seenTimestamp != null

    LaunchedEffect(showProgress) {
        if (!showProgress) {
            animatedProgress.snapTo(0f)
        }
    }

    val headlineColor = if (isSeen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    val supportColor = if (isSeen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (showProgress) {
                    drawRect(
                        color = progressColor,
                        size = size.copy(width = size.width * animatedProgress.value),
                        alpha = 0.2f
                    )
                }
            }
            .then(
                if (isSeen) {
                    Modifier.pointerInput(plate) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            showProgress = true
                            val job = coroutineScope.launch {
                                animatedProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
                                )
                            }

                            val up = withTimeoutOrNull(2000) {
                                var event: PointerEvent
                                do {
                                    event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                                event.changes.firstOrNull()
                            }

                            if (up == null) {
                                // Timeout happened, successfully held for 2 seconds.
                                onPlateClicked()
                            }

                            job.cancel()
                            showProgress = false
                        }
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${plate.code}: ${plate.city}", color = headlineColor)
            val secondaryText = if (seenTimestamp != null) {
                val date = remember(seenTimestamp) { Date(seenTimestamp) }
                val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                "${plate.state} - ${dateFormat.format(date)}"
            } else {
                plate.state
            }
            Text(
                text = secondaryText,
                color = supportColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(enabled = !isSeen) { onPlateClicked() },
            contentAlignment = Alignment.Center
        ) {
            if (!isSeen) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Toggle seen"
                )
            }
        }
    }
}
