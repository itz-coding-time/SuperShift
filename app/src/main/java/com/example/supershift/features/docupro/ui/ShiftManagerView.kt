package com.example.supershift.features.docupro.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.IncidentLog
import com.example.supershift.data.ScheduleEntry
import com.example.supershift.data.ShiftState
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagerView(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    val activeShift by dao.getActiveShift().collectAsState(initial = null)

    val allTasks by dao.getAllTasks().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())
    val schedule by dao.getSchedule().collectAsState(initial = emptyList())

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (activeShift?.isOpen == true) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (activeShift?.isOpen == true) "SHIFT ACTIVE" else "NO ACTIVE SHIFT",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (activeShift?.isOpen == true) MaterialTheme.colorScheme.primary else Color.Gray
                )
                if (activeShift?.isTruckNight == true) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(4.dp))
                            Text("TRUCK NIGHT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (activeShift?.isOpen == true) {
                Spacer(Modifier.height(8.dp))
                Text("${activeShift?.shiftName} Shift", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Started at: ${timeFormat.format(Date(activeShift!!.startTimeMs))}", style = MaterialTheme.typography.bodyMedium)
                Text("Ends at: ${timeFormat.format(Date(activeShift!!.endTimeMs))}", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val reportBuilder = StringBuilder()
                            reportBuilder.append("Total Tasks Completed: ${allTasks.count { it.isCompleted }} / ${allTasks.size}\n\n")

                            listOf("Kitchen", "POS", "Float", "MOD").forEach { arch ->
                                val archTasksCompleted = allTasks.count { it.archetype == arch && it.isCompleted }
                                val archTasksTotal = allTasks.count { it.archetype == arch }
                                val archAssocs = associates.filter { it.currentArchetype == arch }.joinToString { it.name }
                                val assignedString = if (archAssocs.isEmpty()) "Unmanned" else archAssocs
                                reportBuilder.append("[$arch ZONE]\nCompleted: $archTasksCompleted/$archTasksTotal\nAssigned: $assignedString\n\n")
                            }

                            val missedTasks = allTasks.filter { !it.isCompleted }
                            if (missedTasks.isNotEmpty()) {
                                reportBuilder.append("--- MISSED TASKS (FELL OFF) ---\n")
                                missedTasks.forEach { task ->
                                    reportBuilder.append("- [${task.archetype}] ${task.taskName} (${task.priority} Priority)\n")
                                }
                            }

                            dao.insertIncident(IncidentLog(associateId = -1, category = "End of Shift Report", description = reportBuilder.toString().trim(), timestampMs = System.currentTimeMillis()))
                            dao.clearShiftState()
                            dao.deleteJitTasks()
                            dao.resetStickyAndPullTasks()
                            dao.resetInventoryCounts()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("END SHIFT & LOG REPORT")
                }
            } else {
                var selectedShiftType by remember { mutableStateOf("Overnight") }
                var isTruckNight by remember { mutableStateOf(false) }

                var startTimeStr by remember { mutableStateOf("22:00") }
                var endTimeStr by remember { mutableStateOf("06:30") }

                var showStartTimePicker by remember { mutableStateOf(false) }
                var showEndTimePicker by remember { mutableStateOf(false) }
                val startTimePickerState = rememberTimePickerState(initialHour = 22, initialMinute = 0)
                val endTimePickerState = rememberTimePickerState(initialHour = 6, initialMinute = 30)

                Text("Initialize MOD shift context:", style = MaterialTheme.typography.bodyMedium)

                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf("Morning", "Afternoon", "Overnight").forEach { type ->
                        FilterChip(selected = selectedShiftType == type, onClick = { selectedShiftType = type }, label = { Text(type) }, modifier = Modifier.padding(end = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTimeStr, onValueChange = {}, readOnly = true, label = { Text("MOD Start Time") },
                        trailingIcon = { IconButton(onClick = { showStartTimePicker = true }) { Icon(Icons.Default.Schedule, contentDescription = null) } }, modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTimeStr, onValueChange = {}, readOnly = true, label = { Text("MOD End Time") },
                        trailingIcon = { IconButton(onClick = { showEndTimePicker = true }) { Icon(Icons.Default.Schedule, contentDescription = null) } }, modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = if (isTruckNight) MaterialTheme.colorScheme.primary else Color.Gray)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Logistics Protocol", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Enable Truck Night routing.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                        Switch(checked = isTruckNight, onCheckedChange = { isTruckNight = it })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            dao.clearSchedule()

                            val modAssoc = associates.find { it.currentArchetype == "MOD" }
                            val modName = modAssoc?.name ?: "MOD"

                            dao.insertScheduleEntry(ScheduleEntry(associateName = modName, startTime = startTimeStr, endTime = endTimeStr))

                            val currentDay = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())

                            associates.forEach { assoc ->
                                if (assoc.currentArchetype != "MOD" && assoc.scheduledDays.contains(currentDay, ignoreCase = true)) {
                                    dao.insertScheduleEntry(ScheduleEntry(associateName = assoc.name, startTime = assoc.defaultStartTime, endTime = assoc.defaultEndTime))
                                }
                            }

                            var startHour = startTimePickerState.hour
                            var startMin = startTimePickerState.minute
                            var endHour = endTimePickerState.hour
                            var endMin = endTimePickerState.minute

                            val now = Calendar.getInstance()
                            val startCal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, startHour); set(Calendar.MINUTE, startMin); set(Calendar.SECOND, 0) }
                            if (startCal.timeInMillis > now.timeInMillis + (12 * 3600000)) startCal.add(Calendar.DAY_OF_YEAR, -1)
                            val endCal = Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis; set(Calendar.HOUR_OF_DAY, endHour); set(Calendar.MINUTE, endMin) }
                            if (endHour <= startHour) endCal.add(Calendar.DAY_OF_YEAR, 1)

                            val shiftName = when (startHour) { in 4..10 -> "Morning"; in 11..16 -> "Afternoon"; else -> "Overnight" }

                            dao.updateShiftState(ShiftState(startTimeMs = startCal.timeInMillis, endTimeMs = endCal.timeInMillis, shiftName = shiftName, isOpen = true, isTruckNight = isTruckNight))

                            if (isTruckNight) {
                                dao.insertTask(ShiftTask(
                                    taskName = "Receive & Count Truck",
                                    archetype = "MOD",
                                    assignedTo = "MOD",
                                    priority = "High",
                                    isTruckTask = true,
                                    taskDescription = "Count cubes and log the split for Ambient zones."
                                ))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("START SHIFT ENGINE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (showStartTimePicker) {
                    AlertDialog(onDismissRequest = { showStartTimePicker = false }, text = { TimePicker(state = startTimePickerState) }, confirmButton = { TextButton(onClick = { startTimeStr = String.format(Locale.getDefault(), "%02d:%02d", startTimePickerState.hour, startTimePickerState.minute); showStartTimePicker = false }) { Text("Confirm") } })
                }
                if (showEndTimePicker) {
                    AlertDialog(onDismissRequest = { showEndTimePicker = false }, text = { TimePicker(state = endTimePickerState) }, confirmButton = { TextButton(onClick = { endTimeStr = String.format(Locale.getDefault(), "%02d:%02d", endTimePickerState.hour, endTimePickerState.minute); showEndTimePicker = false }) { Text("Confirm") } })
                }
            }
        }
    }
}