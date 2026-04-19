package com.example.supershift.features.docupro

import android.content.Context
import com.example.supershift.data.Associate
import com.example.supershift.data.IncidentLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementGenerator {

    /**
     * Reads the statement_template.txt from assets and injects the Room Database entity data.
     */
    fun generateStatement(
        context: Context,
        associate: Associate,
        incident: IncidentLog,
        managerName: String = "Shift Supervisor" // Can be hooked up to Settings later
    ): String {

        // 1. Read the raw text template from the assets folder
        val template = try {
            context.assets.open("statement_template.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return "Error: Could not load statement_template.txt from assets. ${e.localizedMessage}"
        }

        // 2. Format the Unix timestamp from the database into human-readable text
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(incident.timestampMs))

        // 3. Inject the Room entity data into the template placeholders
        // (Make sure these match the bracketed tags in your actual .txt file)
        return template
            .replace("[ASSOCIATE_NAME]", associate.name)
            .replace("[ASSOCIATE_ROLE]", associate.role)
            .replace("[DATE_OF_INCIDENT]", dateString)
            .replace("[INCIDENT_TYPE]", incident.category)
            .replace("[INCIDENT_DESCRIPTION]", incident.description)
            .replace("[MANAGER_NAME]", managerName)
    }
}