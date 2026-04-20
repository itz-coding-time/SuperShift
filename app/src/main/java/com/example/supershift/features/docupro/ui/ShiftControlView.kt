package com.example.supershift.features.docupro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supershift.data.ShiftState
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ShiftManagerView(dao: SuperShiftDao) {
    val scope = rememberCoroutineScope()
    val activeShift by dao.getActiveShift().collectAsState(initial = null)

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activeShift?.isOpen == true)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = "${activeShift?.shiftName} Shift",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Started at: ${timeFormat.format(Date(activeShift!!.startTimeMs))}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { scope.launch { dao.clearShiftState() } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("END SHIFT & CLEAR LOGS")
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
                            dao.updateShiftState(
                                ShiftState(
                                    startTimeMs = System.currentTimeMillis(),
                                    shiftName = selectedShiftType,
                                    isOpen = true
                                )
                            )
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