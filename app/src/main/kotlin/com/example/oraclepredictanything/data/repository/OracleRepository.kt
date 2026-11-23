package com.example.oraclepredictanything.data.repository

import com.example.oraclepredictanything.data.db.AppDatabase
import com.example.oraclepredictanything.data.model.DataPoint
import com.example.oraclepredictanything.data.model.Dataset
import com.example.oraclepredictanything.data.model.PredictionResultEntity
import kotlinx.coroutines.flow.Flow

class OracleRepository(private val db: AppDatabase) {

    // Datasets
    fun getAllDatasets(): Flow<List<Dataset>> = db.datasetDao().getAll()

    suspend fun getDatasetById(id: Long): Dataset? = db.datasetDao().getById(id)

    fun getDatasetsByCategory(catName: String): Flow<List<Dataset>> = db.datasetDao().getByCategory(catName)

    suspend fun insertDataset(dataset: Dataset): Long = db.datasetDao().insert(dataset)

    suspend fun updateDataset(dataset: Dataset) = db.datasetDao().update(dataset)

    suspend fun deleteDataset(dataset: Dataset) {
        db.dataPointDao().deleteByDataset(dataset.id)
        db.datasetDao().delete(dataset)
    }

    // Data Points
    fun getDataPoints(datasetId: Long): Flow<List<DataPoint>> = db.dataPointDao().getByDataset(datasetId)

    suspend fun getDataPointsList(datasetId: Long): List<DataPoint> = db.dataPointDao().getByDatasetList(datasetId)

    suspend fun getDataPointsCount(datasetId: Long): Int = db.dataPointDao().getCountByDataset(datasetId)

    suspend fun insertDataPoint(dataPoint: DataPoint): Long {
        val id = db.dataPointDao().insert(dataPoint)
        // Update dataset updatedAt timestamp
        db.datasetDao().getById(dataPoint.datasetId)?.let {
            db.datasetDao().update(it.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun insertDataPoints(points: List<DataPoint>) {
        if (points.isEmpty()) return
        db.dataPointDao().insertAll(points)
        val datasetId = points.first().datasetId
        db.datasetDao().getById(datasetId)?.let {
            db.datasetDao().update(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateDataPoint(dataPoint: DataPoint) = db.dataPointDao().update(dataPoint)

    suspend fun deleteDataPoint(dataPoint: DataPoint) = db.dataPointDao().delete(dataPoint)

    suspend fun deleteDataPointsByDataset(datasetId: Long) = db.dataPointDao().deleteByDataset(datasetId)

    // Prediction Results
    fun getAllPredictionResults(): Flow<List<PredictionResultEntity>> = db.predictionResultDao().getAll()

    fun getPredictionResultsByDataset(datasetId: Long): Flow<List<PredictionResultEntity>> =
        db.predictionResultDao().getByDataset(datasetId)

    suspend fun getLatestPredictionResult(datasetId: Long): PredictionResultEntity? =
        db.predictionResultDao().getLatestByDataset(datasetId)

    suspend fun getPredictionResultById(id: Long): PredictionResultEntity? =
        db.predictionResultDao().getById(id)

    suspend fun savePredictionResult(result: PredictionResultEntity): Long =
        db.predictionResultDao().insert(result)

    suspend fun deletePredictionResult(result: PredictionResultEntity) =
        db.predictionResultDao().delete(result)

    suspend fun clearAllData() {
        db.clearAllTables()
    }
}
