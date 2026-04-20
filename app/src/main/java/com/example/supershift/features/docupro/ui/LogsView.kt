
package com.example.supershift.features.docupro.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsView(dao: SuperShiftDao) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val incidents by dao.getAllIncidents().collectAsState(initial = emptyList())
    val associates by dao.getAllAssociates().collectAsState(initial = emptyList())

    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text(
            text = "Black Box Logs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (incidents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No incidents in the Black Box.", fontStyle = FontStyle.Italic)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(incidents) { incident ->
                    // Check if it's our automated System Shift Report
                    val isSystemReport = incident.associateId == -1

                    val associate = associates.find { it.id == incident.associateId }
                    val associateName = if (isSystemReport) "SuperShift Engine" else (associate?.name ?: "Unknown Associate")
                    val associateRole = if (isSystemReport) "Automated Summary" else (associate?.role ?: "Unknown Role")

                    var expanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemReport) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateFormat.format(Date(incident.timestampMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isSystemReport) Icons.Default.Edit else Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isSystemReport) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$associateName ($associateRole)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand/Collapse"
                                )
                            }

                            // Expanded Content
                            AnimatedVisibility(visible = expanded) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = if (isSystemReport) "Shift Data:" else "Narrative:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSystemReport) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = incident.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(incident.description))
                                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text("COPY LOG", style = MaterialTheme.typography.labelSmall)
                                        }

                                        val isSevere = incident.category.contains("Dismissal", ignoreCase = true) || incident.category.contains("Violation", ignoreCase = true)

                                        FilterChip(
                                            selected = true,
                                            onClick = { },
                                            label = { Text(incident.category, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = if (isSevere) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = if (isSevere) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
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
}