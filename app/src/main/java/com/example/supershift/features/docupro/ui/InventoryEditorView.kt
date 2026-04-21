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
import com.example.supershift.data.InventoryItem
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.utils.CsvExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryEditorView(dao: SuperShiftDao) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("RTE") }
    val scope = rememberCoroutineScope()
    val items by dao.getInventoryByCategory(selectedCategory).collectAsState(initial = emptyList())
    var showAddItemDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            scope.launch {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    CsvExporter.exportInventory(stream, items)
                    Toast.makeText(context, "$selectedCategory backed up successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Item", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Inventory Editor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Manage your dynamic Pull Lists here. No CSVs required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("RTE", "Prep", "Bread", "Bakery").forEachIndexed { index, cat ->
                    SegmentedButton(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                    ) { Text(cat) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$selectedCategory Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (items.isNotEmpty()) {
                    TextButton(onClick = { exportLauncher.launch("${selectedCategory}_Pull_Backup.csv") }) {
                        Icon(Icons.Default.Send, contentDescription = "Export CSV", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Backup to CSV")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items in $selectedCategory. Tap + to create.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        // --- INLINE EDITING STATE ---
                        var isEditing by remember { mutableStateOf(false) }
                        var editName by remember(item.itemName) { mutableStateOf(item.itemName) }
                        var editBuildTo by remember(item.buildTo) { mutableStateOf(item.buildTo) }

                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            if (isEditing) {
                                // EDIT MODE UI
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Item Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(value = editBuildTo, onValueChange = { editBuildTo = it }, label = { Text("Build To") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 8.dp)) {
                                        IconButton(onClick = {
                                            if (editName.isNotBlank() && editBuildTo.isNotBlank()) {
                                                scope.launch {
                                                    dao.updateInventoryItem(item.copy(itemName = editName, buildTo = editBuildTo))
                                                    isEditing = false
                                                }
                                            }
                                        }) { Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary) }

                                        IconButton(onClick = {
                                            editName = item.itemName
                                            editBuildTo = item.buildTo
                                            isEditing = false
                                        }) { Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            } else {
                                // VIEW MODE UI
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Build To: ${item.buildTo}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Row {
                                        IconButton(onClick = { isEditing = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { scope.launch { dao.deleteInventoryItem(item) } }) {
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

    if (showAddItemDialog) {
        var newItemName by remember { mutableStateOf("") }
        var newBuildTo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add $selectedCategory Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newItemName, onValueChange = { newItemName = it }, label = { Text("Item Name (e.g. Turkey)") })
                    OutlinedTextField(value = newBuildTo, onValueChange = { newBuildTo = it }, label = { Text("Build To Amount (e.g. 12 BT)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newItemName.isNotBlank() && newBuildTo.isNotBlank()) {
                        scope.launch {
                            dao.insertInventoryItem(InventoryItem(itemName = newItemName, buildTo = newBuildTo, category = selectedCategory))
                            val defaultArch = when (selectedCategory) {
                                "RTE", "Bakery" -> "POS"
                                "Prep", "Bread" -> "Kitchen"
                                else -> "Float"
                            }
                            if (dao.getTaskByPullCategory(selectedCategory) == null) {
                                dao.insertTask(ShiftTask(taskName = "$selectedCategory Pull", archetype = defaultArch, isPullTask = true, pullCategory = selectedCategory, basePoints = 50))
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