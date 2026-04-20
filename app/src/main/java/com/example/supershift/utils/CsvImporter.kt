package com.example.supershift.utils

import com.example.supershift.data.ShiftTask
import com.example.supershift.data.SuperShiftDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object CsvImporter {

    /**
     * Parses a CSV stream and updates the database for a specific category.
     */
    suspend fun importCsvStream(inputStream: InputStream, category: String, dao: SuperShiftDao) {
        withContext(Dispatchers.IO) {
            val tasksToInsert = mutableListOf<ShiftTask>()

            try {
                inputStream.bufferedReader().useLines { lines ->
                    // Skip header: Item Name, Pull Amount, Need
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())

                        if (tokens.size >= 2) {
                            tasksToInsert.add(
                                ShiftTask(
                                    itemName = tokens[0].trim().replace("\"", ""),
                                    pullAmount = tokens[1].trim(),
                                    category = category,
                                    basePoints = 10
                                )
                            )
                        }
                    }
                }

                if (tasksToInsert.isNotEmpty()) {
                    // 1. Clear the old pending list for this specific category
                    dao.deletePendingTasksByCategory(category)
                    // 2. Inject the fresh data
                    dao.insertTasks(tasksToInsert)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}