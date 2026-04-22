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
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    val tasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val overallProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val activeMods = associates.filter { it.currentArchetype == "MOD" }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Shift Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SHIFT PROGRESS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { overallProgress }, modifier = Modifier.fillMaxWidth().height(12.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("$completedTasks / $totalTasks Tasks Completed", style = MaterialTheme.typography.bodySmall)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                Text("Active MOD(s):", style = MaterialTheme.typography.labelSmall)
                if (activeMods.isEmpty()) {
                    Text("No MOD Assigned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    activeMods.forEach { mod ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(mod.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Floor Zones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val archetypes = listOf("Kitchen", "POS", "Float")

            items(archetypes) { archetype ->
                val archTasks = tasks.filter { it.archetype == archetype }
                val archAssocs = associates.filter { it.currentArchetype == archetype }
                val completedCount = archTasks.count { it.isCompleted }
                val totalCount = archTasks.size

                val activeTask = archTasks.filter { !it.isCompleted }.sortedBy { when (it.priority) { "High" -> 1; "Normal" -> 2; "Low" -> 3; else -> 2 } }.firstOrNull()

                Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${archetype.uppercase()}: $completedCount/$totalCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            if (activeTask == null && archTasks.isNotEmpty()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF4CAF50))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Text("Assigned: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(text = if (archAssocs.isEmpty()) "Unmanned!" else archAssocs.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium, color = if (archAssocs.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (activeTask != null) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    when (dismissValue) {
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            scope.launch { dao.updateTask(activeTask.copy(isCompleted = true, completedBy = "MOD")) }
                                            false // Spring back cleanly
                                        }
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            val assignedName = activeTask.assignedTo ?: archAssocs.firstOrNull()?.name ?: "Associate"
                                            scope.launch { dao.updateTask(activeTask.copy(isCompleted = true, completedBy = assignedName)) }
                                            false // Spring back cleanly
                                        }
                                        else -> false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val direction = dismissState.dismissDirection
                                    val color = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.tertiary
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primary
                                        else -> Color.Transparent
                                    }
                                    // EXTENDED ICONS RESTORED!
                                    val icon = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.AdminPanelSettings
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.PersonSearch
                                        else -> Icons.Default.Circle
                                    }
                                    val alignment = when (direction) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        else -> Alignment.Center
                                    }
                                    Box(Modifier.fillMaxSize().background(color, shape = MaterialTheme.shapes.small).padding(horizontal = 16.dp), contentAlignment = alignment) {
                                        Icon(icon, contentDescription = null, tint = Color.White)
                                    }
                                }
                            ) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Working on: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Text(text = activeTask.taskName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                                    }
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