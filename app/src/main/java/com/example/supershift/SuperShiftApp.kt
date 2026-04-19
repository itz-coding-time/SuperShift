package com.example.supershift

import android.app.Application
import com.example.supershift.data.SuperShiftDatabase

class SuperShiftApp : Application() {

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { SuperShiftDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        // We can initialize other global variables or analytics here later
    }
}