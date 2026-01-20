package com.aettner.plates

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.aettner.plates.ui.theme.PlatesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "LicensePlatePrefs"
private const val SEEN_PLATES_KEY = "seen_plates"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlatesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var licensePlates by remember { mutableStateOf<List<LicensePlate>>(emptyList()) }
                    val context = LocalContext.current

                    val sharedPreferences = remember {
                        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    }

                    var seenPlates by remember {
                        mutableStateOf(sharedPreferences.getStringSet(SEEN_PLATES_KEY, emptySet()) ?: emptySet())
                    }

                    LaunchedEffect(seenPlates) {
                        sharedPreferences.edit {
                            putStringSet(SEEN_PLATES_KEY, seenPlates)
                        }
                    }

                    LaunchedEffect(Unit) {
                        withContext(Dispatchers.IO) {
                            try {
                                val jsonString = context.assets.open("german_license_plates.json").bufferedReader().use { it.readText() }
                                licensePlates = Json.decodeFromString<List<LicensePlate>>(jsonString)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    LicensePlateList(
                        licensePlates = licensePlates,
                        seenPlates = seenPlates,
                        onPlateClick = { plate ->
                            seenPlates = seenPlates + plate.code
                            Log.d("LicensePlateClick", "Clicked and marked as seen: ${plate.code}")
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun LicensePlateList(
    licensePlates: List<LicensePlate>,
    seenPlates: Set<String>,
    onPlateClick: (LicensePlate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(items = licensePlates, key = { it.code }) { plate ->
            val isSeen = plate.code in seenPlates
            Text(
                text = "${plate.code}: ${plate.city} (${plate.state})",
                color = if (isSeen) Color.Gray else Color.Unspecified,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(plate) {
                        detectTapGestures(
                            onDoubleTap = {
                                onPlateClick(plate)
                            }
                        )
                    }
                    .padding(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlatesTheme {
        LicensePlateList(
            licensePlates = listOf(LicensePlate("KA", "Karlsruhe", "Baden-Württemberg")),
            seenPlates = setOf("KA"),
            onPlateClick = {}
        )
    }
}
