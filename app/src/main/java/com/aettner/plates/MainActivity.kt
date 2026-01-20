package com.aettner.plates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.aettner.plates.ui.theme.PlatesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlatesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var licensePlates by remember { mutableStateOf<List<LicensePlate>>(emptyList()) }
                    val context = LocalContext.current

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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun LicensePlateList(licensePlates: List<LicensePlate>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(licensePlates) { plate ->
            Text(text = "${plate.code}: ${plate.city} (${plate.state})")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PlatesTheme {
        LicensePlateList(licensePlates = listOf(LicensePlate("KA", "Karlsruhe", "Baden-Württemberg")))
    }
}