package com.example.oraclepredictanything.engine

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Mathematical evaluation utilities for error metrics, residual variance, and confidence intervals.
 */
object BacktestUtils {

    fun computeMAE(actual: List<Double>, predicted: List<Double>): Double {
        if (actual.isEmpty() || predicted.isEmpty()) return 0.0
        val count = minOf(actual.size, predicted.size)
        if (count == 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) {
            sum += abs(actual[i] - predicted[i])
        }
        return sum / count
    }

    fun computeRMSE(actual: List<Double>, predicted: List<Double>): Double {
        if (actual.isEmpty() || predicted.isEmpty()) return 0.0
        val count = minOf(actual.size, predicted.size)
        if (count == 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) {
            val diff = actual[i] - predicted[i]
            sum += diff * diff
        }
        return sqrt(sum / count)
    }

    fun computeMAPE(actual: List<Double>, predicted: List<Double>): Double {
        if (actual.isEmpty() || predicted.isEmpty()) return 0.0
        val count = minOf(actual.size, predicted.size)
        if (count == 0) return 0.0
        var sum = 0.0
        var validCount = 0
        for (i in 0 until count) {
            val a = actual[i]
            if (abs(a) > 1e-9) {
                sum += abs((a - predicted[i]) / a) * 100.0
                validCount++
            }
        }
        return if (validCount > 0) sum / validCount else 0.0
    }

    fun accuracyLabel(mape: Double): String {
        return when {
            mape < 5.0 -> "Excellent (<5% error)"
            mape < 10.0 -> "Good (5-10% error)"
            mape < 20.0 -> "Moderate (10-20% error)"
            else -> "Low (>20% error)"
        }
    }

    fun computeResidualStdDev(actual: List<Double>, fitted: List<Double>): Double {
        val count = minOf(actual.size, fitted.size)
        if (count <= 1) return 1.0
        var sumSq = 0.0
        for (i in 0 until count) {
            val res = actual[i] - fitted[i]
            sumSq += res * res
        }
        val variance = sumSq / (count - 1).coerceAtLeast(1)
        val s = sqrt(variance)
        return if (s.isNaN() || s < 1e-6) 1.0 else s
    }

    /**
     * Compute 80% (z=1.28) and 95% (z=1.96) normal distribution confidence intervals expanding over horizon steps.
     */
    fun computeBands(
        predictions: List<Double>,
        stdDev: Double
    ): ConfidenceBands {
        val lower80 = mutableListOf<Double>()
        val upper80 = mutableListOf<Double>()
        val lower95 = mutableListOf<Double>()
        val upper95 = mutableListOf<Double>()

        for ((step, p) in predictions.withIndex()) {
            // Error expands with forecast horizon sqrt(step + 1)
            val expansion = sqrt((step + 1).toDouble())
            val se = stdDev * expansion
            val z80 = 1.28155 * se
            val z95 = 1.95996 * se

            lower80.add(p - z80)
            upper80.add(p + z80)
            lower95.add(p - z95)
            upper95.add(p + z95)
        }

        return ConfidenceBands(lower80, upper80, lower95, upper95)
    }

    data class ConfidenceBands(
        val lower80: List<Double>,
        val upper80: List<Double>,
        val lower95: List<Double>,
        val upperBound95: List<Double>
    )
}
