package com.example.supershift.features.docupro.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.utils.CsvImporter
import kotlinx.coroutines.launch

@Composable
fun ManagerSettingsView(
    dao: SuperShiftDao,
    onNavigate: (String) -> Unit,
    onLock: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetCategory by remember { mutableStateOf("") }

    val csvPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    when {
                        targetCategory == "Checklist" -> CsvImporter.importTaskChecklist(stream, dao)
                        targetCategory in listOf("Starter", "Finisher A", "Finisher B") -> CsvImporter.importTableFlipStream(stream, targetCategory, dao)
                        else -> CsvImporter.importCsvStream(stream, targetCategory, dao)
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Manager Control Panel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // --- 1. SHIFT TIMING ---
        ShiftManagerView(dao = dao)
        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. TABLE FLIPS ---
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Table Flip Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Manage Midnight Flip items via editor or CSV.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(onClick = { onNavigate("TableEditor") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Open Table Editor")
                }

                Text("CSV Injection:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { targetCategory = "Starter"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Starter") }
                    Button(onClick = { targetCategory = "Finisher A"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Fin A") }
                    Button(onClick = { targetCategory = "Finisher B"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Fin B") }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. INVENTORY PULLS ---
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Inventory Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Manage Pull Lists via editor or CSV.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(onClick = { onNavigate("Inventory") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Open Pull List Editor")
                }

                Text("CSV Injection:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 12.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { targetCategory = "RTE"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("RTE") }
                    Button(onClick = { targetCategory = "Prep"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Prep") }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { targetCategory = "Bread"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Bread") }
                    Button(onClick = { targetCategory = "Bakery"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Bakery") }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- GENERAL TASKS (Checklist) ---
        Button(onClick = { targetCategory = "Checklist"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
            Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Upload General Task Checklist")
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // --- 4. ASSOCIATES ---
        Text("Team Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("Roster") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Person, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Manage Associate Roster")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("Schedule") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AccountBox, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Manage Shift Schedule")
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- 5. LOGS & HR ---
        Text("Accountability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("Logs") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
            Icon(Icons.Default.Warning, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("View Black Box Logs")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("Statements") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
            Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Generate HR Statements")
        }
        Spacer(modifier = Modifier.height(32.dp))

        // --- 6. LOCK ---
        Button(onClick = onLock, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
            Text("Lock & Return")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}