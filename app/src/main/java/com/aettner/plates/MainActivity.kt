package com.aettner.plates

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aettner.plates.ui.theme.PlatesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "LicensePlatePrefs"
private const val SEEN_PLATES_KEY_TIMESTAMP = "seen_plates_timestamps"
private const val SEEN_PLATES_KEY_OLD = "seen_plates"
private const val THEME_KEY = "theme_preference"

sealed class Screen(val route: String) {
    data object List : Screen("list")
    data object Map : Screen("map")
}

sealed class FilterState {
    data object All : FilterState()
    data object Seen : FilterState()
    data object Unseen : FilterState()
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val sharedPreferences = remember {
                context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            }
            var themePreference by remember {
                mutableStateOf(sharedPreferences.getString(THEME_KEY, "System") ?: "System")
            }

            LaunchedEffect(themePreference) {
                sharedPreferences.edit {
                    putString(THEME_KEY, themePreference)
                }
            }

            val useDarkTheme = when (themePreference) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            PlatesTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                var licensePlates by remember { mutableStateOf<List<LicensePlate>>(emptyList()) }

                var seenPlates by remember {
                    val savedJson = sharedPreferences.getString(SEEN_PLATES_KEY_TIMESTAMP, null)
                    val map = if (savedJson != null) {
                        Json.decodeFromString<Map<String, Long>>(savedJson)
                    } else {
                        val oldSet = sharedPreferences.getStringSet(SEEN_PLATES_KEY_OLD, null) ?: emptySet()
                        oldSet.associateWith { System.currentTimeMillis() }
                    }
                    mutableStateOf(map)
                }

                LaunchedEffect(seenPlates) {
                    sharedPreferences.edit {
                        val json = Json.encodeToString(seenPlates)
                        putString(SEEN_PLATES_KEY_TIMESTAMP, json)
                        remove(SEEN_PLATES_KEY_OLD)
                    }
                }

