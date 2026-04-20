package com.example.supershift.features.hud.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.data.ShiftTask
import kotlinx.coroutines.launch

@Composable
fun ActiveHudScreen(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    // The DB will auto-refresh this list the second a task is clicked!
    val pendingTasks by dao.getPendingTasks().collectAsState(initial = emptyList())

    // Group the tasks by your CSV file categories (RTE, Prep, etc.)
    val groupedTasks = pendingTasks.groupBy { it.category }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(
            text = "Active Deployment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (pendingTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("All floors clear. Excellent shift!", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                groupedTasks.forEach { (category, tasks) ->
                    item {
                        Text(
                            text = category.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                        Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(tasks) { task ->
                        TaskCard(task = task, onComplete = {
                            scope.launch {
                                // Mark the task as completed in the DB. It instantly vanishes from the UI.
                                dao.updateTask(task.copy(isCompleted = true))
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: ShiftTask, onComplete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Amount: ${task.pullAmount}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${task.basePoints} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onComplete) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Complete", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DONE")
                }
            }
        }
    }
}