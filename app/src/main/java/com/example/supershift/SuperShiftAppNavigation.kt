package com.example.supershift

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperShiftAppNavigation(dao: SuperShiftDao) {
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
                    var pinCode by remember { mutableStateOf("") }
                    var isUnlocked by remember { mutableStateOf(false) }
                    var activeManagerScreen by remember { mutableStateOf("Menu") }

                    if (!isUnlocked) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("General Settings", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(32.dp))

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
                                    // Hardcoded for testing; we will link this to the Associate DB PIN later
                                    if (pinCode == "1234") {
                                        isUnlocked = true
                                        pinCode = "" // Clear for security
                                    }
                                },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Unlock Manager Features")
                            }
                        }
                    } else {
                        when (activeManagerScreen) {
                            "Menu" -> {
                                Column(modifier = Modifier.padding(16.dp)) {
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