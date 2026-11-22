package com.example.oraclepredictanything.engine.quality

import kotlin.math.abs
import kotlin.math.sqrt

data class QualityReport(
    val warnings: List<String>,
    val outlierIndices: List<Int>,
    val missingCount: Int,
    val duplicateCount: Int,
    val qualityScore: Double,
    val adjustedConfidence: Double
)

/**
 * Deterministic quality audits for input series:
 * - Detects sample size insufficiency
 * - Outlier detection via both IQR and Z-Score
 * - Missing values (NaN / Infinite)
 * - Stagnant duplicate runs
 */
object DataQualityChecker {

    fun check(data: List<Double>, minRequired: Int = 7): QualityReport {
        val warnings = mutableListOf<String>()
        val outliers = mutableListOf<Int>()
        var missingCount = 0
        var duplicateCount = 0

        if (data.size < minRequired) {
            warnings.add("Dataset contains only ${data.size} points. At least $minRequired points are recommended for reliable forecasting.")
        }

        // Check missing or invalid
        val cleanValues = mutableListOf<Double>()
        for ((idx, v) in data.withIndex()) {
            if (v.isNaN() || v.isInfinite()) {
                missingCount++
                warnings.add("Missing or infinite value detected at index $idx.")
            } else {
                cleanValues.add(v)
            }
        }

        // Check consecutive duplicates
        for (i in 1 until data.size) {
            if (abs(data[i] - data[i - 1]) < 1e-9) {
                duplicateCount++
            }
        }
        if (duplicateCount > data.size / 2 && data.size > 5) {
            warnings.add("Over 50% of consecutive values are identical (stagnant pattern detected).")
        }

        // Outlier detection using IQR
        if (cleanValues.size >= 4) {
            val sorted = cleanValues.sorted()
            val q1 = percentile(sorted, 0.25)
            val q3 = percentile(sorted, 0.75)
            val iqr = q3 - q1

            val lowerFence = q1 - 1.5 * iqr
            val upperFence = q3 + 1.5 * iqr

            // Also calculate z-scores
            val mean = cleanValues.average()
            val variance = cleanValues.map { (it - mean) * (it - mean) }.average()
            val std = sqrt(variance)

            for ((idx, v) in data.withIndex()) {
                if (v.isNaN() || v.isInfinite()) continue
                val isIqrOutlier = v < lowerFence || v > upperFence
                val isZOutlier = std > 1e-6 && abs(v - mean) / std > 3.0
                if (isIqrOutlier || isZOutlier) {
                    outliers.add(idx)
                }
            }

            if (outliers.isNotEmpty()) {
                warnings.add("${outliers.size} statistical outlier(s) detected. Confidence bands widened accordingly.")
            }
        }

        // Quality score from 0.0 to 1.0
        var score = 1.0
        if (data.size < minRequired) {
            score -= 0.3 * (1.0 - (data.size.toDouble() / minRequired.coerceAtLeast(1)))
        }
        score -= (missingCount * 0.1).coerceAtMost(0.3)
        score -= (outliers.size * 0.05).coerceAtMost(0.2)
        if (duplicateCount > data.size / 2) {
            score -= 0.15
        }
        score = score.coerceIn(0.25, 1.0)

        val adjustedConfidence = score

        return QualityReport(
            warnings = warnings,
            outlierIndices = outliers,
            missingCount = missingCount,
            duplicateCount = duplicateCount,
            qualityScore = score,
            adjustedConfidence = adjustedConfidence
        )
    }

    private fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val pos = p * (sorted.size - 1)
        val lower = pos.toInt()
        val frac = pos - lower
        return if (lower + 1 < sorted.size) {
            sorted[lower] + frac * (sorted[lower + 1] - sorted[lower])
        } else {
            sorted[lower]
        }
    }
}
