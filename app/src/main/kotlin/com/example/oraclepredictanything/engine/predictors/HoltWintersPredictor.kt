package com.example.oraclepredictanything.engine.predictors

import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.BacktestUtils
import com.example.oraclepredictanything.engine.PredictionOutput
import com.example.oraclepredictanything.engine.Predictor

/**
 * Holt-Winters Additive Triple Exponential Smoothing.
 * Automatically checks for seasonal periodicity (e.g., 4, 7, 12 periods).
 * Falls back to Holt's Linear Trend if sample length is less than two full cycles.
 */
class HoltWintersPredictor : Predictor {
    override val name: String = "Holt-Winters Seasonal"

    override fun predict(data: List<Double>, horizon: Int): PredictionOutput {
        val period = detectPeriod(data)
        if (period == null || data.size < 2 * period) {
            // Fallback to Holt's linear trend
            return HoltPredictor().predict(data, horizon).copy(modelName = name)
        }

        val alpha = 0.2
        val beta = 0.1
        val gamma = 0.2

        // Initialize level and trend across first two cycles
        var level = data.subList(0, period).average()
        val nextCycleAvg = data.subList(period, 2 * period).average()
        var trend = (nextCycleAvg - level) / period

        // Initial seasonal indices
        val seasonal = DoubleArray(period)
        for (i in 0 until period) {
            seasonal[i] = data[i] - level
        }

        val fitted = mutableListOf<Double>()
        for (t in 0 until data.size) {
            val sIdx = t % period
            val yHat = level + trend + seasonal[sIdx]
            fitted.add(yHat)

            val actual = data[t]
            val lastLevel = level
            level = alpha * (actual - seasonal[sIdx]) + (1 - alpha) * (level + trend)
            trend = beta * (level - lastLevel) + (1 - beta) * trend
            seasonal[sIdx] = gamma * (actual - level) + (1 - gamma) * seasonal[sIdx]
        }

        // Extrapolate
        val predictions = mutableListOf<Double>()
        for (h in 1..horizon) {
            val sIdx = (data.size + h - 1) % period
            val pred = level + h * trend + seasonal[sIdx]
            predictions.add(pred)
        }

        val stdDev = BacktestUtils.computeResidualStdDev(data, fitted)
        val bands = BacktestUtils.computeBands(predictions, stdDev)

        return PredictionOutput(
            predictions = predictions,
            lowerBound80 = bands.lower80,
            upperBound80 = bands.upper80,
            lowerBound95 = bands.lower95,
            upperBound95 = bands.upperBound95,
            modelName = name
        )
    }

    override fun backtest(data: List<Double>, holdout: Int): BacktestResult {
        val actualHoldout = holdout.coerceIn(1, (data.size - 2).coerceAtLeast(1))
        if (data.size <= actualHoldout + 1) {
            return BacktestResult(0.0, 0.0, 0.0, "Insufficient Data")
        }
        val train = data.dropLast(actualHoldout)
        val test = data.takeLast(actualHoldout)

        val out = predict(train, actualHoldout)
        val mae = BacktestUtils.computeMAE(test, out.predictions)
        val rmse = BacktestUtils.computeRMSE(test, out.predictions)
        val mape = BacktestUtils.computeMAPE(test, out.predictions)

        return BacktestResult(mae, rmse, mape, BacktestUtils.accuracyLabel(mape))
    }

    /**
     * Inspect autocorrelation at typical candidate lags (7 for weekly, 12 for monthly, 4 for quarterly).
     */
    private fun detectPeriod(data: List<Double>): Int? {
        val candidates = listOf(7, 12, 4)
        for (lag in candidates) {
            if (data.size >= 2 * lag) {
                val corr = computeAutocorrelation(data, lag)
                if (corr > 0.3) {
                    return lag
                }
            }
        }
        // If data is sufficiently large, default to 7 if >= 14, or 4 if >= 8
        return when {
            data.size >= 14 -> 7
            data.size >= 8 -> 4
            else -> null
        }
    }

    private fun computeAutocorrelation(data: List<Double>, lag: Int): Double {
        if (data.size <= lag) return 0.0
        val mean = data.average()
        var numerator = 0.0
        var denom = 0.0
        for (i in data.indices) {
            val d = data[i] - mean
            denom += d * d
            if (i >= lag) {
                numerator += (data[i] - mean) * (data[i - lag] - mean)
            }
        }
        return if (denom > 1e-9) numerator / denom else 0.0
    }
}
