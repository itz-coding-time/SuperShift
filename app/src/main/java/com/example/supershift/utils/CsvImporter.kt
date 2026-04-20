package com.example.supershift.utils

import com.example.supershift.data.InventoryItem
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object CsvImporter {

    suspend fun importCsvStream(inputStream: InputStream, category: String, dao: SuperShiftDao) {
        withContext(Dispatchers.IO) {
            val itemsToInsert = mutableListOf<InventoryItem>()

            try {
                inputStream.bufferedReader().useLines { lines ->
                    // Header: Item Name, Pull Amount, Need
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())

                        if (tokens.size >= 2) {
                            itemsToInsert.add(
                                InventoryItem(
                                    itemName = tokens[0].trim().replace("\"", ""),
                                    buildTo = tokens[1].trim(), // Keeps UOM like "12 BT"
                                    category = category,
                                    amountHave = null,
                                    amountNeeded = null
                                )
                            )
                        }
                    }
                }

                if (itemsToInsert.isNotEmpty()) {
                    // 1. Clear old inventory for this pull
                    dao.clearInventoryByCategory(category)
                    // 2. Insert new list
                    dao.insertInventoryItems(itemsToInsert)

                    // 3. Ensure the Core Pull Task exists in the Task List!
                    if (dao.getTaskByPullCategory(category) == null) {
                        // Default Kitchen for Prep/Bakery/Bread, Float for RTE (Can adjust later)
                        val defaultArchetype = if (category == "RTE") "Float" else "Kitchen"
                        dao.insertTask(
                            ShiftTask(
                                taskName = "$category Pull",
                                archetype = defaultArchetype,
                                isPullTask = true,
                                pullCategory = category,
                                basePoints = 50 // Pulls are worth major XP
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Parses a generic Task Checklist and appends it to the Shift tasks.
     * Expected CSV Header: Task Name, Zone, XP
     */
    suspend fun importTaskChecklist(inputStream: InputStream, dao: SuperShiftDao) {
        withContext(Dispatchers.IO) {
            val tasksToInsert = mutableListOf<ShiftTask>()

            try {
                inputStream.bufferedReader().useLines { lines ->
                    // Drop header: Task Name, Zone, XP
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())

                        if (tokens.size >= 2) {
                            tasksToInsert.add(
                                ShiftTask(
                                    taskName = tokens[0].trim().replace("\"", ""),
                                    archetype = tokens[1].trim(), // e.g., "Kitchen", "POS", "Float"
                                    basePoints = tokens.getOrNull(2)?.trim()?.toIntOrNull() ?: 10,
                                    isPullTask = false
                                )
                            )
                        }
                    }
                }

                if (tasksToInsert.isNotEmpty()) {
                    // We just append these.
                    tasksToInsert.forEach { dao.insertTask(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}