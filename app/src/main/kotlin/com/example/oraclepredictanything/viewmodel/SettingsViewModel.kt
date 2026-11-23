package com.example.oraclepredictanything.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oraclepredictanything.OracleApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = getApplication<OracleApp>().preferences
    private val repository = getApplication<OracleApp>().repository

    val themeMode: StateFlow<String> = preferences.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "DARK"
        )

    val defaultHorizon: StateFlow<Int> = preferences.defaultHorizon
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 7
        )

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setDefaultHorizon(horizon: Int) {
        viewModelScope.launch {
            preferences.setDefaultHorizon(horizon)
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllData()
            onComplete()
        }
    }
}
