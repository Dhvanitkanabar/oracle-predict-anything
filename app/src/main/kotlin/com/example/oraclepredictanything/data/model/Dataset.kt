package com.example.oraclepredictanything.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved dataset collection belonging to a prediction category.
 */
@Entity(tableName = "datasets")
data class Dataset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val fieldNamesJson: String = ""
) {
    val category: PredictionCategory
        get() = PredictionCategory.fromName(categoryName)
}
