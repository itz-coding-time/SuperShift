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
import com.example.supershift.data.InventoryItem
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryEditorView(dao: SuperShiftDao) {
    var selectedCategory by remember { mutableStateOf("RTE") }
    val scope = rememberCoroutineScope()
    val items by dao.getInventoryByCategory(selectedCategory).collectAsState(initial = emptyList())
    var showAddItemDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddItemDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Inventory Editor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Manage your dynamic Pull Lists here. No CSVs required.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
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

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items in $selectedCategory. Tap + to create.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(item.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Build To: ${item.buildTo}", style = MaterialTheme.typography.bodyMedium)
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
                            // Ensure the master Pull Task exists on the floor
                            val defaultArch = if (selectedCategory == "RTE") "Float" else "Kitchen"
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