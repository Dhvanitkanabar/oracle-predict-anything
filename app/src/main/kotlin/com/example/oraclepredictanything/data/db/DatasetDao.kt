package com.example.oraclepredictanything.data.db

import androidx.room.*
import com.example.oraclepredictanything.data.model.Dataset
import kotlinx.coroutines.flow.Flow

@Dao
interface DatasetDao {
    @Query("SELECT * FROM datasets ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<Dataset>>

    @Query("SELECT * FROM datasets WHERE id = :id")
    suspend fun getById(id: Long): Dataset?

    @Query("SELECT * FROM datasets WHERE categoryName = :catName ORDER BY updatedAt DESC")
    fun getByCategory(catName: String): Flow<List<Dataset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dataset: Dataset): Long

    @Update
    suspend fun update(dataset: Dataset)

    @Delete
    suspend fun delete(dataset: Dataset)

    @Query("DELETE FROM datasets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
