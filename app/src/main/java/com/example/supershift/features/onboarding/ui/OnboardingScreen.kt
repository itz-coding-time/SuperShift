package com.example.supershift.features.onboarding.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supershift.data.Associate
import com.example.supershift.data.ScheduleEntry
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(dao: SuperShiftDao, onComplete: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("supershift_prefs", Context.MODE_PRIVATE)

    var step by remember { mutableIntStateOf(0) }
    var modName by remember { mutableStateOf("") }
    var modPin by remember { mutableStateOf("") }
    var selectedShift by remember { mutableStateOf("Overnight") }

    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = { slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut() },
                label = "Onboarding"
            ) { targetStep ->
                when (targetStep) {
                    0 -> {
                        // STEP 1: WELCOME
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(24.dp))
                            Text("SuperShift", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("Your shift, made easier.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(32.dp))
                            Text("An Android Operations Engine\nDesigned by CaseSmarts LLC.", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(48.dp))
                            Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) { Text("Begin Setup") }
                        }
                    }
                    1 -> {
                        // STEP 2: MOD SETUP
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Command Authentication", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("Who is the MOD initializing this database?", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(value = modName, onValueChange = { modName = it }, label = { Text("Enter MOD Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { if (modName.isNotBlank()) step++ }, modifier = Modifier.fillMaxWidth(), enabled = modName.isNotBlank()) { Text("Next") }
                        }
                    }
                    2 -> {
                        // STEP 3: SECURITY (NEW)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Gateway Security", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("Set a custom PIN to lock the Manager Settings.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            Text("If you ever forget this, the master fallback is always '1234'.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = modPin,
                                onValueChange = { if (it.length <= 8) modPin = it },
                                label = { Text("Enter 4-Digit PIN") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (modPin.isBlank()) "Skip (Use 1234)" else "Set PIN & Continue")
                            }
                        }
                    }
                    3 -> {
                        // STEP 4: SHIFT TEMPLATES
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("Shift Architecture", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("What time frame does your shift normally fall into?", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))

                            val shifts = listOf(
                                "Morning" to "Starts 06:00 - 08:00\n(+10.5 hrs end time)",
                                "Afternoon" to "Starts 12:00 - 16:00\n(+10.5 hrs end time)",
                                "Overnight" to "Starts 20:00 - 22:00\n(+10.5 hrs end time)"
                            )

                            shifts.forEach { (title, desc) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (selectedShift == title) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                                    onClick = { selectedShift = title }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (selectedShift == title) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                        Text(desc, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { step++ }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
                        }
                    }
                    4 -> {
                        // STEP 5: FINISH
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF4CAF50))
                            Spacer(Modifier.height(24.dp))
                            Text("Database Initialized", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("Enjoy making your shifts smarter.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(48.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        val finalPin = if (modPin.isBlank()) "1234" else modPin
                                        // Save PIN to Preferences
                                        prefs.edit().putString("manager_pin", finalPin).apply()

                                        // 1. Create the MOD
                                        dao.insertAssociate(Associate(name = modName, role = "Manager", currentArchetype = "MOD", pinCode = finalPin))

                                        // 2. Add their default 10.5 hr schedule entry
                                        val (start, end) = when (selectedShift) {
                                            "Morning" -> "06:00" to "16:30"
                                            "Afternoon" -> "14:00" to "00:30"
                                            else -> "22:00" to "08:30"
                                        }
                                        dao.insertScheduleEntry(ScheduleEntry(associateName = modName, startTime = start, endTime = end))

                                        onComplete()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Launch SuperShift") }
                        }
                    }
                }
            }
        }
    }
}