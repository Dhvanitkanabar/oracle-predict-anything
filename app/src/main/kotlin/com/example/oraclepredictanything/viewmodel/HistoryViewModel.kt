package com.example.oraclepredictanything.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oraclepredictanything.OracleApp
import com.example.oraclepredictanything.data.model.PredictionResultEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<OracleApp>().repository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    val predictions: StateFlow<List<PredictionResultEntity>> = combine(
        repository.getAllPredictionResults(),
        _searchQuery,
        _selectedFilter
    ) { all, query, filter ->
        all.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.datasetName.contains(query, ignoreCase = true) ||
                    item.modelUsed.contains(query, ignoreCase = true)
            val matchesFilter = filter == "ALL" ||
                    (filter == "TRADING" && item.overallSignal.isNotBlank()) ||
                    (filter == "HIGH_CONFIDENCE" && item.confidence >= 0.7)
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun deletePrediction(result: PredictionResultEntity) {
        viewModelScope.launch {
            repository.deletePredictionResult(result)
        }
    }
}
