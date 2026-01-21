package com.aettner.plates

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object MapUtils {

    private val stateToSvgId = mapOf(
        "Baden-Württemberg" to "DE-BW",
        "Bayern" to "DE-BY",
        "Berlin" to "DE-BE",
        "Brandenburg" to "DE-BB",
        "Freie Hansestadt Bremen" to "DE-HB",
        "Freie und Hansestadt Hamburg" to "DE-HH",
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

    fun getColoredSvg(svgContent: String, licensePlates: List<LicensePlate>, seenPlates: Set<String>): String {
        // Reconstruct the style block to ensure validity.
        val styleBuilder = StringBuilder()
        styleBuilder.append(
            """<style>
        .state {
            fill: white;
            stroke: green;
            stroke-width: 1px;
            cursor: pointer;
            transition: fill 0.2s;
        }
        .state:hover {
            fill: #c0392b;
        }"""
        )

        val platesByState = licensePlates.groupBy { it.state }

        // Add specific override rules only for completed states.
        for ((state, platesInState) in platesByState) {
            val allPlatesInState = platesInState.map { it.code }.toSet()
            if (seenPlates.containsAll(allPlatesInState)) {
                val hexColor = String.format("#%06X", 0xFFFFFF and Color(0xFFFFD700).toArgb()) // Gold
                val svgId = stateToSvgId[state]
                if (svgId != null) {
                    styleBuilder.append("\n        #$svgId { fill: $hexColor; }")
                }
            }
        }

        styleBuilder.append("\n    </style>")

        // Replace the old style block with the new, valid one.
        val startTag = "<style>"
        val endTag = "</style>"
        val startIndex = svgContent.indexOf(startTag)
        val endIndex = svgContent.indexOf(endTag)

        if (startIndex == -1 || endIndex == -1) {
            return svgContent // Fallback
        }

        return svgContent.substring(0, startIndex) +
               styleBuilder.toString() +
               svgContent.substring(endIndex + endTag.length)
    }
}
