package com.example.oraclepredictanything.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Saved historical prediction result snapshot with complete audit trail.
 */
@Entity(
    tableName = "prediction_results",
    foreignKeys = [
        ForeignKey(
            entity = Dataset::class,
            parentColumns = ["id"],
            childColumns = ["datasetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["datasetId"])]
)
data class PredictionResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val datasetId: Long,
    val datasetName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val forecastJson: String,       // Serialized forecast predictions and bands
    val modelUsed: String,          // Best or ensemble model name
    val mae: Double,
    val rmse: Double,
    val mape: Double,
    val confidence: Double,         // 0.0 to 1.0
    val explanationText: String,
    val overallSignal: String = ""  // For trading datasets: "BULLISH", "BEARISH", "NEUTRAL"
)
