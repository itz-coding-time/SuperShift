package com.example.supershift.features.hud.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.utils.TimeUtils
import kotlinx.coroutines.delay

@Composable
fun OverviewScreen(dao: SuperShiftDao) {
    val tasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())
    val activeShift by dao.getActiveShift().collectAsState(initial = null)

    // Force UI refresh every minute so the time bar moves live
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            currentTime = System.currentTimeMillis()
        }
    }

    val activeMods = associates.filter { it.currentArchetype == "MOD" }
    val isShiftActive = activeShift?.isOpen == true

    // GLOBAL FALLBACK PACING (Used if no schedules are entered)
    val globalTimePct = if (isShiftActive && activeShift!!.endTimeMs > activeShift!!.startTimeMs) {
        val totalMs = activeShift!!.endTimeMs - activeShift!!.startTimeMs
        val passedMs = currentTime - activeShift!!.startTimeMs
        (passedMs.toFloat() / totalMs).coerceIn(0f, 1f)
    } else 0f

    // SMART GLOBAL PACING: Averages the exact schedule progress of ALL associates currently clocked in.
    val scheduledAssociatesProgress = associates.mapNotNull { assoc ->
        schedule.find { it.associateName.equals(assoc.name, ignoreCase = true) }?.let { sched ->
            TimeUtils.calculateAssociateShiftProgress(sched.startTime, sched.endTime)
        }
    }
    val smartGlobalTimePct = if (scheduledAssociatesProgress.isNotEmpty()) {
        scheduledAssociatesProgress.average().toFloat()
    } else {
        globalTimePct
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Shift Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))

        if (!isShiftActive) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("⚠️ NO ACTIVE SHIFT. Pacing calculation disabled.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
            }
        } else {
            // GLOBAL PACING CARD
            PacingCardUI(
                title = "GLOBAL PROGRESS",
                tasks = tasks,
                timePct = smartGlobalTimePct,
                isGlobal = true,
                subtext = if (scheduledAssociatesProgress.isNotEmpty()) "Averaged across ${scheduledAssociatesProgress.size} active schedules" else "Based on MOD Shift Duration"
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text("Active MOD(s): ", style = MaterialTheme.typography.labelSmall)
                if (activeMods.isEmpty()) Text("Unassigned", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                activeMods.forEach { mod ->
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(mod.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Zone Pacing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // SMART ZONE PACING CARDS
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val archetypes = listOf("Kitchen", "POS", "Float")
                items(archetypes) { archetype ->
                    val zoneTasks = tasks.filter { it.archetype == archetype }
                    val zoneAssocs = associates.filter { it.currentArchetype == archetype }

                    // SMART ZONE LOGIC: Find the schedules of the people in THIS zone.
                    val zoneProgressList = zoneAssocs.mapNotNull { assoc ->
                        schedule.find { it.associateName.equals(assoc.name, ignoreCase = true) }?.let { sched ->
                            TimeUtils.calculateAssociateShiftProgress(sched.startTime, sched.endTime)
                        }
                    }

                    val zoneTimePct = if (zoneProgressList.isNotEmpty()) {
                        zoneProgressList.average().toFloat()
                    } else {
                        smartGlobalTimePct // Fallback if they forgot to schedule the person
                    }

                    val subtext = if (zoneProgressList.isNotEmpty()) {
                        "Tracking pacing for ${zoneAssocs.joinToString { it.name }}'s schedule"
                    } else {
                        "Using global pacing (No schedule found)"
                    }

                    PacingCardUI(title = "${archetype.uppercase()} ZONE", tasks = zoneTasks, timePct = zoneTimePct, isGlobal = false, subtext = subtext)
                }

                if (schedule.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
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
                }
            }
        }
    }
}

@Composable
fun PacingCardUI(title: String, tasks: List<com.example.supershift.data.ShiftTask>, timePct: Float, isGlobal: Boolean, subtext: String) {
    val total = tasks.size
    val completed = tasks.count { it.isCompleted }
    val taskPct = if (total > 0) completed.toFloat() / total else 1f

    // PACING ALGORITHM
    // If you have completed 50% of tasks, but only 30% of your shift is over, you are AHEAD.
    val diff = taskPct - timePct
    val (pacingText, pacingColor) = when {
        total == 0 -> "NO TASKS" to Color.Gray
        diff < -0.05f -> "BEHIND PACE" to Color(0xFFE53935) // Over 5% behind expected target
        diff in -0.05f..0.15f -> "ON TRACK" to Color(0xFF4CAF50) // Within acceptable variance
        else -> "AHEAD OF PACE" to Color(0xFF2196F3) // Crushing it
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isGlobal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (isGlobal) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(subtext, style = MaterialTheme.typography.bodySmall, color = if (isGlobal) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else Color.Gray)
                }
                Surface(color = pacingColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                    Text(pacingText, style = MaterialTheme.typography.labelSmall, color = pacingColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TASK PROGRESS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tasks: $completed/$total", style = MaterialTheme.typography.bodySmall)
                Text("${(taskPct * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { taskPct }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(8.dp))

            // TIME PROGRESS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Expected Time Target", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("${(timePct * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            LinearProgressIndicator(progress = { timePct }, modifier = Modifier.fillMaxWidth().height(4.dp), color = Color.Gray, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        }
    }
}