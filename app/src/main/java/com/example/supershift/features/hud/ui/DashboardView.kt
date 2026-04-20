package com.example.supershift.features.hud.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao

@Composable
fun DashboardScreen(dao: SuperShiftDao) {
    val tasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val overallProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f

    val activeMods = associates.filter { it.currentArchetype == "MOD" }
    val otherAssociates = associates.filter { it.currentArchetype != "MOD" }

    // --- MOD AUTO-ASSIGN LOGIC ---
    // 1. Look for explicit MOD tasks first.
    val pendingModTasks = tasks.filter { it.archetype == "MOD" && !it.isCompleted }
    // 2. Look for Float tasks if no MOD tasks exist.
    val pendingFloatTasks = tasks.filter { it.archetype == "Float" && !it.isCompleted }

    val modWorkingOnTask = pendingModTasks.firstOrNull() ?: pendingFloatTasks.firstOrNull()
    val isAutoAssignedFloat = pendingModTasks.isEmpty() && pendingFloatTasks.isNotEmpty()

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Shift Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        // --- COMMAND CENTER: OVERALL SHIFT & MOD STATUS ---
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

                Spacer(modifier = Modifier.height(12.dp))

                // MOD "WORKING ON" SECTION
                Row {
                    Text("Working on: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(
                        text = if (modWorkingOnTask != null) {
                            if (isAutoAssignedFloat) "(Auto-Float) ${modWorkingOnTask.taskName}" else modWorkingOnTask.taskName
                        } else {
                            "All Caught Up!"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (modWorkingOnTask != null) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- SHIFT SCHEDULE CARD ---
        if (schedule.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Scheduled Shift", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    schedule.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.associateName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${entry.startTime} - ${entry.endTime}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Floor Zones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // --- TRUST BUT VERIFY CARDS ---
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Removed MOD from the Floor Zones!
            val archetypes = listOf("Kitchen", "POS", "Float")

            items(archetypes) { archetype ->
                val archTasks = tasks.filter { it.archetype == archetype }
                val archAssocs = associates.filter { it.currentArchetype == archetype }
                val completedCount = archTasks.count { it.isCompleted }
                val totalCount = archTasks.size
                val activeTask = archTasks.firstOrNull { !it.isCompleted }

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
                            Text(
                                text = if (archAssocs.isEmpty()) "Unmanned!" else archAssocs.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (archAssocs.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row {
                            Text("Working on: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Text(
                                text = activeTask?.taskName ?: if (archTasks.isEmpty()) "No tasks assigned" else "All Caught Up!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (activeTask != null) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}