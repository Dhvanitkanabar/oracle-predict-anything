package com.example.oraclepredictanything.engine

import com.example.oraclepredictanything.engine.predictors.*

/**
 * Registry of deterministic mathematical predictors and ensemble weighting coordinator.
 */
object PredictorRegistry {

    val allPredictors: List<Predictor> = listOf(
        SMAPredictor(),
        WMAPredictor(),
        EMAPredictor(),
        HoltPredictor(),
        HoltWintersPredictor(),
        LinearRegressionPredictor(),
        PolynomialRegressionPredictor()
    )

    /**
     * Run backtesting on all registered models and return backtest metrics map.
     */
    fun backtestAll(data: List<Double>, holdout: Int = 5): Map<String, BacktestResult> {
        val results = mutableMapOf<String, BacktestResult>()
        for (p in allPredictors) {
            try {
                results[p.name] = p.backtest(data, holdout)
            } catch (e: Exception) {
                results[p.name] = BacktestResult(999.0, 999.0, 999.0, "Error")
            }
        }
        return results
    }

    /**
     * Compute an ensemble forecast:
     * 1. Run backtest on each model
     * 2. Calculate inverse-error weight: w_m = 1 / (MAPE_m + 0.5)^2
     * 3. Normalize weights to sum to 1.0
     * 4. Compute weighted average predictions and confidence intervals
     */
    fun ensemble(data: List<Double>, horizon: Int): EnsembleResult {
        if (data.isEmpty()) {
            val emptyList = List(horizon) { 0.0 }
            return EnsembleResult(
                predictions = emptyList,
                lowerBound80 = emptyList,
                upperBound80 = emptyList,
                lowerBound95 = emptyList,
                upperBound95 = emptyList,
                modelResults = emptyMap(),
                weights = emptyMap(),
                bestModel = "None"
            )
        }

        val backtests = backtestAll(data)
        val outputs = mutableMapOf<String, PredictionOutput>()

        for (p in allPredictors) {
            outputs[p.name] = p.predict(data, horizon)
        }

        // Calculate inverse MAPE weights
        val rawWeights = mutableMapOf<String, Double>()
        var sumWeights = 0.0

        for ((name, bt) in backtests) {
            // Avoid division by zero, penalize high MAPE non-linearly
            val score = 1.0 / ((bt.mape.coerceAtLeast(0.1) + 1.0) * (bt.mape.coerceAtLeast(0.1) + 1.0))
            rawWeights[name] = score
            sumWeights += score
        }

        val normalizedWeights = mutableMapOf<String, Double>()
        for ((name, w) in rawWeights) {
            normalizedWeights[name] = if (sumWeights > 0) w / sumWeights else (1.0 / allPredictors.size)
        }

        // Best model is the one with minimum backtest MAPE
        val bestEntry = backtests.minByOrNull { it.value.mape }
        val bestModelName = bestEntry?.key ?: allPredictors.first().name

        // Weighted combination of predicted steps
        val ensemblePred = MutableList(horizon) { 0.0 }
        val ensembleL80 = MutableList(horizon) { 0.0 }
        val ensembleU80 = MutableList(horizon) { 0.0 }
        val ensembleL95 = MutableList(horizon) { 0.0 }
        val ensembleU95 = MutableList(horizon) { 0.0 }

        for ((name, out) in outputs) {
            val w = normalizedWeights[name] ?: 0.0
            for (h in 0 until horizon) {
                if (h < out.predictions.size) {
                    ensemblePred[h] += out.predictions[h] * w
                    ensembleL80[h] += out.lowerBound80[h] * w
                    ensembleU80[h] += out.upperBound80[h] * w
                    ensembleL95[h] += out.lowerBound95[h] * w
                    ensembleU95[h] += out.upperBound95[h] * w
                }
            }
        }

        return EnsembleResult(
            predictions = ensemblePred,
            lowerBound80 = ensembleL80,
            upperBound80 = ensembleU80,
            lowerBound95 = ensembleL95,
            upperBound95 = ensembleU95,
            modelResults = outputs,
            weights = normalizedWeights,
            bestModel = bestModelName
        )
    }
}
