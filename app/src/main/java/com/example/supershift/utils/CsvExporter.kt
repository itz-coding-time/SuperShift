package com.example.supershift.utils

import com.example.supershift.data.InventoryItem
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.TableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object CsvExporter {

    suspend fun exportInventory(outputStream: OutputStream, items: List<InventoryItem>) {
        withContext(Dispatchers.IO) {
            outputStream.bufferedWriter().use { writer ->
                writer.write("Item Name,Pull Amount\n")
                items.forEach { item ->
                    writer.write("\"${item.itemName}\",\"${item.buildTo}\"\n")
                }
            }
        }
    }

    suspend fun exportTableFlips(outputStream: OutputStream, items: List<TableItem>) {
        withContext(Dispatchers.IO) {
            outputStream.bufferedWriter().use { writer ->
                writer.write("Item Name\n")
                items.forEach { item ->
                    writer.write("\"${item.itemName}\"\n")
                }
            }
        }
    }

    suspend fun exportTaskChecklist(outputStream: OutputStream, tasks: List<ShiftTask>) {
        withContext(Dispatchers.IO) {
            outputStream.bufferedWriter().use { writer ->
                // Write the CSV Header
                writer.write("TaskName,TaskArchetype,TaskPriority,IsSticky\n")

                // Filter out the auto-generated tasks
                val exportableTasks = tasks.filter { !it.isPullTask && !it.taskName.startsWith("Midnight Flip:") }

                exportableTasks.forEach { task ->
                    val stickyVal = if (task.isSticky) "1" else "0"

                    // NEW: Convert text to Numeric Schema (1=High, 2=Normal, 3=Low)
                    val priorityNum = when (task.priority) {
                        "High" -> "1"
                        "Normal" -> "2"
                        "Low" -> "3"
                        else -> "2"
                    }

                    writer.write("\"${task.taskName}\",\"${task.archetype}\",\"$priorityNum\",\"$stickyVal\"\n")
                }
            }
        }
    }
}