                val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                    uri?.let {
                        try {
                            val json = Json.encodeToString(seenPlates)
                            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                                outputStream.write(json.toByteArray())
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val getContentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    uri?.let {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val jsonString = inputStream.bufferedReader().use { it.readText() }
                                val importedPlates = Json.decodeFromString<Map<String, Long>>(jsonString)
                                seenPlates = importedPlates
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        try {
                            val jsonString =
                                context.assets.open("german_license_plates.json").bufferedReader().use { it.readText() }
                            licensePlates = Json.decodeFromString<List<LicensePlate>>(jsonString)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val states = remember(licensePlates) {
                    listOf("All states") + licensePlates.map { it.state }.distinct().sorted()
                }

                val completedStates = remember(licensePlates, seenPlates) {
                    licensePlates.groupBy { it.state }.filter {
                        val allPlatesInState = it.value.map { plate -> plate.code }.toSet()
                        seenPlates.keys.containsAll(allPlatesInState)
                    }.keys
                }

                var showMenu by remember { mutableStateOf(false) }
                var showSettingsMenu by remember { mutableStateOf(false) }
                var filterState by remember { mutableStateOf<FilterState>(FilterState.Unseen) }
                var showStateMenu by remember { mutableStateOf(false) }
                var selectedState by remember { mutableStateOf("All states") }

                var searchQuery by remember { mutableStateOf("") }
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                val filteredPlates = remember(licensePlates, seenPlates, filterState, selectedState) {
                    val stateFilteredPlates = if (selectedState == "All states") {
                        licensePlates
                    } else {
                        licensePlates.filter { it.state == selectedState }
                    }

                    when (filterState) {
                        FilterState.All -> stateFilteredPlates
                        FilterState.Seen -> stateFilteredPlates.filter { it.code in seenPlates }
                        FilterState.Unseen -> stateFilteredPlates.filter { it.code !in seenPlates }
                    }
                }

                LaunchedEffect(filteredPlates) {
                    searchQuery = ""
                    if (filteredPlates.isNotEmpty()) {
                        listState.scrollToItem(0)
                    }
                }

                val searchResults = remember(searchQuery, filteredPlates) {
                    if (searchQuery.isBlank()) {
                        emptyList()
                    } else {
                        filteredPlates.filter { it.code.startsWith(searchQuery, ignoreCase = true) }.take(5)
                    }
                }

                val platesForState = remember(licensePlates, selectedState) {
                    if (selectedState == "All states") {
                        licensePlates
                    } else {
                        licensePlates.filter { it.state == selectedState }
                    }
                }

                val seenPlatesInStateCount = remember(platesForState, seenPlates) {
                    platesForState.count { it.code in seenPlates }
                }

                val totalPlatesInStateCount = platesForState.size

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(if (totalPlatesInStateCount > 0) "Plates: $seenPlatesInStateCount / $totalPlatesInStateCount" else "Plates") },
                            actions = {
                                Box {
                                    TextButton(onClick = { showStateMenu = true }) {
                                        Text(selectedState)
                                    }
                                    DropdownMenu(
                                        expanded = showStateMenu,
                                        onDismissRequest = { showStateMenu = false }
                                    ) {
                                        states.forEach { state ->
                                            DropdownMenuItem(text = {
                                                Row {
                                                    Text(state)
                                                    if (state in completedStates) {
                                                        Icon(
                                                            imageVector = Icons.Default.EmojiEvents,
                                                            contentDescription = "Trophy",
                                                            tint = Color.Yellow,
                                                            modifier = Modifier.padding(start = 4.dp)
                                                        )
                                                    }
                                                }
                                            }, onClick = {
                                                selectedState = state
                                                showStateMenu = false
                                            })
                                        }
                                    }
                                }

                                IconButton(onClick = {
                                    filterState = if (filterState == FilterState.Unseen) {
                                        FilterState.All
                                    } else {
                                        FilterState.Unseen
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (filterState == FilterState.Unseen) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Filter"
                                    )
                                }

                                Box {
                                    IconButton(onClick = { showMenu = !showMenu }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Export") },
                                            onClick = {
                                                createDocumentLauncher.launch("plates_export.json")
                                                showMenu = false
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Import") },
                                            onClick = {
                                                getContentLauncher.launch("application/json")
                                                showMenu = false
                                            })
                                        DropdownMenuItem(
                                            text = { Text("Settings") },
                                            onClick = { showSettingsMenu = true; showMenu = false })
                                    }
                                    DropdownMenu(
                                        expanded = showSettingsMenu,
                                        onDismissRequest = { showSettingsMenu = false })
                                    {
                                        DropdownMenuItem(
                                            text = { Text("System Theme") },
                                            onClick = { themePreference = "System"; showSettingsMenu = false })
                                        DropdownMenuItem(
                                            text = { Text("Light Theme") },
                                            onClick = { themePreference = "Light"; showSettingsMenu = false })
                                        DropdownMenuItem(
                                            text = { Text("Dark Theme") },
                                            onClick = { themePreference = "Dark"; showSettingsMenu = false })
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.List, contentDescription = "List") },
                                label = { Text("List") },
                                selected = navController.currentDestination?.route == Screen.List.route,
                                onClick = { navController.navigate(Screen.List.route) }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                                label = { Text("Map") },
                                selected = navController.currentDestination?.route == Screen.Map.route,
                                onClick = { navController.navigate(Screen.Map.route) }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.List.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.List.route) {
                            Column {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search for plate code") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )

                                if (searchQuery.isNotBlank()) {
                                    if (searchResults.isNotEmpty()) {
                                        Card(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Column {
                                                searchResults.forEach { plate ->
                                                    val isSeen = plate.code in seenPlates
                                                    Text(
                                                        text = "${plate.code}: ${plate.city} (${plate.state})",
                                                        color = if (isSeen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                coroutineScope.launch {
                                                                    val index = filteredPlates.indexOf(plate)
                                                                    if (index != -1) {
                                                                        listState.scrollToItem(index)
                                                                    }
                                                                }
                                                                searchQuery = ""
                                                            }
                                                            .padding(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Text("No results found", modifier = Modifier.padding(16.dp))
                                    }
                                }

                                LicensePlateList(
                                    licensePlates = filteredPlates,
                                    seenPlates = seenPlates,
                                    onPlateClick = { plate ->
                                        seenPlates = if (plate.code in seenPlates) {
                                            Log.d("LicensePlateClick", "Marked as unseen: ${plate.code}")
                                            seenPlates - plate.code
                                        } else {
                                            Log.d("LicensePlateClick", "Marked as seen: ${plate.code}")
                                            seenPlates + (plate.code to System.currentTimeMillis())
                                        }
                                    },
                                    listState = listState
                                )
                            }
                        }
                        composable(Screen.Map.route) {
                            MapScreen(licensePlates, seenPlates.keys, useDarkTheme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LicensePlateList(
    licensePlates: List<LicensePlate>,
    seenPlates: Map<String, Long>,
    onPlateClick: (LicensePlate) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(modifier = modifier, state = listState) {
        items(items = licensePlates, key = { it.code }) { plate ->
            val seenTimestamp = seenPlates[plate.code]
            val isSeen = seenTimestamp != null

            val secondaryText = if (seenTimestamp != null) {
                val date = remember(seenTimestamp) { Date(seenTimestamp) }
                val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                "${plate.state} - ${dateFormat.format(date)}"
            } else {
                plate.state
            }
            ListItem(
                headlineContent = { Text("${plate.code}: ${plate.city}") },
                supportingContent = {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                colors = ListItemDefaults.colors(
                    headlineColor = if (isSeen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    supportingColor = if (isSeen) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(plate) {
                        detectTapGestures(
                            onDoubleTap = {
                                onPlateClick(plate)
                            }
                        )
                    }
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
            seenPlates = mapOf("KA" to System.currentTimeMillis()),
            onPlateClick = {}
        )
    }
}
