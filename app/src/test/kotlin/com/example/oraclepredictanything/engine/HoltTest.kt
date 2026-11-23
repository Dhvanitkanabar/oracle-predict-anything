package com.example.oraclepredictanything.engine

import com.example.oraclepredictanything.engine.predictors.HoltPredictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HoltTest {

    @Test
    fun testHoltLinearTrendExtrapolation() {
        // Pure linear series y = 10 * x: 10, 20, 30, 40, 50, 60, 70, 80, 90, 100
        val data = (1..10).map { it.toDouble() * 10.0 }
        val holt = HoltPredictor()
        val result = holt.predict(data, horizon = 3)

        assertEquals(3, result.predictions.size)
        // With step 10, expected next points are ~110, 120, 130 (within reasonable tolerance)
        assertTrue("Next prediction should be around 110, was ${result.predictions[0]}",
            result.predictions[0] in 100.0..120.0)
        assertTrue("Trend should continue upward", result.predictions[1] > result.predictions[0])
        assertTrue("Trend should continue upward", result.predictions[2] > result.predictions[1])
    }

    @Test
    fun testHoltBacktestOnLinearSeries() {
        val data = (1..20).map { it.toDouble() * 2.5 + 50.0 }
        val bt = HoltPredictor().backtest(data, holdout = 4)
        assertTrue("MAPE on linear series should be small: ${bt.mape}", bt.mape < 15.0)
    }
}
