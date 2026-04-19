package com.example.supershift

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.features.docupro.ui.LogsView // Import the new LogsView!
import com.example.supershift.features.docupro.ui.StatementsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperShiftAppNavigation(dao: SuperShiftDao) {
    var selectedItem by remember { mutableIntStateOf(0) }

    // Fixed: Added "Statements" to the items list so there are exactly 5 tabs
    val items = listOf("Dashboard", "Logs", "Tasks", "Statements", "Settings")

    // Fixed: Added the Edit icon to match the Statements tab
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.Warning,
        Icons.AutoMirrored.Filled.List,
        Icons.Default.Edit,
        Icons.Default.Settings
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SuperShift Engine") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                // Tab 0: Dashboard (DocuPro Home / SuperReEnforce Active HUD)
                0 -> Text("Dashboard Screen Placeholder", modifier = Modifier.padding(16.dp))

                // Tab 1: The Black Box Logs
                1 -> LogsView(dao = dao)

                // Tab 2: Gamified Tasks / Deployment
                2 -> Text("Tasks Screen Placeholder", modifier = Modifier.padding(16.dp))

                // Tab 3: Statements
                3 -> StatementsList(dao = dao)

                // Tab 4: Settings
                4 -> Text("Settings Screen Placeholder", modifier = Modifier.padding(16.dp))
            }
        }
    }
}