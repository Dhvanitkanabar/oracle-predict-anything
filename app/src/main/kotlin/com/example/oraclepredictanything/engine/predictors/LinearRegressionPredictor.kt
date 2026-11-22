package com.example.oraclepredictanything.engine.predictors

import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.BacktestUtils
import com.example.oraclepredictanything.engine.PredictionOutput
import com.example.oraclepredictanything.engine.Predictor

/**
 * Classical Ordinary Least Squares (OLS) Linear Regression: y = alpha + beta * x
 */
class LinearRegressionPredictor : Predictor {
    override val name: String = "Linear Regression (OLS)"

    override fun predict(data: List<Double>, horizon: Int): PredictionOutput {
        if (data.isEmpty()) {
            return PredictionOutput(
                predictions = List(horizon) { 0.0 },
                lowerBound80 = List(horizon) { 0.0 },
                upperBound80 = List(horizon) { 0.0 },
                lowerBound95 = List(horizon) { 0.0 },
                upperBound95 = List(horizon) { 0.0 },
                modelName = name
            )
        }
        if (data.size == 1) {
            val v = data[0]
            return PredictionOutput(
                predictions = List(horizon) { v },
                lowerBound80 = List(horizon) { v },
                upperBound80 = List(horizon) { v },
                lowerBound95 = List(horizon) { v },
                upperBound95 = List(horizon) { v },
                modelName = name
            )
        }

        val n = data.size.toDouble()
        var sumX = 0.0
        var sumY = 0.0
        var sumXY = 0.0
        var sumXX = 0.0

        for (i in data.indices) {
            val x = i.toDouble()
            val y = data[i]
            sumX += x
            sumY += y
            sumXY += x * y
            sumXX += x * x
        }

        val denominator = n * sumXX - sumX * sumX
        val slope = if (denominator != 0.0) (n * sumXY - sumX * sumY) / denominator else 0.0
        val intercept = (sumY - slope * sumX) / n

        val fitted = mutableListOf<Double>()
        for (i in data.indices) {
            fitted.add(intercept + slope * i.toDouble())
        }

        val predictions = mutableListOf<Double>()
        for (h in 0 until horizon) {
            val x = (data.size + h).toDouble()
            predictions.add(intercept + slope * x)
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
