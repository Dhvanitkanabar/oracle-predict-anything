package com.example.oraclepredictanything.engine.predictors

import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.BacktestUtils
import com.example.oraclepredictanything.engine.PredictionOutput
import com.example.oraclepredictanything.engine.Predictor

class EMAPredictor(private val defaultAlpha: Double = 0.3) : Predictor {
    override val name: String = "Exponential Smoothing (EMA)"

    override fun predict(data: List<Double>, horizon: Int): PredictionOutput {
        if (data.isEmpty()) return emptyOutput(horizon)

        // Find optimal alpha between 0.1 and 0.9 minimizing MSE on training data
        var bestAlpha = defaultAlpha
        var bestMse = Double.MAX_VALUE

        for (a in 1..9) {
            val alphaCandidate = a * 0.1
            var s = data[0]
            var mse = 0.0
            for (i in 1 until data.size) {
                val error = data[i] - s
                mse += error * error
                s = alphaCandidate * data[i] + (1 - alphaCandidate) * s
            }
            if (mse < bestMse) {
                bestMse = mse
                bestAlpha = alphaCandidate
            }
        }

        // Run smoothed sequence
        var currentS = data[0]
        val fitted = mutableListOf<Double>()
        fitted.add(currentS)
        for (i in 1 until data.size) {
            currentS = bestAlpha * data[i] + (1 - bestAlpha) * currentS
            fitted.add(currentS)
        }

        // Forecast is constant last level for simple exponential smoothing
        val predictions = List(horizon) { currentS }
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
