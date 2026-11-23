package com.example.oraclepredictanything.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an individual data point in a time series or categorical dataset.
 */
@Entity(
    tableName = "data_points",
    foreignKeys = [
        ForeignKey(
            entity = Dataset::class,
            parentColumns = ["id"],
            childColumns = ["datasetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["datasetId", "timestamp"])]
)
data class DataPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val datasetId: Long,
    val timestamp: Long,
    val value: Double,
    val label: String = "",       // Optional label e.g., "2024", "Jan", "Topic Name"
    val extraJson: String = ""    // For multi-column extra fields (OHLCV, etc.)
)
