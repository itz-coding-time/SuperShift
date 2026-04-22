package com.example.supershift.features.hud.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.utils.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegationScreen(dao: SuperShiftDao, isDebugMode: Boolean = false) {
    val scope = rememberCoroutineScope()
    val activeShift by dao.getActiveShift().collectAsState(initial = null)

    val isShiftActive = activeShift?.isOpen == true || isDebugMode

    val allTasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Quick Delegation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Swipe targets to clear them or override assignments.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val archetypes = listOf("Kitchen", "POS", "Float")

            items(archetypes) { archetype ->
                val zoneTasksAll = allTasks.filter { it.archetype == archetype }
                val zoneTasksPending = zoneTasksAll.filter { !it.isCompleted }
                val allZoneAssocs = associates.filter { it.currentArchetype == archetype }

                val activeAssocs = allZoneAssocs.filter { assoc ->
                    val sched = schedule.find { it.associateName.equals(assoc.name, ignoreCase = true) }
                    val isOnClock = sched?.let { TimeUtils.isAssociateOnClock(it.startTime, it.endTime) } ?: true
                    val hasAssignedTasks = zoneTasksPending.any { it.assignedTo == assoc.name }

                    isOnClock || hasAssignedTasks || isDebugMode
                }

                val completedCount = zoneTasksAll.count { it.isCompleted }
                val totalCount = zoneTasksAll.size

                val unassignedPool = zoneTasksPending.filter { it.assignedTo == null }.sortedWith(
                    compareBy({ when (it.priority) { "High" -> 1; "Normal" -> 2; "Low" -> 3; else -> 2 } }, { it.id })
                ).toMutableList()

                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${archetype.uppercase()} ZONE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text("$completedCount/$totalCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Text("Active: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(text = if (activeAssocs.isEmpty()) "Unmanned!" else activeAssocs.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium, color = if (activeAssocs.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (activeAssocs.isNotEmpty()) {
                            activeAssocs.forEach { assoc ->
                                val myQueue = zoneTasksPending.filter { it.assignedTo == assoc.name }.sortedBy { it.id }
                                val activeTask = myQueue.firstOrNull() ?: unassignedPool.removeFirstOrNull()
                                val queuedCount = myQueue.size

                                if (activeTask != null) {
                                    val isPriorityPull = activeTask.assignedTo == null

                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { dismissValue ->
                                            if (!isShiftActive) return@rememberSwipeToDismissBoxState false
                                            when (dismissValue) {
                                                SwipeToDismissBoxValue.StartToEnd -> { scope.launch { dao.updateTask(activeTask.copy(isCompleted = true, completedBy = "MOD")) }; false }
                                                SwipeToDismissBoxValue.EndToStart -> { scope.launch { dao.updateTask(activeTask.copy(isCompleted = true, completedBy = assoc.name)) }; false }
                                                else -> false
                                            }
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = isShiftActive,
                                        enableDismissFromEndToStart = isShiftActive,
                                        backgroundContent = {
                                            val direction = dismissState.dismissDirection
                                            val color = when (direction) { SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiary; SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary; else -> Color.Transparent }
                                            val icon = when (direction) { SwipeToDismissBoxValue.StartToEnd -> Icons.Default.AdminPanelSettings; SwipeToDismissBoxValue.EndToStart -> Icons.Default.PersonSearch; else -> Icons.Default.Person }
                                            val alignment = when (direction) { SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart; SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd; else -> Alignment.Center }
                                            Box(Modifier.fillMaxSize().background(color, shape = MaterialTheme.shapes.small).padding(horizontal = 16.dp), contentAlignment = alignment) { Icon(icon, contentDescription = null, tint = Color.White) }
                                        }
                                    ) {
                                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(8.dp))
                                                Text("${assoc.name}: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                                                Text(text = activeTask.taskName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                                if (queuedCount > 1) {
                                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(start = 8.dp)) {
                                                        Text("+${queuedCount - 1} queued", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                                    }
                                                } else if (isPriorityPull) {
                                                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(start = 8.dp)) {
                                                        Text("Priority Auto-Pull", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(color = Color(0xFF4CAF50).copy(alpha = 0.1f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                                            Spacer(Modifier.width(8.dp))
                                            Text("${assoc.name}: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                                            Text("Idle (All Clear!)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val backlog = unassignedPool.firstOrNull()
                            if (backlog != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Zone Backlog (Next Up):", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("- ${backlog.taskName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                        } else {
                            val activeTask = unassignedPool.firstOrNull()
                            if (activeTask != null) {
                                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("UNMANNED TARGET: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        Text(text = activeTask.taskName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text("All Caught Up!", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}