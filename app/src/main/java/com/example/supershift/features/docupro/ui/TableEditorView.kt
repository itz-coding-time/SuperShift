package com.example.supershift.features.docupro.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.data.TableItem
import com.example.supershift.utils.CsvExporter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditorView(dao: SuperShiftDao) {
    val context = LocalContext.current
    var selectedStation by remember { mutableStateOf("Starter") }
    val scope = rememberCoroutineScope()
    val items by dao.getTableItemsByStation(selectedStation).collectAsState(initial = emptyList())
    var showAddItemDialog by remember { mutableStateOf(false) }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let {
                scope.launch {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        CsvExporter.exportTableFlips(stream, items)
                        Toast.makeText(
                            context,
                            "$selectedStation backed up successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Item",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text(
                "Table Flip Editor",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Manage your Midnight Flip items directly on the device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$selectedStation Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (items.isNotEmpty()) {
                    TextButton(onClick = {
                        val safeName = selectedStation.replace(" ", "_")
                        exportLauncher.launch("${safeName}_Flips.csv")
                    }) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Export CSV",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Backup to CSV")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items on $selectedStation. Tap + to add.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        // --- INLINE EDITING STATE ---
                        var isEditing by remember { mutableStateOf(false) }
                        var editName by remember(item.itemName) { mutableStateOf(item.itemName) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (isEditing) {
                                // EDIT MODE UI
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Item Name") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(modifier = Modifier.padding(start = 8.dp)) {
                                        IconButton(onClick = {
                                            if (editName.isNotBlank()) {
                                                scope.launch {
                                                    dao.updateTableItem(item.copy(itemName = editName))
                                                    isEditing = false
                                                }
                                            }
                                        }) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Save",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(onClick = {
                                            editName = item.itemName
                                            isEditing = false
                                        }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            } else {
                                // VIEW MODE UI
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        item.itemName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(onClick = { isEditing = true }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                dao.deleteTableItem(
                                                    item
                                                )
                                            }
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
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
                            dao.insertTableItems(
                                listOf(
                                    TableItem(
                                        itemName = newItemName,
                                        station = selectedStation
                                    )
                                )
                            )
                            val taskName = "Midnight Flip: $selectedStation"
                            val tasks = dao.getAllTasks().firstOrNull() ?: emptyList()
                            if (tasks.none { it.taskName == taskName }) {
                                dao.insertTask(
                                    ShiftTask(
                                        taskName = taskName,
                                        archetype = "Kitchen",
                                        priority = "High",
                                        isSticky = true
                                    )
                                )
                            }
                            showAddItemDialog = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddItemDialog = false
                }) { Text("Cancel") }
            }
        )
    }
}