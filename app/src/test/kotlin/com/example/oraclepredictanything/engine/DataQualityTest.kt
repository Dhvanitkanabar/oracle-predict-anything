package com.example.oraclepredictanything.engine

import com.example.oraclepredictanything.engine.quality.DataQualityChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataQualityTest {

    @Test
    fun testCleanDataHighScore() {
        val clean = (1..25).map { it.toDouble() }
        val report = DataQualityChecker.check(clean, minRequired = 10)

        assertEquals(0, report.missingCount)
        assertEquals(0, report.outlierIndices.size)
        assertTrue("Clean data should have score >= 0.85, was ${report.qualityScore}", report.qualityScore >= 0.85)
    }

    @Test
    fun testOutlierDetection() {
        val dataWithOutlier = mutableListOf(10.0, 10.2, 10.1, 9.9, 10.3, 10.0, 10.1, 10.2, 500.0, 10.1, 10.0, 10.2)
        val report = DataQualityChecker.check(dataWithOutlier, minRequired = 5)

        assertTrue("Should detect outlier index 8", report.outlierIndices.contains(8))
        assertTrue("Warnings should mention outlier", report.warnings.any { it.contains("outlier") })
    }

    @Test
    fun testSmallSampleWarning() {
        val small = listOf(10.0, 20.0)
        val report = DataQualityChecker.check(small, minRequired = 10)

        assertTrue(report.warnings.any { it.contains("points") })
        assertTrue(report.qualityScore < 1.0)
    }
}
