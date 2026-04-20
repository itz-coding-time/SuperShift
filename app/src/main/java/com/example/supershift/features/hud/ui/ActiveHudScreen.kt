package com.example.supershift.features.hud.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.supershift.data.IncidentLog
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveHudScreen(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    val allTasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var activePullCategory by remember { mutableStateOf<String?>(null) }
    var activeTableFlipTask by remember { mutableStateOf<ShiftTask?>(null) }

    val pendingTasks = allTasks.filter { !it.isCompleted }
    val sortedPendingTasks = pendingTasks.sortedBy { when (it.priority) { "High" -> 1; "Normal" -> 2; "Low" -> 3; else -> 2 } }
    val groupedTasks = sortedPendingTasks.groupBy { it.archetype }

    val kitchenPending = sortedPendingTasks.count { it.archetype == "Kitchen" }
    val posPending = sortedPendingTasks.count { it.archetype == "POS" }
    val behindZone = when {
        kitchenPending > posPending -> "Kitchen"
        posPending > kitchenPending -> "POS"
        else -> null
    }

    val hasDedicatedFloat = associates.any { it.currentArchetype == "Float" }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddTaskDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Active Tasks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            if (sortedPendingTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("All tasks complete. Excellent shift!") }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                    val displayOrder = listOf("Kitchen", "POS", "Float", "MOD")

                    displayOrder.forEach { archetype ->
                        val tasks = groupedTasks[archetype] ?: emptyList()
                        if (tasks.isNotEmpty() || (archetype == "Float" && behindZone != null)) {
                            item {
                                val zoneTitle = if (archetype == "Float" && !hasDedicatedFloat) "FLOAT ZONE (MOD COVERAGE)" else "$archetype ZONE"
                                val hasHighPriority = tasks.any { it.priority == "High" && !it.isCompleted }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text(zoneTitle.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (archetype == "MOD") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
                                    if (archetype != "MOD" && hasHighPriority) {
                                        TextButton(onClick = { scope.launch { dao.pullHighPriorityToMod(archetype) } }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                            Text("PULL HIGH PRIORITY TO MOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            items(tasks) { task ->
                                TaskCardUI(
                                    task = task,
                                    onClick = {
                                        if (task.isPullTask) activePullCategory = task.pullCategory
                                        else if (task.taskName.startsWith("Midnight Flip:")) activeTableFlipTask = task
                                    },
                                    onComplete = { scope.launch { dao.updateTask(task.copy(isCompleted = true)) } }
                                )
                            }

                            if (archetype == "Float" && behindZone != null) {
                                val assistTasks = sortedPendingTasks.filter { it.archetype == behindZone }.take(3)
                                if (assistTasks.isNotEmpty()) {
                                    item {
                                        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                            Text("🚨 PRIORITY ASSIST: ${behindZone.uppercase()} IS BEHIND", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(8.dp))
                                        }
                                    }
                                    items(assistTasks) { task ->
                                        TaskCardUI(task, onClick = { if (task.isPullTask) activePullCategory = task.pullCategory else if (task.taskName.startsWith("Midnight Flip:")) activeTableFlipTask = task }, onComplete = { scope.launch { dao.updateTask(task.copy(isCompleted = true)) } })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        var newTaskName by remember { mutableStateOf("") }
        var selectedArchetype by remember { mutableStateOf("Kitchen") }
        var isSticky by remember { mutableStateOf(false) }
        var selectedPriority by remember { mutableStateOf("Normal") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Assign New Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTaskName, onValueChange = { newTaskName = it }, label = { Text("Task Description") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Assign to Zone:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Kitchen", "POS", "Float", "MOD").forEach { arch ->
                            FilterChip(selected = selectedArchetype == arch, onClick = { selectedArchetype = arch }, label = { Text(arch) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Priority:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Low", "Normal", "High").forEach { prio ->
                            FilterChip(
                                selected = selectedPriority == prio,
                                onClick = { selectedPriority = prio },
                                label = { Text(prio) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (prio == "High") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isSticky, onCheckedChange = { isSticky = it })
                        Text("Make Sticky (Repeats every shift)", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newTaskName.isNotBlank()) {
                        scope.launch {
                            dao.insertTask(ShiftTask(taskName = newTaskName, archetype = selectedArchetype, isSticky = isSticky, priority = selectedPriority))
                            showAddTaskDialog = false
                        }
                    }
                }) { Text("Assign") }
            },
            dismissButton = { TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") } }
        )
    }

    if (activePullCategory != null) {
        InventoryPullDialog(category = activePullCategory!!, dao = dao, onClose = { activePullCategory = null })
    }

    if (activeTableFlipTask != null) {
        val stationName = activeTableFlipTask!!.taskName.replace("Midnight Flip: ", "").trim()
        TableFlipDialog(station = stationName, flipTask = activeTableFlipTask!!, dao = dao, onClose = { activeTableFlipTask = null })
    }
}

@Composable
fun TaskCardUI(task: ShiftTask, onClick: () -> Unit, onComplete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.taskName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (task.priority == "High") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) { Text("HIGH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Black) }
                    } else if (task.priority == "Low") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(color = Color.LightGray, shape = MaterialTheme.shapes.small) { Text("LOW", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                    }
                }

                if (!task.taskDescription.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(task.taskDescription!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (task.isPullTask || task.taskName.startsWith("Midnight Flip:")) {
                    Text(if (task.isPullTask) "Tap to begin count" else "Tap to begin audit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Button(onClick = onComplete) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("DONE")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableFlipDialog(station: String, flipTask: ShiftTask, dao: SuperShiftDao, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val items by dao.getTableItemsByStation(station).collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                TopAppBar(
                    title = { Text("$station Midnight Flip") },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } }
                )
                LazyColumn(modifier = Modifier.padding(16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (item.isInitialed) "Initialed (Safe)" else "Blank (Waste)", color = if (item.isInitialed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                        Switch(
                                            checked = item.isInitialed,
                                            onCheckedChange = { scope.launch { dao.updateTableItem(item.copy(isInitialed = it, wasteAmount = if (it) null else item.wasteAmount)) } },
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }
                                if (!item.isInitialed) {
                                    OutlinedTextField(
                                        value = item.wasteAmount ?: "",
                                        onValueChange = { scope.launch { dao.updateTableItem(item.copy(wasteAmount = it)) } },
                                        label = { Text("Waste Amount (UOM)") },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            val wastedItems = items.filter { !it.isInitialed }
                            if (wastedItems.isNotEmpty()) {
                                val wasteLog = java.lang.StringBuilder("Waste Report:\n")
                                wastedItems.forEach { wasteLog.append("- ${it.itemName}: ${it.wasteAmount ?: "Unspecified"}\n") }
                                dao.insertIncident(IncidentLog(associateId = -1, category = "Midnight Waste: $station", description = wasteLog.toString().trim(), timestampMs = System.currentTimeMillis()))
                                dao.insertTask(ShiftTask(taskName = "Record $station Waste", archetype = "MOD", priority = "High", taskDescription = wasteLog.toString().trim(), isSticky = false))
                            }
                            dao.resetTableItemsByStation(station)
                            dao.updateTask(flipTask.copy(isCompleted = true))
                            onClose()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("SUBMIT FLIP & LOG WASTE")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryPullDialog(category: String, dao: SuperShiftDao, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val inventoryList by dao.getInventoryByCategory(category).collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column {
                TopAppBar(
                    title = { Text("$category Pull List") },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } }
                )
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(inventoryList) { item ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Build To: ${item.buildTo}", style = MaterialTheme.typography.bodySmall)
                                        Text("Needed: ${item.amountNeeded ?: "?"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                                    }
                                    OutlinedTextField(
                                        value = item.amountHave?.toString() ?: "",
                                        onValueChange = { newValue ->
                                            val have = newValue.toIntOrNull() ?: 0
                                            val btDigits = item.buildTo.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                                            val needed = maxOf(0, btDigits - have)
                                            scope.launch { dao.updateInventoryItem(item.copy(amountHave = have, amountNeeded = needed)) }
                                        },
                                        label = { Text("Amount Have") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(140.dp)
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