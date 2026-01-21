package com.aettner.plates

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest

private val stateToIdMap = mapOf(
    "Baden-Württemberg" to "DE-BW",
    "Bayern" to "DE-BY",
    "Berlin" to "DE-BE",
    "Brandenburg" to "DE-BB",
    "Bremen" to "DE-HB",
    "Hamburg" to "DE-HH",
    "Hessen" to "DE-HE",
    "Mecklenburg-Vorpommern" to "DE-MV",
    "Niedersachsen" to "DE-NI",
    "Nordrhein-Westfalen" to "DE-NW",
    "Rheinland-Pfalz" to "DE-RP",
    "Saarland" to "DE-SL",
    "Sachsen" to "DE-SN",
    "Sachsen-Anhalt" to "DE-ST",
    "Schleswig-Holstein" to "DE-SH",
    "Thüringen" to "DE-TH"
)

@Composable
fun MapScreen(
    licensePlates: List<LicensePlate>,
    seenPlates: Set<String>,
    onPlateLongClick: (LicensePlate) -> Unit = {}
) {
    val context = LocalContext.current

    val completedStates = remember(licensePlates, seenPlates) {
        licensePlates.groupBy { it.state }.filter { (_, platesInState) ->
            val allPlatesInState = platesInState.map { it.code }.toSet()
            seenPlates.containsAll(allPlatesInState)
        }.keys
    }

    val svgContent = remember(completedStates) {
        var content = context.assets.open("map_of_germany.svg").bufferedReader().use { it.readText() }
        completedStates.forEach { stateName ->
            val stateId = stateToIdMap[stateName]
            if (stateId != null) {
                content = content.replace(
                    "id=\"$stateId\" class=\"state\"",
                    "id=\"$stateId\" class=\"state seen\""
                )
            }
        }
        content
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Zoomable {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(data = svgContent)
                        .decoderFactory(SvgDecoder.Factory())
                        .build()
                ),
                contentDescription = "Map of Germany",
            )
        }
    }
}