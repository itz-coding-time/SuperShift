package com.example.supershift

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.features.docupro.ui.InventoryEditorView
import com.example.supershift.features.docupro.ui.LogsView
import com.example.supershift.features.docupro.ui.RosterView
import com.example.supershift.features.docupro.ui.ShiftManagerView
import com.example.supershift.features.docupro.ui.ShiftScheduleView
import com.example.supershift.features.docupro.ui.StatementsList
import com.example.supershift.features.hud.ui.ActiveHudScreen
import com.example.supershift.features.hud.ui.DashboardScreen
import com.example.supershift.utils.CsvImporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperShiftAppNavigation(dao: SuperShiftDao) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

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
                            Button(onClick = { if (pinCode == "1234") { isUnlocked = true; pinCode = "" } }, modifier = Modifier.padding(top = 16.dp)) { Text("Unlock Manager Features") }
                        }
                    } else {
                        when (activeManagerScreen) {
                            "Menu" -> {
                                val context = LocalContext.current
                                var targetCategory by remember { mutableStateOf("") }
                                val csvPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
                                    uri?.let { scope.launch { context.contentResolver.openInputStream(it)?.use { stream -> if (targetCategory == "Checklist") CsvImporter.importTaskChecklist(stream, dao) else CsvImporter.importCsvStream(stream, targetCategory, dao) } } }
                                }

                                Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                                    Text("Manager Control Panel", style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    ShiftManagerView(dao = dao)
                                    Spacer(modifier = Modifier.height(24.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text("Dynamic Inventory Editor", style = MaterialTheme.typography.titleMedium)
                                    Button(onClick = { activeManagerScreen = "Inventory" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                                        Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Open Pull List Editor")
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text("Inventory & Task CSV Injection", style = MaterialTheme.typography.titleMedium)
                                    Button(onClick = { targetCategory = "Checklist"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                        Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Upload Task Checklist")
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { targetCategory = "RTE"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("RTE") }
                                        Button(onClick = { targetCategory = "Prep"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Prep") }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { targetCategory = "Bread"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Bread") }
                                        Button(onClick = { targetCategory = "Bakery"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Bakery") }
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(onClick = { activeManagerScreen = "Roster" }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Person, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Manage Associate Roster")
                                    }

                                    // --- NEW: SCHEDULE BUTTON ---
                                    Button(onClick = { activeManagerScreen = "Schedule" }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                        Icon(Icons.Default.AccountBox, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Manage Shift Schedule")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { activeManagerScreen = "Logs" }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Warning, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("View Black Box Logs")
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { activeManagerScreen = "Statements" }, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Generate HR Statements")
                                    }
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Button(onClick = { isUnlocked = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Lock & Return") }
                                }
                            }
                            "Inventory" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; InventoryEditorView(dao = dao) } }
                            "Roster" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; RosterView(dao = dao) } }
                            "Schedule" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; ShiftScheduleView(dao = dao) } } // NEW ROUTE
                            "Logs" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; LogsView(dao = dao) } }
                            "Statements" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back") }; StatementsList(dao = dao) } }
                        }
                    }
                }
            }
        }
    }
}
