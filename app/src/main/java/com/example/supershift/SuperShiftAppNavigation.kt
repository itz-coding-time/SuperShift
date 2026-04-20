package com.example.supershift

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.features.docupro.ui.* // Imports all Manager Views
import com.example.supershift.features.hud.ui.ActiveHudScreen
import com.example.supershift.features.hud.ui.DashboardScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperShiftAppNavigation(dao: SuperShiftDao) {
    var selectedItem by remember { mutableIntStateOf(0) }

    val items = listOf("Dashboard", "Tasks", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.AutoMirrored.Filled.List, Icons.Default.Settings)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SuperShift Engine") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer, titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(icon = { Icon(icons[index], contentDescription = item) }, label = { Text(item) }, selected = selectedItem == index, onClick = { selectedItem = index })
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedItem) {
                0 -> DashboardScreen(dao = dao)
                1 -> ActiveHudScreen(dao = dao)
                2 -> {
                    var pinCode by remember { mutableStateOf("") }
                    var isUnlocked by remember { mutableStateOf(false) }
                    var activeManagerScreen by remember { mutableStateOf("Menu") }

                    if (!isUnlocked) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("General Settings", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(32.dp))
                            Text("Manager Area", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = pinCode, onValueChange = { pinCode = it }, label = { Text("Enter Manager PIN") },
                                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                            Button(onClick = { if (pinCode == "0318") { isUnlocked = true; pinCode = "" } }, modifier = Modifier.padding(top = 16.dp)) { Text("Unlock Manager Features") }
                        }
                    } else {
                        when (activeManagerScreen) {
                            "Menu" -> ManagerSettingsView(
                                dao = dao,
                                onNavigate = { route -> activeManagerScreen = route },
                                onLock = { isUnlocked = false; activeManagerScreen = "Menu" }
                            )

                            // --- SUB-ROUTING ---
                            "TableEditor" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; TableEditorView(dao = dao) } }
                            "Inventory" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; InventoryEditorView(dao = dao) } }
                            "Roster" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; RosterView(dao = dao) } }
                            "Schedule" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; ShiftScheduleView(dao = dao) } }
                            "Logs" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; LogsView(dao = dao) } }
                            "Statements" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; StatementsList(dao = dao) } }
                        }
                    }
                }
            }
        }
    }
}