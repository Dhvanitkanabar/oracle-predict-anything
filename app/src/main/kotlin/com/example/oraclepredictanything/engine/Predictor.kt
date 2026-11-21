package com.example.oraclepredictanything.engine

/**
 * Universal interface for deterministic statistical time-series predictors.
 */
interface Predictor {
    val name: String

    /**
     * Generate forecasts for the next [horizon] steps.
     */
    fun predict(data: List<Double>, horizon: Int): PredictionOutput

    /**
     * Backtest the model by reserving the last [holdout] points.
     */
    fun backtest(data: List<Double>, holdout: Int = 5): BacktestResult
}
