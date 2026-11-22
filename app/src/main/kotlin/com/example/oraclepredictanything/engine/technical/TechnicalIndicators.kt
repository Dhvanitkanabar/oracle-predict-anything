package com.example.oraclepredictanything.engine.technical

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure statistical technical indicators for financial market price series.
 */
object TechnicalIndicators {

    /**
     * Simple Moving Average (SMA) series.
     */
    fun sma(data: List<Double>, period: Int): List<Double> {
        if (data.size < period || period <= 0) return emptyList()
        val result = mutableListOf<Double>()
        var windowSum = 0.0
        for (i in 0 until period) {
            windowSum += data[i]
        }
        result.add(windowSum / period)

        for (i in period until data.size) {
            windowSum += data[i] - data[i - period]
            result.add(windowSum / period)
        }
        return result
    }

    /**
     * Exponential Moving Average (EMA) series with smoothing factor 2 / (period + 1).
     */
    fun ema(data: List<Double>, period: Int): List<Double> {
        if (data.size < period || period <= 0) return emptyList()
        val result = mutableListOf<Double>()
        val multiplier = 2.0 / (period + 1.0)

        // Seed with SMA
        var currentEma = data.take(period).average()
        result.add(currentEma)

        for (i in period until data.size) {
            currentEma = (data[i] - currentEma) * multiplier + currentEma
            result.add(currentEma)
        }
        return result
    }

    /**
     * Relative Strength Index (RSI) using Wilder's smoothed moving averages.
     * Returns series bounded within [0.0, 100.0].
     */
    fun rsi(data: List<Double>, period: Int = 14): List<Double> {
        if (data.size <= period) return emptyList()
        val gains = mutableListOf<Double>()
        val losses = mutableListOf<Double>()

        for (i in 1 until data.size) {
            val change = data[i] - data[i - 1]
            if (change > 0) {
                gains.add(change)
                losses.add(0.0)
            } else {
                gains.add(0.0)
                losses.add(-change)
            }
        }

        var avgGain = gains.take(period).average()
        var avgLoss = losses.take(period).average()

        val result = mutableListOf<Double>()
        val initialRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        val initialRsi = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + initialRs))
        result.add(initialRsi)

        for (i in period until gains.size) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period
            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            val rsiVal = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
            result.add(rsiVal.coerceIn(0.0, 100.0))
        }
        return result
    }

    /**
     * Moving Average Convergence Divergence (MACD):
     * Returns Triple(macdLine, signalLine, histogram).
     */
    fun macd(
        data: List<Double>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): Triple<List<Double>, List<Double>, List<Double>> {
        if (data.size < slowPeriod + signalPeriod) {
            return Triple(emptyList(), emptyList(), emptyList())
        }
        val fastEma = ema(data, fastPeriod)
        val slowEma = ema(data, slowPeriod)

        // Align starts (slowEma starts at index slowPeriod - 1)
        val offset = slowPeriod - fastPeriod
        val macdLine = mutableListOf<Double>()
        for (i in slowEma.indices) {
            macdLine.add(fastEma[i + offset] - slowEma[i])
        }

        val signalLine = ema(macdLine, signalPeriod)
        val signalOffset = signalPeriod - 1
        val histogram = mutableListOf<Double>()
        for (i in signalLine.indices) {
            histogram.add(macdLine[i + signalOffset] - signalLine[i])
        }

        return Triple(macdLine, signalLine, histogram)
    }

    /**
     * Bollinger Bands: Returns Triple(upperBand, middleBand, lowerBand) with [multiplier] standard deviations.
     */
    fun bollingerBands(
        data: List<Double>,
        period: Int = 20,
        multiplier: Double = 2.0
    ): Triple<List<Double>, List<Double>, List<Double>> {
        val middle = sma(data, period)
        if (middle.isEmpty()) return Triple(emptyList(), emptyList(), emptyList())

        val upper = mutableListOf<Double>()
        val lower = mutableListOf<Double>()

        for (i in period - 1 until data.size) {
            val slice = data.subList(i - period + 1, i + 1)
            val mean = middle[i - (period - 1)]
            var varianceSum = 0.0
            for (v in slice) {
                varianceSum += (v - mean) * (v - mean)
            }
            val std = sqrt(varianceSum / period)
            upper.add(mean + multiplier * std)
            lower.add(mean - multiplier * std)
        }
        return Triple(upper, middle, lower)
    }

    /**
     * Stochastic Oscillator %K and %D:
     * %K = (Close - Low_14) / (High_14 - Low_14) * 100
     * %D = 3-period SMA of %K
     */
    fun stochastic(
        close: List<Double>,
        high: List<Double>,
        low: List<Double>,
        kPeriod: Int = 14,
        dPeriod: Int = 3
    ): Pair<List<Double>, List<Double>> {
        val n = minOf(close.size, high.size, low.size)
        if (n < kPeriod) return Pair(emptyList(), emptyList())

        val kLine = mutableListOf<Double>()
        for (i in kPeriod - 1 until n) {
            var minL = Double.MAX_VALUE
            var maxH = Double.MIN_VALUE
            for (j in i - kPeriod + 1..i) {
                minL = min(minL, low[j])
                maxH = max(maxH, high[j])
            }
            val range = maxH - minL
            val k = if (range > 1e-9) ((close[i] - minL) / range) * 100.0 else 50.0
            kLine.add(k.coerceIn(0.0, 100.0))
        }

        val dLine = sma(kLine, dPeriod)
        return Pair(kLine, dLine)
    }

    /**
     * Detect dynamic horizontal support and resistance levels using rolling swing pivots.
     */
    fun supportResistance(data: List<Double>, lookback: Int = 20): Pair<Double, Double> {
        if (data.isEmpty()) return Pair(0.0, 0.0)
        val slice = data.takeLast(lookback.coerceAtMost(data.size))
        val support = slice.minOrNull() ?: 0.0
        val resistance = slice.maxOrNull() ?: 0.0
        return Pair(support, resistance)
    }
}
