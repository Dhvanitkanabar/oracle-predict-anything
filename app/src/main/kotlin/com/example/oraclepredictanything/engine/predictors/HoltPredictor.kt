package com.example.oraclepredictanything.engine.predictors

import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.BacktestUtils
import com.example.oraclepredictanything.engine.PredictionOutput
import com.example.oraclepredictanything.engine.Predictor

/**
 * Holt's Linear Trend method (Double Exponential Smoothing).
 * Explicitly tracks level and trend components with automated grid-search optimization for alpha & beta.
 */
class HoltPredictor : Predictor {
    override val name: String = "Holt's Linear Trend"

    override fun predict(data: List<Double>, horizon: Int): PredictionOutput {
        if (data.size < 2) {
            val v = data.firstOrNull() ?: 0.0
            return PredictionOutput(
                predictions = List(horizon) { v },
                lowerBound80 = List(horizon) { v },
                upperBound80 = List(horizon) { v },
                lowerBound95 = List(horizon) { v },
                upperBound95 = List(horizon) { v },
                modelName = name
            )
        }

        // Grid search for best (alpha, beta)
        var bestAlpha = 0.3
        var bestBeta = 0.1
        var minSse = Double.MAX_VALUE

        val alphas = listOf(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8)
        val betas = listOf(0.05, 0.1, 0.15, 0.2, 0.25, 0.3)

        for (a in alphas) {
            for (b in betas) {
                var l = data[0]
                var bTrend = data[1] - data[0]
                var sse = 0.0
                for (t in 1 until data.size) {
                    val yHat = l + bTrend
                    val err = data[t] - yHat
                    sse += err * err
                    val nextL = a * data[t] + (1 - a) * (l + bTrend)
                    val nextB = b * (nextL - l) + (1 - b) * bTrend
                    l = nextL
                    bTrend = nextB
                }
                if (sse < minSse) {
                    minSse = sse
                    bestAlpha = a
                    bestBeta = b
                }
            }
        }

        // Final pass with best parameters
        var level = data[0]
        var trend = data[1] - data[0]
        val fitted = mutableListOf<Double>()
        fitted.add(level)

        for (t in 1 until data.size) {
            val yHat = level + trend
            fitted.add(yHat)
            val nextL = bestAlpha * data[t] + (1 - bestAlpha) * (level + trend)
            val nextB = bestBeta * (nextL - level) + (1 - bestBeta) * trend
            level = nextL
            trend = nextB
        }

        // Extrapolate h steps ahead: y_{t+h} = level + h * trend
        val predictions = mutableListOf<Double>()
        for (h in 1..horizon) {
            predictions.add(level + h * trend)
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
}
