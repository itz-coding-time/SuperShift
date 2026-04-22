package com.example.supershift

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.features.docupro.ui.* import com.example.supershift.features.hud.ui.ActiveHudScreen
import com.example.supershift.features.hud.ui.OverviewScreen
import com.example.supershift.features.hud.ui.DelegationScreen
import com.example.supershift.features.onboarding.ui.OnboardingScreen
import com.example.supershift.ui.theme.SuperShiftTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperShiftAppNavigation(dao: SuperShiftDao) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("supershift_prefs", Context.MODE_PRIVATE)

    var isOnboarded by remember { mutableStateOf(prefs.getBoolean("is_onboarded", false)) }
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("dark_mode", true)) }
    var isDebugMode by remember { mutableStateOf(prefs.getBoolean("debug_mode", false)) }
    var selectedItem by remember { mutableIntStateOf(0) }

    val items = listOf("Overview", "Delegation", "Tasks", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.SwapHoriz, Icons.AutoMirrored.Filled.List, Icons.Default.Settings)

    SuperShiftTheme(darkTheme = isDarkMode) {
        if (!isOnboarded) {
            OnboardingScreen(dao = dao) {
                prefs.edit().putBoolean("is_onboarded", true).apply()
                isOnboarded = true
            }
        } else {
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
                        0 -> OverviewScreen(dao = dao)
                        1 -> DelegationScreen(dao = dao, isDebugMode = isDebugMode)
                        2 -> ActiveHudScreen(dao = dao, isDebugMode = isDebugMode)
                        3 -> {
                            var pinCode by remember { mutableStateOf("") }
                            var isUnlocked by remember { mutableStateOf(false) }
                            var activeManagerScreen by remember { mutableStateOf("Menu") }

                            val customManagerPin = prefs.getString("manager_pin", "1234") ?: "1234"

                            BackHandler(enabled = isUnlocked && activeManagerScreen != "Menu") {
                                activeManagerScreen = "Menu"
                            }

                            if (!isUnlocked) {
                                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                                    Text("General Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Dark Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("Toggle high-contrast UI", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it; prefs.edit().putBoolean("dark_mode", it).apply() })
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(32.dp))

                                    Text("Manager Gateway", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Enter custom MOD PIN (or 1234) to unlock tools.", style = MaterialTheme.typography.bodySmall)

                                    OutlinedTextField(
                                        value = pinCode, onValueChange = { pinCode = it }, label = { Text("Enter Manager PIN") },
                                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                    )
                                    Button(
                                        onClick = { if (pinCode == customManagerPin || pinCode == "1234") { isUnlocked = true; pinCode = "" } },
                                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                    ) { Text("Unlock Gateway") }
                                }
                            } else {
                                when (activeManagerScreen) {
                                    "Menu" -> ManagerSettingsView(
                                        dao = dao,
                                        isDebugMode = isDebugMode,
                                        onDebugToggle = { isDebugMode = it; prefs.edit().putBoolean("debug_mode", it).apply() },
                                        onNavigate = { route -> activeManagerScreen = route },
                                        onLock = { isUnlocked = false; activeManagerScreen = "Menu" }
                                    )
                                    "TableEditor" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back to Menu") }; TableEditorView(dao = dao) } }
                                    "Inventory" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back to Menu") }; InventoryEditorView(dao = dao) } }
                                    "Roster" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back to Menu") }; RosterView(dao = dao) } }
                                    "Schedule" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back to Menu") }; ShiftScheduleView(dao = dao) } }
                                    "Logs" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back to Menu") }; LogsView(dao = dao) } }
                                    "Statements" -> { Column { TextButton(onClick = { activeManagerScreen = "Menu" }) { Text("<- Back to Menu") }; StatementsList(dao = dao) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}