package com.example.oraclepredictanything.engine

import com.example.oraclepredictanything.engine.predictors.LinearRegressionPredictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinearRegressionTest {

    @Test
    fun testExactLinearFit() {
        // Exact equation: y = 3.0 * x + 7.0 for x = 0..9
        val data = (0..9).map { 3.0 * it + 7.0 }
        val reg = LinearRegressionPredictor()
        val result = reg.predict(data, horizon = 3)

        assertEquals(3, result.predictions.size)
        // For x=10: 3*10 + 7 = 37.0
        assertEquals(37.0, result.predictions[0], 0.001)
        // For x=11: 3*11 + 7 = 40.0
        assertEquals(40.0, result.predictions[1], 0.001)
        // For x=12: 3*12 + 7 = 43.0
        assertEquals(43.0, result.predictions[2], 0.001)
    }

    @Test
    fun testRegressionBacktestPerfectFit() {
        val data = (0..14).map { 5.0 * it + 12.0 }
        val bt = LinearRegressionPredictor().backtest(data, holdout = 3)
        assertEquals(0.0, bt.mae, 0.001)
        assertEquals(0.0, bt.rmse, 0.001)
        assertEquals(0.0, bt.mape, 0.001)
    }
}
