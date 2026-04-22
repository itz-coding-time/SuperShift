package com.example.supershift.utils

import com.example.supershift.data.Associate
import com.example.supershift.data.InventoryItem
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.TableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object CsvExporter {
    suspend fun exportTaskChecklist(outputStream: OutputStream, tasks: List<ShiftTask>) {
        withContext(Dispatchers.IO) {
            try {
                outputStream.bufferedWriter().use { writer ->
                    writer.write("TaskName,Archetype,Priority,IsSticky\n")
                    tasks.forEach { task ->
                        val name = "\"${task.taskName.replace("\"", "\"\"")}\""
                        val arch = "\"${task.archetype}\""
                        val prio = "\"${task.priority}\""
                        val sticky = if (task.isSticky) "1" else "0"
                        writer.write("$name,$arch,$prio,$sticky\n")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun exportInventory(outputStream: OutputStream, items: List<InventoryItem>) {
        withContext(Dispatchers.IO) {
            try {
                outputStream.bufferedWriter().use { writer ->
                    writer.write("ItemName,BuildTo\n")
                    items.forEach { item ->
                        val name = "\"${item.itemName.replace("\"", "\"\"")}\""
                        val bt = "\"${item.buildTo}\""
                        writer.write("$name,$bt\n")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun exportRoster(outputStream: OutputStream, associates: List<Associate>) {
        withContext(Dispatchers.IO) {
            try {
                outputStream.bufferedWriter().use { writer ->
                    // Standard Headers for the Roster
                    writer.write("Name,Role,Archetype,ScheduledDays,DefaultStart,DefaultEnd,PinCode\n")
                    associates.forEach { assoc ->
                        val name = "\"${assoc.name.replace("\"", "\"\"")}\""
                        val role = "\"${assoc.role}\""
                        val archetype = "\"${assoc.currentArchetype}\""
                        val days = "\"${assoc.scheduledDays}\""
                        val start = "\"${assoc.defaultStartTime}\""
                        val end = "\"${assoc.defaultEndTime}\""
                        val pin = "\"${assoc.pinCode ?: ""}\""
                        writer.write("$name,$role,$archetype,$days,$start,$end,$pin\n")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun exportTableFlips(outputStream: OutputStream, items: List<TableItem>) {
        withContext(Dispatchers.IO) {
            try {
                outputStream.bufferedWriter().use { writer ->
                    writer.write("ItemName,Station,IsInitialed,WasteAmount\n")
                    items.forEach { item ->
                        val name = "\"${item.itemName.replace("\"", "\"\"")}\""
                        val station = "\"${item.station}\""
                        val initialed = if (item.isInitialed) "1" else "0"
                        val waste = "\"${item.wasteAmount ?: ""}\""
                        writer.write("$name,$station,$initialed,$waste\n")
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}