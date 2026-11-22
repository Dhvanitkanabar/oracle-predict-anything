package com.example.oraclepredictanything.engine.technical

import kotlin.math.abs

data class IndicatorContribution(
    val name: String,
    val signal: String,     // "BULLISH", "BEARISH", "NEUTRAL"
    val rawValue: Double,
    val scoreContribution: Double, // -1.0 to +1.0
    val weight: Double,
    val explanation: String
)

data class TradingAnalysis(
    val overallSignal: String,   // "BULLISH", "BEARISH", "NEUTRAL"
    val compositeScore: Double,  // -1.0 to +1.0
    val confidencePercent: Double,
    val indicators: List<IndicatorContribution>,
    val supportLevel: Double,
    val resistanceLevel: Double,
    val summaryExplanation: String
)

/**
 * Transparent rule-based indicator aggregator for trading series.
 * Evaluates multiple classical indicators with zero hidden state.
 */
class TradingSignalGenerator {

    fun analyze(
        close: List<Double>,
        high: List<Double> = emptyList(),
        low: List<Double> = emptyList()
    ): TradingAnalysis {
        if (close.isEmpty()) {
            return TradingAnalysis(
                overallSignal = "NEUTRAL",
                compositeScore = 0.0,
                confidencePercent = 0.0,
                indicators = emptyList(),
                supportLevel = 0.0,
                resistanceLevel = 0.0,
                summaryExplanation = "Insufficient data to compute technical indicators."
            )
        }

        val indicators = mutableListOf<IndicatorContribution>()

        // 1. RSI Indicator
        val rsiSeries = TechnicalIndicators.rsi(close, 14)
        if (rsiSeries.isNotEmpty()) {
            val rsi = rsiSeries.last()
            val (signal, score, expl) = when {
                rsi < 30.0 -> Triple("BULLISH", +1.0, "RSI is oversold at ${String.format("%.1f", rsi)} (mean-reversion bounce expected).")
                rsi > 70.0 -> Triple("BEARISH", -1.0, "RSI is overbought at ${String.format("%.1f", rsi)} (pullback risk elevated).")
                rsi in 45.0..55.0 -> Triple("NEUTRAL", 0.0, "RSI is in neutral equilibrium at ${String.format("%.1f", rsi)}.")
                rsi > 55.0 -> Triple("BULLISH", +0.5, "RSI shows positive momentum at ${String.format("%.1f", rsi)}.")
                else -> Triple("BEARISH", -0.5, "RSI shows negative momentum at ${String.format("%.1f", rsi)}.")
            }
            indicators.add(IndicatorContribution("RSI (14)", signal, rsi, score, 0.25, expl))
        }

        // 2. MACD Histogram
        val (macdLine, signalLine, hist) = TechnicalIndicators.macd(close)
        if (hist.isNotEmpty()) {
            val h = hist.last()
            val prevH = if (hist.size > 1) hist[hist.size - 2] else h
            val (signal, score, expl) = when {
                h > 0 && h > prevH -> Triple("BULLISH", +1.0, "MACD histogram is positive and expanding (+${String.format("%.2f", h)}).")
                h > 0 -> Triple("BULLISH", +0.5, "MACD histogram is positive but decelerating.")
                h < 0 && h < prevH -> Triple("BEARISH", -1.0, "MACD histogram is negative and deteriorating (${String.format("%.2f", h)}).")
                h < 0 -> Triple("BEARISH", -0.5, "MACD histogram is negative but curling upward.")
                else -> Triple("NEUTRAL", 0.0, "MACD is crossing zero.")
            }
            indicators.add(IndicatorContribution("MACD (12,26,9)", signal, h, score, 0.25, expl))
        }

        // 3. Bollinger Bands
        val (upper, middle, lower) = TechnicalIndicators.bollingerBands(close)
        if (upper.isNotEmpty() && lower.isNotEmpty()) {
            val lastClose = close.last()
            val u = upper.last()
            val l = lower.last()
            val m = middle.last()
            val (signal, score, expl) = when {
                lastClose <= l -> Triple("BULLISH", +1.0, "Price touched lower Bollinger Band (${String.format("%.2f", l)}), indicating deep discount.")
                lastClose >= u -> Triple("BEARISH", -1.0, "Price tested upper Bollinger Band (${String.format("%.2f", u)}), near statistical ceiling.")
                lastClose > m -> Triple("BULLISH", +0.4, "Price is trading above the 20-period moving average.")
                else -> Triple("BEARISH", -0.4, "Price is trading below the 20-period moving average.")
            }
            indicators.add(IndicatorContribution("Bollinger Bands (20,2)", signal, lastClose, score, 0.20, expl))
        }

        // 4. SMA Crossover (Fast 9 vs Slow 21)
        val fastSma = TechnicalIndicators.sma(close, 9)
        val slowSma = TechnicalIndicators.sma(close, 21)
        if (fastSma.isNotEmpty() && slowSma.isNotEmpty()) {
            val f = fastSma.last()
            val s = slowSma.last()
            val diffPct = (f - s) / s * 100.0
            val (signal, score, expl) = when {
                diffPct > 0.5 -> Triple("BULLISH", +1.0, "Fast SMA(9) is above Slow SMA(21) by ${String.format("%.1f", diffPct)}% (Golden Cross pattern).")
                diffPct < -0.5 -> Triple("BEARISH", -1.0, "Fast SMA(9) is below Slow SMA(21) by ${String.format("%.1f", abs(diffPct))}% (Death Cross pattern).")
                else -> Triple("NEUTRAL", 0.0, "SMA 9/21 are converging.")
            }
            indicators.add(IndicatorContribution("SMA Crossover (9/21)", signal, diffPct, score, 0.15, expl))
        }

        // 5. Stochastic (if high & low supplied, else emulate from close)
        val hList = if (high.size == close.size) high else close
        val lList = if (low.size == close.size) low else close
        val (kLine, dLine) = TechnicalIndicators.stochastic(close, hList, lList)
        if (kLine.isNotEmpty() && dLine.isNotEmpty()) {
            val k = kLine.last()
            val d = dLine.last()
            val (signal, score, expl) = when {
                k < 20.0 && k > d -> Triple("BULLISH", +1.0, "Stochastic %K (${String.format("%.1f", k)}) crossed above %D in oversold territory.")
                k > 80.0 && k < d -> Triple("BEARISH", -1.0, "Stochastic %K (${String.format("%.1f", k)}) crossed below %D in overbought territory.")
                k > 60.0 -> Triple("BULLISH", +0.5, "Stochastic indicates positive momentum.")
                k < 40.0 -> Triple("BEARISH", -0.5, "Stochastic indicates weak momentum.")
                else -> Triple("NEUTRAL", 0.0, "Stochastic is neutral.")
            }
            indicators.add(IndicatorContribution("Stochastic (14,3)", signal, k, score, 0.15, expl))
        }

        // Support & Resistance levels
        val (sup, res) = TechnicalIndicators.supportResistance(close)

        // Compute weighted composite score
        var totalWeightedScore = 0.0
        var totalWeight = 0.0
        for (ind in indicators) {
            totalWeightedScore += ind.scoreContribution * ind.weight
            totalWeight += ind.weight
        }
        val composite = if (totalWeight > 0) totalWeightedScore / totalWeight else 0.0

        val overallSignal = when {
            composite >= 0.25 -> "BULLISH"
            composite <= -0.25 -> "BEARISH"
            else -> "NEUTRAL"
        }

        val confidence = (abs(composite) * 100.0).coerceIn(20.0, 95.0)

        val bullishCount = indicators.count { it.signal == "BULLISH" }
        val bearishCount = indicators.count { it.signal == "BEARISH" }
        val neutralCount = indicators.count { it.signal == "NEUTRAL" }

        val summary = "Signal: $overallSignal (Score: ${String.format("%+.2f", composite)}). " +
                "$bullishCount indicators bullish, $bearishCount bearish, $neutralCount neutral. " +
                "Key Support at ${String.format("%.2f", sup)}, Resistance at ${String.format("%.2f", res)}."

        return TradingAnalysis(
            overallSignal = overallSignal,
            compositeScore = composite,
            confidencePercent = confidence,
            indicators = indicators,
            supportLevel = sup,
            resistanceLevel = res,
            summaryExplanation = summary
        )
    }
}
