package com.example.supershift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.supershift.ui.theme.SuperShiftTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Grab the database instance cleanly from the custom Application class.
        // NOTE: Ensure your class is named SuperShiftApp and NOT Application
        // to avoid colliding with Android's native android.app.Application class.
        val database = (application as SuperShiftApp).database
        val dao = database.superShiftDao()

        setContent {
            SuperShiftTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // This calls your newly separated SuperShiftAppNavigation.kt file!
                    SuperShiftAppNavigation(dao = dao)
                }
            }
        }
    }
}