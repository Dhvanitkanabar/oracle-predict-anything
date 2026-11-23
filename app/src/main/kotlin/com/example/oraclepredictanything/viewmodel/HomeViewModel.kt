package com.example.oraclepredictanything.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oraclepredictanything.OracleApp
import com.example.oraclepredictanything.data.model.Dataset
import com.example.oraclepredictanything.data.model.PredictionResultEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<OracleApp>().repository

    val recentDatasets: StateFlow<List<Dataset>> = repository.getAllDatasets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentPredictions: StateFlow<List<PredictionResultEntity>> = repository.getAllPredictionResults()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
