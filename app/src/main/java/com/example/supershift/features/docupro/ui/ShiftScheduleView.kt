package com.example.supershift.features.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.ScheduleEntry
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScheduleView(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Shift")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Daily Shift Schedule", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Manually log associate shift times to track coverage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            if (schedule.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No shifts scheduled. Tap + to add.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(schedule) { entry ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(entry.associateName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodyMedium)
                                    }
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

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var start by remember { mutableStateOf("22:00") }
        var end by remember { mutableStateOf("06:30") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Scheduled Shift") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Associate Name") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("Start") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("End") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            dao.insertScheduleEntry(ScheduleEntry(associateName = name, startTime = start, endTime = end))
                            showAddDialog = false
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}