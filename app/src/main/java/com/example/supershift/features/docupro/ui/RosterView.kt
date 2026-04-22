package com.example.supershift.features.docupro.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.Associate
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.utils.CsvExporter
import com.example.supershift.utils.CsvImporter
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterView(dao: SuperShiftDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())

    // NEW: State holds the associate currently being added OR edited
    var activeDialogAssoc by remember { mutableStateOf<Associate?>(null) }
    var isNewAssoc by remember { mutableStateOf(true) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    CsvExporter.exportRoster(stream, associates)
                    Toast.makeText(context, "Roster backed up successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    CsvImporter.importRoster(stream, dao)
                    Toast.makeText(context, "Roster Synced!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isNewAssoc = true
                    activeDialogAssoc = Associate(name = "", role = "Team Member", currentArchetype = "Float")
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Associate")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Master Associate Roster", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Set recurring shift templates here. The engine will automatically draft them on their scheduled days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Import CSV")
                }
                Button(onClick = { exportLauncher.launch("SuperShift_Roster.csv") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Export CSV")
                }
            }

            if (associates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Roster is empty.") }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(associates) { assoc ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(assoc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        if (assoc.currentArchetype == "MOD") {
                                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(start = 8.dp)) {
                                                Text("MOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Default Zone: ${assoc.currentArchetype}", style = MaterialTheme.typography.bodySmall)
                                    if (assoc.scheduledDays.isNotBlank()) {
                                        Text("Works: ${assoc.scheduledDays} (${assoc.defaultStartTime} - ${assoc.defaultEndTime})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row {
                                    // NEW: Edit Profile Button
                                    IconButton(onClick = {
                                        isNewAssoc = false
                                        activeDialogAssoc = assoc
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    if (assoc.currentArchetype != "MOD") {
                                        IconButton(onClick = { scope.launch { dao.deleteAssociate(assoc) } }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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

    // --- ADD/EDIT PROFILE DIALOG ---
    if (activeDialogAssoc != null) {
        val currentAssoc = activeDialogAssoc!!

        var name by remember(currentAssoc) { mutableStateOf(currentAssoc.name) }
        var archetype by remember(currentAssoc) { mutableStateOf(currentAssoc.currentArchetype) }
        var pinCode by remember(currentAssoc) { mutableStateOf(currentAssoc.pinCode ?: "") }

        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        var selectedDays by remember(currentAssoc) { mutableStateOf(currentAssoc.scheduledDays.split(",").filter { it.isNotBlank() }.toSet()) }

        var startTime by remember(currentAssoc) { mutableStateOf(currentAssoc.defaultStartTime) }
        var endTime by remember(currentAssoc) { mutableStateOf(currentAssoc.defaultEndTime) }

        val sParts = startTime.split(":")
        val eParts = endTime.split(":")

        var showStartTimePicker by remember { mutableStateOf(false) }
        var showEndTimePicker by remember { mutableStateOf(false) }
        val startPickerState = rememberTimePickerState(initialHour = sParts[0].toIntOrNull() ?: 22, initialMinute = sParts[1].toIntOrNull() ?: 0)
        val endPickerState = rememberTimePickerState(initialHour = eParts[0].toIntOrNull() ?: 6, initialMinute = eParts[1].toIntOrNull() ?: 30)

        AlertDialog(
            onDismissRequest = { activeDialogAssoc = null },
            title = { Text(if (isNewAssoc) "Add to Roster" else "Edit Profile") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (currentAssoc.currentArchetype != "MOD") {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Associate Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                        Text("Default Zone (Archetype)", style = MaterialTheme.typography.labelSmall)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("Kitchen", "POS", "Float").forEachIndexed { index, arch ->
                                SegmentedButton(selected = archetype == arch, onClick = { archetype = arch }, shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)) { Text(arch) }
                            }
                        }
                    } else {
                        Text("Editing MOD Profile for $name", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = pinCode, onValueChange = { if (it.length <= 8) pinCode = it }, label = { Text("Update PIN Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Auto-Schedule Template", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        daysOfWeek.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            Surface(
                                onClick = { selectedDays = if (isSelected) selectedDays - day else selectedDays + day },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.size(32.dp)
                            ) { Box(contentAlignment = Alignment.Center) { Text(day.first().toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) } }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = {}, readOnly = true, label = { Text("Start Time") }, trailingIcon = { IconButton(onClick = { showStartTimePicker = true }) { Icon(Icons.Default.Schedule, null) } }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endTime, onValueChange = {}, readOnly = true, label = { Text("End Time") }, trailingIcon = { IconButton(onClick = { showEndTimePicker = true }) { Icon(Icons.Default.Schedule, null) } }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val daysString = daysOfWeek.filter { selectedDays.contains(it) }.joinToString(",")
                        val updatedAssoc = currentAssoc.copy(
                            name = name,
                            currentArchetype = archetype,
                            scheduledDays = daysString,
                            defaultStartTime = startTime,
                            defaultEndTime = endTime,
                            pinCode = if (pinCode.isNotBlank()) pinCode else currentAssoc.pinCode
                        )

                        scope.launch {
                            if (isNewAssoc) {
                                dao.insertAssociate(updatedAssoc)
                            } else {
                                dao.updateAssociate(updatedAssoc)
                            }
                            activeDialogAssoc = null
                        }
                    }
                }) { Text("Save Profile") }
            },
            dismissButton = { TextButton(onClick = { activeDialogAssoc = null }) { Text("Cancel") } }
        )

        if (showStartTimePicker) { AlertDialog(onDismissRequest = { showStartTimePicker = false }, text = { TimePicker(state = startPickerState) }, confirmButton = { TextButton(onClick = { startTime = String.format(Locale.getDefault(), "%02d:%02d", startPickerState.hour, startPickerState.minute); showStartTimePicker = false }) { Text("Confirm") } }) }
        if (showEndTimePicker) { AlertDialog(onDismissRequest = { showEndTimePicker = false }, text = { TimePicker(state = endPickerState) }, confirmButton = { TextButton(onClick = { endTime = String.format(Locale.getDefault(), "%02d:%02d", endPickerState.hour, endPickerState.minute); showEndTimePicker = false }) { Text("Confirm") } }) }
    }
}