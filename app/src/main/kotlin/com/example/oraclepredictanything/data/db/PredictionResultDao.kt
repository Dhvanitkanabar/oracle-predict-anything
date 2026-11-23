package com.example.oraclepredictanything.data.db

import androidx.room.*
import com.example.oraclepredictanything.data.model.PredictionResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionResultDao {
    @Query("SELECT * FROM prediction_results ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PredictionResultEntity>>

    @Query("SELECT * FROM prediction_results WHERE datasetId = :datasetId ORDER BY createdAt DESC")
    fun getByDataset(datasetId: Long): Flow<List<PredictionResultEntity>>

    @Query("SELECT * FROM prediction_results WHERE datasetId = :datasetId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestByDataset(datasetId: Long): PredictionResultEntity?

    @Query("SELECT * FROM prediction_results WHERE id = :id")
    suspend fun getById(id: Long): PredictionResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: PredictionResultEntity): Long

    @Delete
    suspend fun delete(result: PredictionResultEntity)

    @Query("DELETE FROM prediction_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM prediction_results")
    suspend fun deleteAll()
}
