package com.example.supershift

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.features.docupro.ui.LogsView // Import the new LogsView!
import com.example.supershift.features.docupro.ui.StatementsList
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.supershift.utils.CsvImporter
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperShiftAppNavigation(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    var selectedItem by remember { mutableIntStateOf(0) }

    // Fixed: Reduced to the 3 core shift-level tabs.
    val items = listOf("Dashboard", "Tasks", "Settings")

    // Fixed: Aligned icons with the 3 core tabs.
    val icons = listOf(
        Icons.Default.Home,
        Icons.AutoMirrored.Filled.List,
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

                // Tab 1: Gamified Tasks / Deployment
                1 -> Text("Tasks Screen Placeholder", modifier = Modifier.padding(16.dp))

                // Tab 2: Settings & Secure Manager Gateway
                2 -> {
                    val context = LocalContext.current
                    var pinCode by remember { mutableStateOf("") }
                    var isUnlocked by remember { mutableStateOf(false) }
                    var activeManagerScreen by remember { mutableStateOf("Menu") }

                    // Create a generic picker for CSV files
                    var targetCategory by remember { mutableStateOf("") }
                    val csvPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                        onResult = { uri ->
                            uri?.let {
                                scope.launch {
                                    context.contentResolver.openInputStream(it)?.use { stream ->
                                        CsvImporter.importCsvStream(stream, targetCategory, dao)
                                    }
                                }
                            }
                        }
                    )

                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        if (!isUnlocked) {
                            Text("Manager Control Panel", style = MaterialTheme.typography.headlineSmall)

                            Spacer(modifier = Modifier.height(24.dp))

                            // --- DATA INJECTION SECTION ---
                            Text("Inventory Injection", style = MaterialTheme.typography.titleMedium)
                            Text("Upload new CSV files to update the floor lists.", style = MaterialTheme.typography.bodySmall)

                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { targetCategory = "RTE"; csvPickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }, modifier = Modifier.weight(1f)) {
                                    Text("RTE")
                                }
                                Button(onClick = { targetCategory = "Prep"; csvPickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }, modifier = Modifier.weight(1f)) {
                                    Text("Prep")
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { targetCategory = "Bread"; csvPickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }, modifier = Modifier.weight(1f)) {
                                    Text("Bread")
                                }
                                Button(onClick = { targetCategory = "Bakery"; csvPickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv")) }, modifier = Modifier.weight(1f)) {
                                    Text("Bakery")
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(24.dp))

                            Text("Manager Area", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = pinCode,
                                onValueChange = { pinCode = it },
                                label = { Text("Enter Manager PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                            Button(
                                onClick = {
                                    if (pinCode == "1234") {
                                        isUnlocked = true
                                        pinCode = ""
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Unlock Manager Features")
                            }
                        } else {
                            when (activeManagerScreen) {
                                "Menu" -> {
                                    Column {
                                        Text("Manager Settings", style = MaterialTheme.typography.headlineSmall)
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Button(
                                            onClick = { activeManagerScreen = "Logs" },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("View Black Box Logs")
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { activeManagerScreen = "Statements" },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Generate HR Statements")
                                        }
                                        Spacer(modifier = Modifier.height(32.dp))
                                        Button(
                                            onClick = { isUnlocked = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Lock & Return")
                                        }
                                    }
                                }
                                "Logs" -> {
                                    Column {
                                        TextButton(onClick = { activeManagerScreen = "Menu" }, modifier = Modifier.padding(8.dp)) {
                                            Text("<- Back to Manager Menu")
                                        }
                                        LogsView(dao = dao)
                                    }
                                }
                                "Statements" -> {
                                    Column {
                                        TextButton(onClick = { activeManagerScreen = "Menu" }, modifier = Modifier.padding(8.dp)) {
                                            Text("<- Back to Manager Menu")
                                        }
                                        StatementsList(dao = dao)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}