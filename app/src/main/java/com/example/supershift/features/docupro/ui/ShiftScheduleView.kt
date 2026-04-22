package com.example.supershift.features.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.ScheduleEntry
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScheduleView(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val activeShift by dao.getActiveShift().collectAsState(initial = null)

    var showAddDialog by remember { mutableStateOf(false) }

    // NEW: State for Editing an existing entry
    var entryToEdit by remember { mutableStateOf<ScheduleEntry?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Shift")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Daily Shift Schedule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Adjust active shift times or reassign zones on the fly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            if (schedule.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No shifts scheduled. Tap + to add.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(schedule) { entry ->
                        // Match the entry to the Roster to get their current zone
                        val assoc = associates.find { it.name.equals(entry.associateName, ignoreCase = true) }

                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.associateName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodyMedium)
                                    }

                                    // NEW: DYNAMIC ZONE SWAPPER
                                    if (assoc != null && assoc.currentArchetype != "MOD") {
                                        Spacer(Modifier.height(8.dp))
                                        AssistChip(
                                            onClick = {
                                                // Cycle the zone instantly!
                                                val nextZone = when (assoc.currentArchetype) {
                                                    "Kitchen" -> "POS"
                                                    "POS" -> "Float"
                                                    else -> "Kitchen"
                                                }
                                                scope.launch { dao.updateAssociate(assoc.copy(currentArchetype = nextZone)) }
                                            },
                                            label = { Text("Zone: ${assoc.currentArchetype}") },
                                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Swap", modifier = Modifier.size(16.dp)) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, labelColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                        )
                                    } else if (assoc?.currentArchetype == "MOD") {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Zone: COMMAND (MOD)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Row {
                                    // NEW: Edit Time Button
                                    IconButton(onClick = { entryToEdit = entry }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Time", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { scope.launch { dao.deleteScheduleEntry(entry) } }) {
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

    // --- ADD SHIFT DIALOG ---
    if (showAddDialog) {
        var initialStartHour = 22; var initialStartMinute = 0
        var initialEndHour = 6; var initialEndMinute = 30

        activeShift?.let { shift ->
            if (shift.startTimeMs > 0) {
                val startCal = Calendar.getInstance().apply { timeInMillis = shift.startTimeMs }
                initialStartHour = startCal.get(Calendar.HOUR_OF_DAY); initialStartMinute = startCal.get(Calendar.MINUTE)
            }
            if (shift.endTimeMs > 0) {
                val endCal = Calendar.getInstance().apply { timeInMillis = shift.endTimeMs }
                initialEndHour = endCal.get(Calendar.HOUR_OF_DAY); initialEndMinute = endCal.get(Calendar.MINUTE)
            }
        }

        var selectedAssociate by remember { mutableStateOf(associates.firstOrNull()?.name ?: "") }
        var startTime by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d:%02d", initialStartHour, initialStartMinute)) }
        var endTime by remember { mutableStateOf(String.format(Locale.getDefault(), "%02d:%02d", initialEndHour, initialEndMinute)) }
        var expanded by remember { mutableStateOf(false) }

        var showStartTimePicker by remember { mutableStateOf(false) }
        var showEndTimePicker by remember { mutableStateOf(false) }
        val startTimePickerState = rememberTimePickerState(initialHour = initialStartHour, initialMinute = initialStartMinute)
        val endTimePickerState = rememberTimePickerState(initialHour = initialEndHour, initialMinute = initialEndMinute)

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Schedule Shift") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(value = selectedAssociate, onValueChange = {}, readOnly = true, label = { Text("Select Associate") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            associates.forEach { assoc -> DropdownMenuItem(text = { Text(assoc.name) }, onClick = { selectedAssociate = assoc.name; expanded = false }) }
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
                    if (selectedAssociate.isNotBlank()) {
                        scope.launch {
                            dao.insertScheduleEntry(ScheduleEntry(associateName = selectedAssociate, startTime = startTime, endTime = endTime))
                            showAddDialog = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )

        if (showStartTimePicker) { AlertDialog(onDismissRequest = { showStartTimePicker = false }, text = { TimePicker(state = startTimePickerState) }, confirmButton = { TextButton(onClick = { startTime = String.format(Locale.getDefault(), "%02d:%02d", startTimePickerState.hour, startTimePickerState.minute); showStartTimePicker = false }) { Text("Confirm") } }) }
        if (showEndTimePicker) { AlertDialog(onDismissRequest = { showEndTimePicker = false }, text = { TimePicker(state = endTimePickerState) }, confirmButton = { TextButton(onClick = { endTime = String.format(Locale.getDefault(), "%02d:%02d", endTimePickerState.hour, endTimePickerState.minute); showEndTimePicker = false }) { Text("Confirm") } }) }
    }

    // --- EDIT EXISTING SHIFT DIALOG ---
    if (entryToEdit != null) {
        val sParts = entryToEdit!!.startTime.split(":")
        val eParts = entryToEdit!!.endTime.split(":")
        val sHour = sParts[0].toIntOrNull() ?: 22
        val sMin = sParts[1].toIntOrNull() ?: 0
        val eHour = eParts[0].toIntOrNull() ?: 6
        val eMin = eParts[1].toIntOrNull() ?: 30

        var startTime by remember { mutableStateOf(entryToEdit!!.startTime) }
        var endTime by remember { mutableStateOf(entryToEdit!!.endTime) }

        var showStartTimePicker by remember { mutableStateOf(false) }
        var showEndTimePicker by remember { mutableStateOf(false) }
        val startTimePickerState = rememberTimePickerState(initialHour = sHour, initialMinute = sMin)
        val endTimePickerState = rememberTimePickerState(initialHour = eHour, initialMinute = eMin)

        AlertDialog(
            onDismissRequest = { entryToEdit = null },
            title = { Text("Edit Shift Time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Adjusting hours for ${entryToEdit!!.associateName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = {}, readOnly = true, label = { Text("Start Time") }, trailingIcon = { IconButton(onClick = { showStartTimePicker = true }) { Icon(Icons.Default.Schedule, null) } }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endTime, onValueChange = {}, readOnly = true, label = { Text("End Time") }, trailingIcon = { IconButton(onClick = { showEndTimePicker = true }) { Icon(Icons.Default.Schedule, null) } }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        dao.insertScheduleEntry(entryToEdit!!.copy(startTime = startTime, endTime = endTime))
                        entryToEdit = null
                    }
                }) { Text("Update Shift") }
            },
            dismissButton = { TextButton(onClick = { entryToEdit = null }) { Text("Cancel") } }
        )

        if (showStartTimePicker) { AlertDialog(onDismissRequest = { showStartTimePicker = false }, text = { TimePicker(state = startTimePickerState) }, confirmButton = { TextButton(onClick = { startTime = String.format(Locale.getDefault(), "%02d:%02d", startTimePickerState.hour, startTimePickerState.minute); showStartTimePicker = false }) { Text("Confirm") } }) }
        if (showEndTimePicker) { AlertDialog(onDismissRequest = { showEndTimePicker = false }, text = { TimePicker(state = endTimePickerState) }, confirmButton = { TextButton(onClick = { endTime = String.format(Locale.getDefault(), "%02d:%02d", endTimePickerState.hour, endTimePickerState.minute); showEndTimePicker = false }) { Text("Confirm") } }) }
    }
}