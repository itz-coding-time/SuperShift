package com.example.supershift.features.hud.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.supershift.data.*
import com.example.supershift.utils.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveHudScreen(dao: SuperShiftDao, isDebugMode: Boolean = false) {
    val scope = rememberCoroutineScope()
    val activeShift by dao.getActiveShift().collectAsState(initial = null)

    val isShiftActive = activeShift?.isOpen == true || isDebugMode

    val allTasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var activePullTask by remember { mutableStateOf<ShiftTask?>(null) }
    var activeTableFlipTask by remember { mutableStateOf<ShiftTask?>(null) }
    var taskToDelegate by remember { mutableStateOf<ShiftTask?>(null) }
    var truckReceiveTask by remember { mutableStateOf<ShiftTask?>(null) }

    val pendingTasks = allTasks.filter { !it.isCompleted }

    val sortedPendingTasks = pendingTasks.sortedWith(
        compareBy<ShiftTask> { if (it.assignedTo != null) 0 else 1 }
            .thenBy { it.assignedTo ?: "" }
            .thenBy { if (it.assignedTo != null) it.id else 0 }
            .thenBy { when (it.priority) { "High" -> 1; "Normal" -> 2; "Low" -> 3; else -> 2 } }
            .thenBy { it.id }
    )

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
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Active Tasks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (activeShift?.isTruckNight == true) {
                    Icon(Icons.Default.LocalShipping, contentDescription = "Truck Night", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (activeShift?.isOpen != true) {
                if (isDebugMode) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("🛠️ DEBUG MODE ACTIVE.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("⚠️ NO ACTIVE SHIFT. Task completion disabled.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (sortedPendingTasks.isEmpty() && isShiftActive) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("All tasks complete. Excellent shift!") }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val displayOrder = listOf("Kitchen", "POS", "Float", "MOD")

                    displayOrder.forEach { archetype ->
                        val tasks = groupedTasks[archetype] ?: emptyList()
                        if (tasks.isNotEmpty() || (archetype == "Float" && behindZone != null)) {
                            item {
                                val zoneTitle = if (archetype == "Float" && !hasDedicatedFloat) "FLOAT ZONE (MOD COVERAGE)" else "$archetype ZONE"
                                val hasHighPriority = tasks.any { it.priority == "High" && !it.isCompleted }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text(zoneTitle.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (archetype == "MOD") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary)
                                    if (archetype != "MOD" && hasHighPriority && isShiftActive) {
                                        TextButton(onClick = { scope.launch { dao.pullHighPriorityToMod(archetype) } }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                            Text("PULL HIGH PRIORITY TO MOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            items(tasks, key = { it.id }) { task ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (!isShiftActive) return@rememberSwipeToDismissBoxState false
                                        when (dismissValue) {
                                            SwipeToDismissBoxValue.StartToEnd -> { scope.launch { dao.updateTask(task.copy(archetype = "MOD", assignedTo = "MOD")) }; false }
                                            SwipeToDismissBoxValue.EndToStart -> { taskToDelegate = task; false }
                                            else -> false
                                        }
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = isShiftActive && task.archetype != "MOD",
                                    enableDismissFromEndToStart = isShiftActive,
                                    backgroundContent = {
                                        val direction = dismissState.dismissDirection
                                        val color = when (direction) { SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiary; SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary; else -> Color.Transparent }
                                        val icon = when (direction) { SwipeToDismissBoxValue.StartToEnd -> Icons.Default.AdminPanelSettings; SwipeToDismissBoxValue.EndToStart -> Icons.Default.PersonSearch; else -> Icons.Default.Circle }
                                        val alignment = when (direction) { SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart; SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd; else -> Alignment.Center }
                                        Box(Modifier.fillMaxSize().background(color, shape = MaterialTheme.shapes.medium).padding(horizontal = 20.dp), contentAlignment = alignment) { Icon(icon, contentDescription = null, tint = Color.White) }
                                    }
                                ) {
                                    TaskCardUI(
                                        task = task,
                                        isShiftActive = isShiftActive,
                                        onClick = {
                                            if (task.taskName == "Receive & Count Truck") truckReceiveTask = task
                                            else if (task.isPullTask) activePullTask = task
                                            else if (task.taskName.startsWith("Midnight Flip:")) activeTableFlipTask = task
                                        },
                                        onComplete = { scope.launch { dao.updateTask(task.copy(isCompleted = true, completedBy = task.assignedTo ?: "Associate")) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (taskToDelegate != null) {
        val zoneAssociates = associates.filter { assoc ->
            if (assoc.currentArchetype != taskToDelegate?.archetype) return@filter false
            val sched = schedule.find { it.associateName.equals(assoc.name, ignoreCase = true) }
            val isOnClock = sched?.let { TimeUtils.isAssociateOnClock(it.startTime, it.endTime) } ?: true
            isOnClock || isDebugMode
        }

        AlertDialog(
            onDismissRequest = { taskToDelegate = null },
            title = { Text("Delegate Task") },
            text = {
                Column {
                    Text("Who is taking '${taskToDelegate?.taskName}'?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    if (zoneAssociates.isEmpty()) {
                        Text("No associates are currently active in this zone.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    } else {
                        zoneAssociates.forEach { assoc ->
                            Button(
                                onClick = { scope.launch { dao.updateTask(taskToDelegate!!.copy(assignedTo = assoc.name)); taskToDelegate = null } },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                            ) { Text(assoc.name) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { taskToDelegate = null }) { Text("Cancel") } }
        )
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
                        listOf("Kitchen", "POS", "Float", "MOD").forEach { arch -> FilterChip(selected = selectedArchetype == arch, onClick = { selectedArchetype = arch }, label = { Text(arch) }) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Priority:", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Low", "Normal", "High").forEach { prio ->
                            FilterChip(selected = selectedPriority == prio, onClick = { selectedPriority = prio }, label = { Text(prio) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (prio == "High") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer))
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
                        scope.launch { dao.insertTask(ShiftTask(taskName = newTaskName, archetype = selectedArchetype, isSticky = isSticky, priority = selectedPriority)); showAddTaskDialog = false }
                    }
                }) { Text("Assign") }
            },
            dismissButton = { TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") } }
        )
    }

    if (truckReceiveTask != null) { TruckManifestDialog(truckTask = truckReceiveTask!!, dao = dao, onClose = { truckReceiveTask = null }) }
    if (activePullTask != null) { InventoryPullDialog(pullTask = activePullTask!!, dao = dao, onClose = { activePullTask = null }) }
    if (activeTableFlipTask != null) { TableFlipDialog(station = activeTableFlipTask!!.taskName.replace("Midnight Flip: ", "").trim(), flipTask = activeTableFlipTask!!, dao = dao, onClose = { activeTableFlipTask = null }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruckManifestDialog(truckTask: ShiftTask, dao: SuperShiftDao, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()

    var countFreezer by remember { mutableStateOf("") }
    var countCooler by remember { mutableStateOf("") }
    var countKitchen by remember { mutableStateOf("") }
    var countSupplies by remember { mutableStateOf("") }
    var countAmbientBack by remember { mutableStateOf("") }
    var countAmbientSoda by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.systemBarsPadding().imePadding()) {
                TopAppBar(
                    title = { Text("Receive Truck Manifest") },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } }
                )

                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Log the Cube counts below. The system will automatically split and assign the Put Away tasks based on your entries.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    }

                    val renderZoneInput = @Composable { title: String, value: String, onValueChange: (String) -> Unit ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = value, onValueChange = onValueChange, label = { Text("Cubes") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    modifier = Modifier.width(100.dp), singleLine = true
                                )
                            }
                        }
                    }

                    item { renderZoneInput("Freezer", countFreezer) { countFreezer = it } }
                    item { renderZoneInput("Cooler", countCooler) { countCooler = it } }
                    item { renderZoneInput("Kitchen & Food", countKitchen) { countKitchen = it } }
                    item { renderZoneInput("Dry Supplies", countSupplies) { countSupplies = it } }

                    item {
                        Spacer(Modifier.height(12.dp))
                        Text("AMBIENT SPLIT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    item { renderZoneInput("Ambient (Backroom)", countAmbientBack) { countAmbientBack = it } }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Ambient (Soda Cooler)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = countAmbientSoda, onValueChange = { countAmbientSoda = it }, label = { Text("Cubes") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    modifier = Modifier.width(100.dp), singleLine = true
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            val newTasks = mutableListOf<ShiftTask>()

                            fun evaluateAndCreate(countStr: String, title: String, targetArch: String) {
                                val count = countStr.toIntOrNull() ?: 0
                                if (count > 0) {
                                    newTasks.add(ShiftTask(
                                        taskName = "Put Away $title ($count Cubes)",
                                        archetype = targetArch,
                                        priority = "High",
                                        isTruckTask = true
                                    ))
                                }
                            }

                            evaluateAndCreate(countFreezer, "Freezer", "Float")
                            evaluateAndCreate(countCooler, "Cooler", "Float")
                            evaluateAndCreate(countKitchen, "Kitchen Goods", "Kitchen")
                            evaluateAndCreate(countSupplies, "Supplies", "Float")
                            evaluateAndCreate(countAmbientBack, "Ambient (Backroom)", "Float")
                            evaluateAndCreate(countAmbientSoda, "Ambient (Soda Cooler)", "POS")

                            if (newTasks.isNotEmpty()) {
                                dao.insertTasks(newTasks)
                            }

                            dao.updateTask(truckTask.copy(isCompleted = true, completedBy = "MOD"))
                            onClose()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("GENERATE PUT AWAY TASKS")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCardUI(task: ShiftTask, isShiftActive: Boolean, onClick: () -> Unit, onComplete: () -> Unit) {
    var isCompleting by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isCompleting) {
        if (isCompleting) { delay(350); onComplete() }
    }

    val exitAnimation = when (task.priority) {
        "High" -> scaleOut(targetScale = 1.1f, animationSpec = tween(200)) + fadeOut(tween(200)) + shrinkVertically(animationSpec = tween(200, delayMillis = 150))
        "Low" -> fadeOut(tween(300)) + shrinkVertically(animationSpec = tween(300))
        else -> shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(350)) + fadeOut(tween(350))
    }

    val buttonColors = when {
        task.taskName == "Receive & Count Truck" -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
        task.priority == "High" -> ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
        task.priority == "Low" -> ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> ButtonDefaults.buttonColors()
    }

    AnimatedVisibility(visible = !isCompleting, enter = fadeIn() + expandVertically(), exit = exitAnimation) {
        val cardBgColor by animateColorAsState(targetValue = if (isCompleting && task.priority == "Normal") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, animationSpec = tween(250), label = "CardColorAnim")

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(if (task.priority == "High") 4.dp else 2.dp), colors = CardDefaults.cardColors(containerColor = cardBgColor), onClick = onClick) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(task.taskName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (task.assignedTo != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(task.assignedTo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!task.taskDescription.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(task.taskDescription!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (task.taskName == "Receive & Count Truck" || task.isPullTask || task.taskName.startsWith("Midnight Flip:")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val hintText = when {
                            task.taskName == "Receive & Count Truck" -> "Tap to input manifest cube counts"
                            task.isPullTask -> "Tap to execute pull list"
                            else -> "Tap to begin audit"
                        }
                        Text(hintText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Button(
                    onClick = { if (task.priority == "High") haptic.performHapticFeedback(HapticFeedbackType.LongPress); isCompleting = true },
                    enabled = isShiftActive && !isCompleting, colors = buttonColors
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (task.priority == "High") "DONE • HIGH" else if (task.priority == "Low") "DONE • LOW" else "DONE")
                }
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
            Column(modifier = Modifier.systemBarsPadding()) {
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
                ) { Text("SUBMIT FLIP & LOG WASTE") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryPullDialog(pullTask: ShiftTask, dao: SuperShiftDao, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val category = pullTask.pullCategory ?: return
    val inventoryList by dao.getInventoryByCategory(category).collectAsState(initial = emptyList())

    val isExecutionPhase = pullTask.taskName.contains("Execution", ignoreCase = true)

    val itemsToPull = inventoryList.filter { (it.amountNeeded ?: 0) > 0 }
    val totalToPull = itemsToPull.size
    val pulledCount = itemsToPull.count { it.isPulled }
    val progress = if (totalToPull > 0) pulledCount.toFloat() / totalToPull else 0f

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.systemBarsPadding().imePadding()) {
                TopAppBar(
                    title = { Text(if (isExecutionPhase) "$category Pull Execution" else "$category Pull List") },
                    navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") } }
                )

                if (isExecutionPhase) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                        Text("Execution Progress", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(12.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("$pulledCount / $totalToPull Items Pulled", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))

                        if (itemsToPull.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No items needed! You are fully stocked.", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(itemsToPull) { item ->
                                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (item.isPulled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(item.itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("Pull Quantity: ${item.amountNeeded}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                                            }
                                            Checkbox(checked = item.isPulled, onCheckedChange = { scope.launch { dao.updateInventoryItem(item.copy(isPulled = it)) } }, modifier = Modifier.scale(1.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                dao.updateTask(pullTask.copy(isCompleted = true))
                                onClose()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        enabled = pulledCount == totalToPull || itemsToPull.isEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("FINISH PULL EXECUTION") }

                } else {
                    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            modifier = Modifier.width(140.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                dao.updateTask(pullTask.copy(isCompleted = true))
                                dao.insertTask(
                                    ShiftTask(
                                        taskName = "${category} Pull Execution",
                                        archetype = pullTask.archetype,
                                        isPullTask = true,
                                        pullCategory = category,
                                        priority = pullTask.priority,
                                        isSticky = false
                                    )
                                )
                                onClose()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("SUBMIT MATH & GENERATE PULL LIST") }
                }
            }
        }
    }
}