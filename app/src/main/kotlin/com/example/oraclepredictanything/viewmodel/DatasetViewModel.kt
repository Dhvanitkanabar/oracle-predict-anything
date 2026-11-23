package com.example.oraclepredictanything.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oraclepredictanything.OracleApp
import com.example.oraclepredictanything.data.model.Dataset
import com.example.oraclepredictanything.data.model.PredictionCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DatasetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<OracleApp>().repository

    val datasets: StateFlow<List<Dataset>> = repository.getAllDatasets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createDataset(name: String, category: PredictionCategory, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newDataset = Dataset(
                name = name.trim(),
                categoryName = category.name,
                fieldNamesJson = category.defaultFields.joinToString(",")
            )
            val id = repository.insertDataset(newDataset)
            onCreated(id)
        }
    }

    fun deleteDataset(dataset: Dataset) {
        viewModelScope.launch {
            repository.deleteDataset(dataset)
        }
    }
}
