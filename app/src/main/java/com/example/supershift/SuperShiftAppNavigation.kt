package com.example.supershift

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperShiftAppNavigation(dao: SuperShiftDao) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Dashboard", "Tasks", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.AutoMirrored.Filled.List, Icons.Default.Settings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SuperShift") }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> Text("Dashboard Screen Placeholder", modifier = Modifier.padding(16.dp))
                1 -> Text("Tasks Screen Placeholder", modifier = Modifier.padding(16.dp))
                2 -> Text("Settings Screen Placeholder", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
