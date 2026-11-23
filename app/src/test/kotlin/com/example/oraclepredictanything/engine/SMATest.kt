package com.example.oraclepredictanything.engine

import com.example.oraclepredictanything.engine.predictors.SMAPredictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SMATest {

    @Test
    fun testSmaKnownValues() {
        val data = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        val sma = SMAPredictor(defaultWindow = 3)
        val result = sma.predict(data, horizon = 2)

        assertEquals(2, result.predictions.size)
        // Last 3 points: 30, 40, 50 -> average is 40.0
        assertEquals(40.0, result.predictions[0], 0.001)

        // Rolling next: 40, 50, 40 -> average is 43.333
        assertEquals(43.333, result.predictions[1], 0.01)
    }

    @Test
    fun testConfidenceBandsOrdered() {
        val data = (1..20).map { it.toDouble() * 5.0 }
        val sma = SMAPredictor()
        val result = sma.predict(data, horizon = 3)

        for (i in 0 until 3) {
            assertTrue("lower95 <= lower80", result.lowerBound95[i] <= result.lowerBound80[i])
            assertTrue("lower80 <= prediction", result.lowerBound80[i] <= result.predictions[i])
            assertTrue("prediction <= upper80", result.predictions[i] <= result.upperBound80[i])
            assertTrue("upper80 <= upper95", result.upperBound80[i] <= result.upperBound95[i])
        }
    }

    @Test
    fun testBacktestAccuracy() {
        val data = (1..15).map { 100.0 }
        val bt = SMAPredictor().backtest(data, holdout = 3)
        assertEquals(0.0, bt.mae, 0.001)
        assertEquals(0.0, bt.mape, 0.001)
    }
}
