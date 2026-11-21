package com.example.oraclepredictanything.engine

/**
 * Standard prediction output holding forecasted values and confidence intervals.
 */
data class PredictionOutput(
    val predictions: List<Double>,
    val lowerBound80: List<Double>,
    val upperBound80: List<Double>,
    val lowerBound95: List<Double>,
    val upperBound95: List<Double>,
    val modelName: String
)

/**
 * Backtest performance metrics for evaluating statistical fit.
 */
data class BacktestResult(
    val mae: Double,
    val rmse: Double,
    val mape: Double,
    val accuracyLabel: String
)

/**
 * Ensemble result combining multiple individual models via inverse-error weighting.
 */
data class EnsembleResult(
    val predictions: List<Double>,
    val lowerBound80: List<Double>,
    val upperBound80: List<Double>,
    val lowerBound95: List<Double>,
    val upperBound95: List<Double>,
    val modelResults: Map<String, PredictionOutput>,
    val weights: Map<String, Double>,
    val bestModel: String
)
