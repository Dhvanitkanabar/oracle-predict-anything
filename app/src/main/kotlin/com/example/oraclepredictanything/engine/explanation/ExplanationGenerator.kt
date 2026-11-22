package com.example.oraclepredictanything.engine.explanation

import com.example.oraclepredictanything.engine.EnsembleResult
import com.example.oraclepredictanything.engine.categorical.TopicPrediction
import com.example.oraclepredictanything.engine.quality.QualityReport
import com.example.oraclepredictanything.engine.technical.TradingAnalysis
import kotlin.math.abs

/**
 * 100% deterministic rule-based natural language explanation generator.
 * Builds rigorous, transparent explanations directly from computed mathematical facts.
 * NO LLMs, NO generative APIs, NO stochastic output.
 */
object ExplanationGenerator {

    /**
     * Generate clear statistical reasoning for numeric time series forecasts.
     */
    fun generateNumeric(
        data: List<Double>,
        ensemble: EnsembleResult,
        quality: QualityReport
    ): String {
        if (data.isEmpty()) return "No historical data provided for forecasting."

        val sb = StringBuilder()
        val n = data.size
        val first = data.first()
        val last = data.last()
        val netChange = last - first
        val netChangePct = if (abs(first) > 1e-9) (netChange / first) * 100.0 else 0.0

        // 1. Historical Pattern
        sb.append("Historical Analysis: Analyzed $n historical data points. ")
        if (abs(netChangePct) > 0.1) {
            val direction = if (netChangePct > 0) "rose" else "declined"
            sb.append("Series $direction by ${String.format("%.1f", abs(netChangePct))}% overall ")
            sb.append("(from ${String.format("%.2f", first)} to ${String.format("%.2f", last)}). ")
        } else {
            sb.append("Series remained relatively flat near ${String.format("%.2f", last)}. ")
        }

        // 2. Short-term momentum
        if (n >= 4) {
            val recentHalf = data.takeLast(n / 2)
            val recentAvg = recentHalf.average()
            val priorAvg = data.take(n / 2).average()
            val momentumPct = if (abs(priorAvg) > 1e-9) ((recentAvg - priorAvg) / priorAvg) * 100.0 else 0.0
            if (abs(momentumPct) > 1.0) {
                val momDirection = if (momentumPct > 0) "accelerating upward" else "decelerating downward"
                sb.append("Recent momentum is $momDirection (${String.format("%+.1f", momentumPct)}% vs prior baseline). ")
            }
        }

        // 3. Model Weighting & Best Model
        sb.append("\n\nModel Selection: ")
        val bestWeight = (ensemble.weights[ensemble.bestModel] ?: 0.0) * 100.0
        sb.append("${ensemble.bestModel} achieved the lowest error in backtesting and received the highest weight (${String.format("%.1f", bestWeight)}%). ")

        // Mention secondary models
        val topWeights = ensemble.weights.toList().sortedByDescending { it.second }.take(3)
        if (topWeights.size > 1) {
            val others = topWeights.drop(1).joinToString(", ") { "${it.first} (${String.format("%.1f", it.second * 100.0)}%)" }
            sb.append("Additional contributing models: $others. ")
        }

        // 4. Expected Forecast
        if (ensemble.predictions.isNotEmpty()) {
            val nextVal = ensemble.predictions.first()
            val expectedStepChange = if (abs(last) > 1e-9) ((nextVal - last) / last) * 100.0 else 0.0
            sb.append("\n\nForecast Projection: Next expected value is ${String.format("%.2f", nextVal)} ")
            sb.append("(${String.format("%+.1f", expectedStepChange)}% vs current). ")
            sb.append("With 80% statistical confidence, value is estimated between ${String.format("%.2f", ensemble.lowerBound80.first())} and ${String.format("%.2f", ensemble.upperBound80.first())}.")
        }

        // 5. Data Quality Warnings
        if (quality.warnings.isNotEmpty()) {
            sb.append("\n\nQuality Notes: ")
            for (w in quality.warnings) {
                sb.append("• $w ")
            }
            sb.append("Overall data confidence is rated at ${(quality.adjustedConfidence * 100).toInt()}%.")
        }

        return sb.toString()
    }

    /**
     * Generate structured trading signal report.
     */
    fun generateTrading(analysis: TradingAnalysis): String {
        val sb = StringBuilder()
        sb.append("Technical Indicator Summary: ${analysis.overallSignal} signal with ${(analysis.confidencePercent).toInt()}% confidence. ")
        sb.append("Composite statistical score is ${String.format("%+.2f", analysis.compositeScore)} (-1.0 Bearish to +1.0 Bullish).\n\n")

        sb.append("Key Levels: Immediate Support at ${String.format("%.2f", analysis.supportLevel)}, Resistance at ${String.format("%.2f", analysis.resistanceLevel)}.\n\n")

        sb.append("Indicator Breakdown:\n")
        for (ind in analysis.indicators) {
            val icon = when (ind.signal) {
                "BULLISH" -> "🟢"
                "BEARISH" -> "🔴"
                else -> "⚪"
            }
            sb.append("$icon ${ind.name} [${ind.signal}]: ${ind.explanation}\n")
        }
        return sb.toString()
    }

    /**
     * Generate explanation for categorical exam / outcome forecasts.
     */
    fun generateCategorical(predictions: List<TopicPrediction>): String {
        if (predictions.isEmpty()) return "No topic predictions generated."
        val sb = StringBuilder()
        val top = predictions.first()
        sb.append("Top Priority: '${top.topicName}' ranks highest with ${String.format("%.1f", top.probabilityPercent)}% probability ")
        sb.append("(expected weight/marks: ${String.format("%.1f", top.expectedWeight)}). ")

        val overdue = predictions.filter { it.isOverdue }
        if (overdue.isNotEmpty()) {
            val overdueNames = overdue.joinToString(", ") { "'${it.topicName}' (${it.lastSeenGap} periods since last appearance)" }
            sb.append("\n\nCycle Overdue Alert: $overdueNames have exceeded their historical recurrence interval and are statistically due to appear.")
        }

        val rising = predictions.filter { it.trend == "Rising" }
        if (rising.isNotEmpty()) {
            val risingNames = rising.joinToString(", ") { "'${it.topicName}'" }
            sb.append("\n\nUpward Trend: $risingNames show increasing frequency/marks over recent cycles.")
        }

        return sb.toString()
    }
}
