package com.example.oraclepredictanything.data.db

import androidx.room.*
import com.example.oraclepredictanything.data.model.DataPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface DataPointDao {
    @Query("SELECT * FROM data_points WHERE datasetId = :datasetId ORDER BY timestamp ASC")
    fun getByDataset(datasetId: Long): Flow<List<DataPoint>>

    @Query("SELECT * FROM data_points WHERE datasetId = :datasetId ORDER BY timestamp ASC")
    suspend fun getByDatasetList(datasetId: Long): List<DataPoint>

    @Query("SELECT COUNT(*) FROM data_points WHERE datasetId = :datasetId")
    suspend fun getCountByDataset(datasetId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: DataPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<DataPoint>)

    @Update
    suspend fun update(point: DataPoint)

    @Delete
    suspend fun delete(point: DataPoint)

    @Query("DELETE FROM data_points WHERE datasetId = :datasetId")
    suspend fun deleteByDataset(datasetId: Long)

    @Query("DELETE FROM data_points WHERE id = :id")
    suspend fun deleteById(id: Long)
}
