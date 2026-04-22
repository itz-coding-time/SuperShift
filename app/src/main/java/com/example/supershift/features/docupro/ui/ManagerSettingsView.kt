package com.example.supershift.features.docupro.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.utils.CsvExporter
import com.example.supershift.utils.CsvImporter
import kotlinx.coroutines.launch

@Composable
fun ManagerSettingsView(
    dao: SuperShiftDao,
    isDebugMode: Boolean, // Added Debug State
    onDebugToggle: (Boolean) -> Unit, // Added Debug Callback
    onNavigate: (String) -> Unit,
    onLock: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetCategory by remember { mutableStateOf("") }

    val allTasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val hasAssociates = associates.isNotEmpty()

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

    val exportChecklistLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    CsvExporter.exportTaskChecklist(stream, allTasks)
                    Toast.makeText(context, "Checklist backed up successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Manager Control Panel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 1: SHIFT TIMING ---
        Text("SHIFT CONTEXT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        ShiftManagerView(dao = dao)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 2: TABLE FLIPS ---
        Text("FOOD SAFETY: MIDNIGHT FLIPS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = { onNavigate("TableEditor") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Dynamic Table Editor")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("CSV Data Sync:", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { targetCategory = "Starter"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Starter") }
                    Button(onClick = { targetCategory = "Finisher A"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Fin A") }
                    Button(onClick = { targetCategory = "Finisher B"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f)) { Text("Fin B") }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 3: INVENTORY ---
        Text("INVENTORY: PULL LISTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = { onNavigate("Inventory") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Dynamic Inventory Editor")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("CSV Data Sync:", style = MaterialTheme.typography.labelSmall)
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

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 3.5: GENERAL TASKS ---
        Text("FLOOR LOGISTICS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = { targetCategory = "Checklist"; csvPickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Upload Z1 Checklist CSV")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { exportChecklistLauncher.launch("Z1_Checklist_Backup.csv") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Backup Checklist to CSV")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 4: ASSOCIATES ---
        Text("PERSONNEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("Roster") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Person, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Associate Roster")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { onNavigate("Schedule") }, modifier = Modifier.fillMaxWidth(), enabled = hasAssociates) {
            Icon(Icons.Default.Schedule, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Associate Schedule")
        }
        if (!hasAssociates) {
            Text("Add an associate to the roster before scheduling.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 5: ACCOUNTABILITY ---
        Text("ACCOUNTABILITY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("Logs") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
            Icon(Icons.Default.Warning, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Black Box Logs")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNavigate("Statements") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
            Icon(Icons.Default.Edit, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("HR Statements")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION 6: SYSTEM & DEBUG (NEW) ---
        Text("SYSTEM & DEBUG", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Developer Debug Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Lifts shift locks to test task execution.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = isDebugMode, onCheckedChange = onDebugToggle)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- SECTION 7: LOCK ---
        Button(onClick = onLock, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Lock Manager Access")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}