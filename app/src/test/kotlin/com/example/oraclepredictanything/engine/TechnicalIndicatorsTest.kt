package com.example.oraclepredictanything.engine

import com.example.oraclepredictanything.engine.technical.TechnicalIndicators
import com.example.oraclepredictanything.engine.technical.TradingSignalGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalIndicatorsTest {

    @Test
    fun testSmaCalculation() {
        val data = listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        val sma3 = TechnicalIndicators.sma(data, 3)
        assertEquals(4, sma3.size)
        assertEquals(2.0, sma3[0], 0.001) // (1+2+3)/3
        assertEquals(3.0, sma3[1], 0.001) // (2+3+4)/3
        assertEquals(4.0, sma3[2], 0.001) // (3+4+5)/3
        assertEquals(5.0, sma3[3], 0.001) // (4+5+6)/3
    }

    @Test
    fun testRsiRangeAndDirection() {
        // Strongly rising price series -> RSI should be in overbought territory (>70)
        val rising = (1..30).map { it.toDouble() * 10.0 }
        val rsiSeries = TechnicalIndicators.rsi(rising, 14)
        assertTrue(rsiSeries.isNotEmpty())
        val lastRsi = rsiSeries.last()
        assertTrue("RSI should be high on rising series, was $lastRsi", lastRsi > 70.0)

        // Strongly falling price series -> RSI should be in oversold territory (<30)
        val falling = (30 downTo 1).map { it.toDouble() * 10.0 }
        val fallingRsi = TechnicalIndicators.rsi(falling, 14)
        assertTrue(fallingRsi.isNotEmpty())
        val lastFallingRsi = fallingRsi.last()
        assertTrue("RSI should be low on falling series, was $lastFallingRsi", lastFallingRsi < 30.0)
    }

    @Test
    fun testBollingerBandsEnclosure() {
        val data = (1..30).map { 100.0 + (it % 5) * 5.0 }
        val (upper, middle, lower) = TechnicalIndicators.bollingerBands(data, 10, 2.0)
        assertTrue(upper.isNotEmpty())
        for (i in upper.indices) {
            assertTrue("lower <= middle", lower[i] <= middle[i])
            assertTrue("middle <= upper", middle[i] <= upper[i])
        }
    }

    @Test
    fun testTradingSignalBullish() {
        // Bullish momentum
        val data = (1..40).map { 1000.0 + it * 20.0 }
        val analysis = TradingSignalGenerator().analyze(data)
        assertTrue(analysis.indicators.isNotEmpty())
        assertTrue("Support <= Resistance", analysis.supportLevel <= analysis.resistanceLevel)
    }
}
