package com.example.supershift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.supershift.ui.theme.SuperShiftTheme

// Note: You will need to import your UI screens here once you copy them over from the zip files.
// import com.example.supershift.features.hud.ActiveHudScreen
// import com.example.supershift.features.deployment.DeploymentScreen
// import com.example.supershift.ui.HomeDashboard
// import com.example.supershift.ui.LogsView
// import com.example.supershift.ui.StatementsList
// import com.example.supershift.ui.SettingsView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Grab the database instance from the Application class
        val database = (application as SuperShiftApp).database
        val dao = database.superShiftDao()

        setContent {
            SuperShiftTheme {
                SuperShiftAppNavigation(dao = dao)
            }
        }
    }
}
