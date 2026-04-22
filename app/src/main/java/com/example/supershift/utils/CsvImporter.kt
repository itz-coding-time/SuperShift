package com.example.supershift.utils

import com.example.supershift.data.Associate
import com.example.supershift.data.InventoryItem
import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import com.example.supershift.data.TableItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.InputStream

object CsvImporter {

    suspend fun importCsvStream(inputStream: InputStream, category: String, dao: SuperShiftDao) {
        withContext(Dispatchers.IO) {
            val itemsToInsert = mutableListOf<InventoryItem>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                        if (tokens.size >= 2) {
                            itemsToInsert.add(InventoryItem(itemName = tokens[0].trim().replace("\"", ""), buildTo = tokens[1].trim(), category = category))
                        }
                    }
                }
                if (itemsToInsert.isNotEmpty()) {
                    dao.clearInventoryByCategory(category)
                    dao.insertInventoryItems(itemsToInsert)

                    if (dao.getTaskByPullCategory(category) == null) {
                        val defaultArchetype = when (category) {
                            "RTE", "Bakery" -> "POS"
                            "Prep", "Bread" -> "Kitchen"
                            else -> "Float"
                        }
                        dao.insertTask(ShiftTask(taskName = "$category Pull List", archetype = defaultArchetype, isPullTask = true, pullCategory = category, basePoints = 50))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun importTaskChecklist(inputStream: InputStream, dao: SuperShiftDao) {
        withContext(Dispatchers.IO) {
            val tasksToInsert = mutableListOf<ShiftTask>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().replace("\"", "") }

                        if (tokens.size >= 2) {
                            val taskName = tokens[0]
                            val archetype = tokens[1]

                            val rawPriority = tokens.getOrNull(2) ?: "2"
                            val safePriority = when (rawPriority) {
                                "1", "High", "high" -> "High"
                                "3", "Low", "low" -> "Low"
                                else -> "Normal"
                            }

                            val rawSticky = tokens.getOrNull(3) ?: "0"
                            val isSticky = rawSticky == "1" || rawSticky.equals("true", ignoreCase = true)

                            tasksToInsert.add(
                                ShiftTask(
                                    taskName = taskName,
                                    archetype = archetype,
                                    priority = safePriority,
                                    isSticky = isSticky,
                                    basePoints = 10,
                                    isPullTask = false
                                )
                            )
                        }
                    }
                }
                if (tasksToInsert.isNotEmpty()) dao.insertTasks(tasksToInsert)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun importTableFlipStream(inputStream: InputStream, station: String, dao: SuperShiftDao) {
        withContext(Dispatchers.IO) {
            val itemsToInsert = mutableListOf<TableItem>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val itemName = line.trim().replace("\"", "")
                        if (itemName.isNotBlank()) {
                            itemsToInsert.add(TableItem(itemName = itemName, station = station))
                        }
                    }
                }
                if (itemsToInsert.isNotEmpty()) {
                    dao.clearTableItemsByStation(station)
                    dao.insertTableItems(itemsToInsert)

                    val taskName = "Midnight Flip: $station"
                    val tasks = dao.getAllTasks().firstOrNull() ?: emptyList()
                    if (tasks.none { it.taskName == taskName }) {
                        dao.insertTask(ShiftTask(taskName = taskName, archetype = "Kitchen", priority = "High", isSticky = true))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    suspend fun importRoster(inputStream: InputStream, dao: SuperShiftDao) {
        withContext(Dispatchers.IO) {
            val associatesToInsert = mutableListOf<Associate>()
            try {
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().replace("\"", "") }
                        if (tokens.size >= 3) {
                            associatesToInsert.add(
                                Associate(
                                    name = tokens[0],
                                    role = tokens[1],
                                    currentArchetype = tokens[2],
                                    scheduledDays = tokens.getOrNull(3) ?: "",
                                    defaultStartTime = tokens.getOrNull(4) ?: "22:00",
                                    defaultEndTime = tokens.getOrNull(5) ?: "06:30",
                                    pinCode = tokens.getOrNull(6).takeIf { !it.isNullOrBlank() }
                                )
                            )
                        }
                    }
                }

                // Smart "Upsert" - Updates existing names, inserts new ones
                if (associatesToInsert.isNotEmpty()) {
                    val existingAssocs = dao.getAllAssociates().firstOrNull() ?: emptyList()
                    associatesToInsert.forEach { newAssoc ->
                        val existingMatch = existingAssocs.find { it.name.equals(newAssoc.name, ignoreCase = true) }
                        if (existingMatch != null) {
                            dao.updateAssociate(newAssoc.copy(id = existingMatch.id)) // Retain their DB ID
                        } else {
                            dao.insertAssociate(newAssoc)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}