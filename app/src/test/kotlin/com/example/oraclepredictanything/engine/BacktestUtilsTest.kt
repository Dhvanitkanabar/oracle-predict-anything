package com.example.oraclepredictanything.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktestUtilsTest {

    @Test
    fun testMAE() {
        val actual = listOf(10.0, 20.0, 30.0)
        val predicted = listOf(12.0, 18.0, 31.0)
        // errors: |10-12|=2, |20-18|=2, |30-31|=1 -> mean = 5/3 = 1.6666...
        val mae = BacktestUtils.computeMAE(actual, predicted)
        assertEquals(1.6667, mae, 0.001)
    }

    @Test
    fun testRMSE() {
        val actual = listOf(0.0, 0.0)
        val predicted = listOf(3.0, 4.0)
        // sq diff: 9 + 16 = 25 -> mean = 12.5 -> sqrt = 3.5355...
        val rmse = BacktestUtils.computeRMSE(actual, predicted)
        assertEquals(3.5355, rmse, 0.001)
    }

    @Test
    fun testMAPE() {
        val actual = listOf(100.0, 200.0)
        val predicted = listOf(110.0, 190.0)
        // percentage errors: 10% and 5% -> mean = 7.5%
        val mape = BacktestUtils.computeMAPE(actual, predicted)
        assertEquals(7.5, mape, 0.001)
    }

    @Test
    fun testConfidenceBandsExpansion() {
        val predictions = listOf(100.0, 100.0, 100.0)
        val bands = BacktestUtils.computeBands(predictions, stdDev = 5.0)

        // Expansion over horizon steps: 80% band distance should strictly increase
        val spread0 = bands.upper80[0] - bands.lower80[0]
        val spread1 = bands.upper80[1] - bands.lower80[1]
        val spread2 = bands.upper80[2] - bands.lower80[2]

        assertTrue(spread1 > spread0)
        assertTrue(spread2 > spread1)
    }
}
