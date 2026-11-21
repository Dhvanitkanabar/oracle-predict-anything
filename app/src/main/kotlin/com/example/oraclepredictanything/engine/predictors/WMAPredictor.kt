package com.example.oraclepredictanything.engine.predictors

import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.BacktestUtils
import com.example.oraclepredictanything.engine.PredictionOutput
import com.example.oraclepredictanything.engine.Predictor

class WMAPredictor(private val defaultWindow: Int = 5) : Predictor {
    override val name: String = "Weighted Moving Average (WMA)"

    override fun predict(data: List<Double>, horizon: Int): PredictionOutput {
        if (data.isEmpty()) return emptyOutput(horizon)
        val window = defaultWindow.coerceIn(1, data.size)
        val rolling = data.toMutableList()
        val predictions = mutableListOf<Double>()

        // Linearly increasing weights: 1, 2, ..., w
        val weights = (1..window).map { it.toDouble() }
        val sumWeights = weights.sum()

        for (h in 0 until horizon) {
            val slice = rolling.takeLast(window)
            var wSum = 0.0
            for (i in slice.indices) {
                wSum += slice[i] * weights[i]
            }
            val pred = wSum / sumWeights
            predictions.add(pred)
            rolling.add(pred)
        }

        val fitted = mutableListOf<Double>()
        for (i in data.indices) {
            if (i < window) {
                fitted.add(data[i])
            } else {
                val slice = data.subList(i - window, i)
                var ws = 0.0
                for (j in slice.indices) ws += slice[j] * weights[j]
                fitted.add(ws / sumWeights)
            }
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

    private fun emptyOutput(horizon: Int) = PredictionOutput(
        predictions = List(horizon) { 0.0 },
        lowerBound80 = List(horizon) { 0.0 },
        upperBound80 = List(horizon) { 0.0 },
        lowerBound95 = List(horizon) { 0.0 },
        upperBound95 = List(horizon) { 0.0 },
        modelName = name
    )
}
