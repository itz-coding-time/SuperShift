package com.example.supershift.features.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.data.TableItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditorView(dao: SuperShiftDao) {
    var selectedStation by remember { mutableStateOf("Starter") }
    val scope = rememberCoroutineScope()
    val items by dao.getTableItemsByStation(selectedStation).collectAsState(initial = emptyList())
    var showAddItemDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Table Flip Editor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Manage your Midnight Flip items directly on the device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // Station Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Starter", "Finisher A", "Finisher B").forEachIndexed { index, station ->
                    SegmentedButton(
                        selected = selectedStation == station,
                        onClick = { selectedStation = station },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                    ) { Text(station) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items on $selectedStation. Tap + to add.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { scope.launch { dao.deleteTableItem(item) } }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        var newItemName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add to $selectedStation") },
            text = {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    label = { Text("Item Name (e.g. Turkey)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newItemName.isNotBlank()) {
                        scope.launch {
                            // Add item to the table
                            dao.insertTableItems(listOf(TableItem(itemName = newItemName, station = selectedStation)))

                            // Ensure the master Flip Task exists on the floor
                            val taskName = "Midnight Flip: $selectedStation"
                            val tasks = dao.getAllTasks().firstOrNull() ?: emptyList()
                            if (tasks.none { it.taskName == taskName }) {
                                dao.insertTask(ShiftTask(taskName = taskName, archetype = "Kitchen", priority = "High", isSticky = true))
                            }
                            showAddItemDialog = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddItemDialog = false }) { Text("Cancel") } }
        )
    }
}