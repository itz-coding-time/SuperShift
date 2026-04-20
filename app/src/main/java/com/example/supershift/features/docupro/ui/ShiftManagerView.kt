package com.example.supershift.features.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.IncidentLog
import com.example.supershift.data.ShiftState
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagerView(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    val activeShift by dao.getActiveShift().collectAsState(initial = null)

    val allTasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (activeShift?.isOpen == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (activeShift?.isOpen == true) "SHIFT ACTIVE" else "NO ACTIVE SHIFT",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = if (activeShift?.isOpen == true) MaterialTheme.colorScheme.primary else Color.Gray
            )

            if (activeShift?.isOpen == true) {
                Text("${activeShift?.shiftName} Shift", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Started at: ${timeFormat.format(Date(activeShift!!.startTimeMs))}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            // 1. GENERATE THE END OF SHIFT REPORT
                            val reportBuilder = StringBuilder()
                            reportBuilder.append("Total Tasks Completed: ${allTasks.count { it.isCompleted }} / ${allTasks.size}\n\n")

                            listOf("Kitchen", "POS", "Float", "MOD").forEach { arch ->
                                val archTasksCompleted = allTasks.count { it.archetype == arch && it.isCompleted }
                                val archTasksTotal = allTasks.count { it.archetype == arch }
                                val archAssocs = associates.filter { it.currentArchetype == arch }.joinToString { it.name }
                                val assignedString = if (archAssocs.isEmpty()) "Unmanned" else archAssocs
                                reportBuilder.append("[$arch ZONE]\nCompleted: $archTasksCompleted/$archTasksTotal\nAssigned: $assignedString\n\n")
                            }

                            // --- LOG MISSED TASKS BEFORE THEY FALL OFF ---
                            val missedTasks = allTasks.filter { !it.isCompleted }
                            if (missedTasks.isNotEmpty()) {
                                reportBuilder.append("--- MISSED TASKS (FELL OFF) ---\n")
                                missedTasks.forEach { task ->
                                    reportBuilder.append("- [${task.archetype}] ${task.taskName} (${task.priority} Priority)\n")
                                }
                            }

                            dao.insertIncident(
                                IncidentLog(
                                    associateId = -1,
                                    category = "End of Shift Report",
                                    description = reportBuilder.toString().trim(),
                                    timestampMs = System.currentTimeMillis()
                                )
                            )

                            // 2. SMART WIPE / RESET
                            dao.clearShiftState()
                            dao.deleteJitTasks()
                            dao.resetStickyAndPullTasks()
                            dao.resetInventoryCounts()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("END SHIFT & LOG REPORT")
                }
            } else {
                var selectedShiftType by remember { mutableStateOf("Overnight") }

                Text("Start a new shift context:", style = MaterialTheme.typography.bodyMedium)

                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf("Morning", "Afternoon", "Overnight").forEach { type ->
                        FilterChip(
                            selected = selectedShiftType == type,
                            onClick = { selectedShiftType = type },
                            label = { Text(type) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            dao.updateShiftState(ShiftState(startTimeMs = System.currentTimeMillis(), shiftName = selectedShiftType, isOpen = true))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("START ${selectedShiftType.uppercase()} SHIFT")
                }
            }
        }
    }
}