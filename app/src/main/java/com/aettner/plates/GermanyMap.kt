package com.aettner.plates

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.min

@Composable
fun GermanyMap(licensePlates: List<LicensePlate>, seenPlates: Set<String>, useDarkTheme: Boolean) {
    val states = mapOf(
        "DE-SH" to "M420 70 L500 90 L560 140 L570 190 L520 210 L470 200 L430 220 L380 190 L360 140 Z",
        "DE-MV" to "M560 140 L660 160 L740 210 L760 270 L720 300 L660 290 L620 310 L570 270 Z",
        "DE-HH" to "M460 210 L485 210 L490 235 L465 240 Z",
        "DE-HB" to "M420 260 L440 260 L445 285 L425 290 Z",
        "DE-NI" to "M320 200 L380 190 L430 220 L460 260 L470 320 L440 360 L390 380 L330 360 L290 320 L300 250 Z",
        "DE-BB" to "M600 310 L680 300 L770 360 L800 440 L760 520 L690 540 L630 500 L590 420 Z",
        "DE-BE" to "M660 400 L690 400 L695 430 L665 435 Z",
        "DE-ST" to "M470 260 L560 270 L590 310 L580 360 L520 390 L470 360 Z",
        "DE-NW" to "M220 330 L300 320 L330 360 L330 440 L290 480 L230 460 L200 400 Z",
        "DE-HE" to "M330 380 L410 370 L450 420 L440 480 L390 520 L340 490 Z",
        "DE-TH" to "M450 390 L520 390 L550 430 L540 480 L480 510 L440 470 Z",
        "DE-SN" to "M550 480 L620 470 L700 510 L710 570 L660 610 L580 600 Z",
        "DE-RP" to "M240 460 L300 460 L330 500 L320 570 L280 610 L240 580 Z",
        "DE-SL" to "M240 600 L270 600 L280 630 L250 640 Z",
        "DE-BW" to "M330 520 L420 520 L470 580 L460 670 L400 720 L340 680 Z",
        "DE-BY" to "M470 520 L620 520 L760 600 L820 720 L800 850 L700 930 L560 900 L480 820 Z"
    )

    val pathParser = PathParser()
    val paths = remember(states) {
        states.mapValues { (_, pathData) ->
            pathParser.parsePathString(pathData).toPath()
        }
    }
    
    val platesByState = remember(licensePlates) {
        licensePlates.groupBy { it.state }
    }

    val borderColor = if (useDarkTheme) Color.White else Color.Black

    Canvas(modifier = Modifier.fillMaxSize()) { 
        val canvasWidth = size.width
        val canvasHeight = size.height
        val sourceWidth = 800f
        val sourceHeight = 1000f

        val scaleX = canvasWidth / sourceWidth
        val scaleY = canvasHeight / sourceHeight
        val scale = min(scaleX, scaleY)

        val translateX = (canvasWidth - sourceWidth * scale) / 2f
        val translateY = (canvasHeight - sourceHeight * scale) / 2f

        withTransform({
            translate(left = translateX, top = translateY)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            paths.forEach { (id, path) ->
                val stateName = StateMapping.stateToSvgId.entries.find { it.value == id }?.key
                
                val platesInState = platesByState[stateName] ?: emptyList()
                val seenPlatesInState = platesInState.count { it.code in seenPlates }
                val totalPlatesInState = platesInState.size
                val percentage = if (totalPlatesInState > 0) {
                    seenPlatesInState.toFloat() / totalPlatesInState.toFloat()
                } else {
                    0f
                }

                val color = when {
                    percentage == 1f -> Color(0xFFFFD700) // Gold
                    percentage > 0f -> {
                        val grayScale = (1 - percentage) * 255
                        Color(grayScale.toInt(), grayScale.toInt(), grayScale.toInt())
                    }
                    else -> Color.White
                }

                drawPath(
                    path = path,
                    color = color,
                )
                drawPath(
                    path = path,
                    color = borderColor,
                    style = Stroke(width = 1f / scale) // Adjust stroke width for scaling
                )
            }
        }
    }
}
