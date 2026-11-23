package com.example.oraclepredictanything

import android.app.Application
import com.example.oraclepredictanything.data.db.AppDatabase
import com.example.oraclepredictanything.data.preferences.AppPreferences
import com.example.oraclepredictanything.data.repository.OracleRepository

class OracleApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val repository: OracleRepository by lazy {
        OracleRepository(database)
    }

    val preferences: AppPreferences by lazy {
        AppPreferences(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
