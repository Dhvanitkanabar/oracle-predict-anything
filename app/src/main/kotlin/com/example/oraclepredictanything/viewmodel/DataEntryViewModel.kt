package com.example.oraclepredictanything.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oraclepredictanything.OracleApp
import com.example.oraclepredictanything.data.model.DataPoint
import com.example.oraclepredictanything.data.model.Dataset
import com.example.oraclepredictanything.data.model.PredictionCategory
import com.example.oraclepredictanything.util.CsvParser
import com.example.oraclepredictanything.util.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataEntryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<OracleApp>().repository

    private val _currentDataset = MutableStateFlow<Dataset?>(null)
    val currentDataset: StateFlow<Dataset?> = _currentDataset.asStateFlow()

    private val _dataPoints = MutableStateFlow<List<DataPoint>>(emptyList())
    val dataPoints: StateFlow<List<DataPoint>> = _dataPoints.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var recentlyDeletedPoint: DataPoint? = null

    fun loadDataset(datasetId: Long) {
        viewModelScope.launch {
            _currentDataset.value = repository.getDatasetById(datasetId)
            repository.getDataPoints(datasetId).collect {
                _dataPoints.value = it
            }
        }
    }

    fun addDataPoint(value: Double, label: String = "", extraJson: String = "") {
        val dataset = _currentDataset.value ?: return
        viewModelScope.launch {
            val newPoint = DataPoint(
                datasetId = dataset.id,
                timestamp = System.currentTimeMillis(),
                value = value,
                label = label.ifBlank { "Point ${_dataPoints.value.size + 1}" },
                extraJson = extraJson
            )
            repository.insertDataPoint(newPoint)
            _snackbarMessage.value = "Added: $value"
        }
    }

    fun deleteDataPoint(point: DataPoint) {
        viewModelScope.launch {
            recentlyDeletedPoint = point
            repository.deleteDataPoint(point)
            _snackbarMessage.value = "Data point removed"
        }
    }

    fun undoDelete() {
        val point = recentlyDeletedPoint ?: return
        viewModelScope.launch {
            repository.insertDataPoint(point)
            recentlyDeletedPoint = null
            _snackbarMessage.value = "Restored data point"
        }
    }

    fun importCsv(content: String): Int {
        val dataset = _currentDataset.value ?: return 0
        val result = CsvParser.parse(content)
        if (result.rows.isEmpty()) return 0

        // Find primary numeric column
        val numCol = result.numericColIndices.firstOrNull() ?: return 0
        val pointsToInsert = mutableListOf<DataPoint>()
        val now = System.currentTimeMillis()

        for ((idx, row) in result.rows.withIndex()) {
            if (numCol < row.size) {
                val value = row[numCol].toDoubleOrNull()
                if (value != null) {
                    val label = if (result.dateColIndex != null && result.dateColIndex < row.size) {
                        row[result.dateColIndex]
                    } else {
                        "Row ${idx + 1}"
                    }
                    pointsToInsert.add(
                        DataPoint(
                            datasetId = dataset.id,
                            timestamp = now + idx * 1000L,
                            value = value,
                            label = label
                        )
                    )
                }
            }
        }

        if (pointsToInsert.isNotEmpty()) {
            viewModelScope.launch {
                repository.insertDataPoints(pointsToInsert)
                _snackbarMessage.value = "Imported ${pointsToInsert.size} points"
            }
        }
        return pointsToInsert.size
    }

    fun loadSampleData() {
        val dataset = _currentDataset.value ?: return
        val samples = when (dataset.category) {
            PredictionCategory.TRADING -> SampleData.getTradingSample(dataset.id)
            PredictionCategory.EXAMS -> SampleData.getExamSample(dataset.id)
            PredictionCategory.SALES -> SampleData.getSalesSample(dataset.id)
            PredictionCategory.WEATHER -> SampleData.getWeatherSample(dataset.id)
            else -> SampleData.getTradingSample(dataset.id)
        }

        viewModelScope.launch {
            repository.deleteDataPointsByDataset(dataset.id)
            repository.insertDataPoints(samples)
            _snackbarMessage.value = "Loaded ${samples.size} sample points for ${dataset.category.displayName}"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }
}
