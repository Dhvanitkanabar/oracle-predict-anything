package com.example.oraclepredictanything.util

import com.example.oraclepredictanything.data.model.DataPoint

/**
 * Built-in realistic sample datasets for instant zero-setup demonstration.
 */
object SampleData {

    /**
     * Realistic NIFTY 50 / Index price series (60 sequential daily points).
     */
    fun getTradingSample(datasetId: Long): List<DataPoint> {
        val basePrices = listOf(
            21850.0, 21920.0, 21890.0, 22010.0, 22080.0, 22050.0, 22140.0, 22210.0,
            22180.0, 22250.0, 22320.0, 22290.0, 22380.0, 22450.0, 22400.0, 22480.0,
            22530.0, 22490.0, 22560.0, 22620.0, 22580.0, 22650.0, 22710.0, 22680.0,
            22740.0, 22800.0, 22760.0, 22830.0, 22890.0, 22850.0, 22920.0, 22980.0,
            22940.0, 23010.0, 23070.0, 23020.0, 23100.0, 23160.0, 23120.0, 23190.0,
            23250.0, 23210.0, 23280.0, 23340.0, 23300.0, 23370.0, 23430.0, 23390.0,
            23460.0, 23520.0, 23480.0, 23550.0, 23610.0, 23570.0, 23640.0, 23700.0,
            23660.0, 23730.0, 23790.0, 23850.0
        )
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L

        return basePrices.mapIndexed { index, close ->
            val open = close - (index % 5 - 2) * 20.0
            val high = close + (index % 3 + 1) * 35.0
            val low = close - (index % 4 + 1) * 30.0
            val vol = 150000.0 + (index % 7) * 25000.0
            val extraJson = """{"open":$open,"high":$high,"low":$low,"volume":$vol}"""
            val timestamp = now - (basePrices.size - index) * dayMillis

            DataPoint(
                datasetId = datasetId,
                timestamp = timestamp,
                value = close,
                label = "Day ${index + 1}",
                extraJson = extraJson
            )
        }
    }

    /**
     * JEE/NEET Exam paper chapter occurrences across 10 years (2015-2024).
     */
    fun getExamSample(datasetId: Long): List<DataPoint> {
        val records = listOf(
            Triple("Calculus & Integration", 2015, 24.0),
            Triple("Electrostatics & Magnetism", 2015, 20.0),
            Triple("Mechanics & Rotational", 2015, 28.0),
            Triple("Thermodynamics", 2015, 16.0),
            Triple("Optics & Waves", 2015, 12.0),

            Triple("Calculus & Integration", 2016, 28.0),
            Triple("Mechanics & Rotational", 2016, 24.0),
            Triple("Electrostatics & Magnetism", 2016, 16.0),
            Triple("Modern Physics", 2016, 16.0),
            Triple("Chemical Kinetics", 2016, 16.0),

            Triple("Calculus & Integration", 2017, 24.0),
            Triple("Electrostatics & Magnetism", 2017, 24.0),
            Triple("Organic Reactions", 2017, 20.0),
            Triple("Optics & Waves", 2017, 16.0),
            Triple("Thermodynamics", 2017, 16.0),

            Triple("Calculus & Integration", 2018, 32.0),
            Triple("Mechanics & Rotational", 2018, 20.0),
            Triple("Modern Physics", 2018, 20.0),
            Triple("Electrostatics & Magnetism", 2018, 16.0),
            Triple("Chemical Kinetics", 2018, 12.0),

            Triple("Calculus & Integration", 2019, 28.0),
            Triple("Electrostatics & Magnetism", 2019, 24.0),
            Triple("Thermodynamics", 2019, 20.0),
            Triple("Organic Reactions", 2019, 16.0),
            Triple("Optics & Waves", 2019, 12.0),

            Triple("Calculus & Integration", 2020, 36.0),
            Triple("Modern Physics", 2020, 24.0),
            Triple("Mechanics & Rotational", 2020, 20.0),
            Triple("Electrostatics & Magnetism", 2020, 12.0),
            Triple("Chemical Kinetics", 2020, 8.0),

            Triple("Calculus & Integration", 2021, 32.0),
            Triple("Electrostatics & Magnetism", 2021, 28.0),
            Triple("Thermodynamics", 2021, 20.0),
            Triple("Optics & Waves", 2021, 12.0),
            Triple("Organic Reactions", 2021, 8.0),

            Triple("Calculus & Integration", 2022, 28.0),
            Triple("Mechanics & Rotational", 2022, 24.0),
            Triple("Modern Physics", 2022, 24.0),
            Triple("Electrostatics & Magnetism", 2022, 16.0),
            Triple("Chemical Kinetics", 2022, 8.0),

            Triple("Calculus & Integration", 2023, 32.0),
            Triple("Electrostatics & Magnetism", 2023, 24.0),
            Triple("Thermodynamics", 2023, 20.0),
            Triple("Modern Physics", 2023, 16.0),
            Triple("Optics & Waves", 2023, 8.0),

            Triple("Calculus & Integration", 2024, 36.0),
            Triple("Mechanics & Rotational", 2024, 28.0),
            Triple("Electrostatics & Magnetism", 2024, 20.0),
            Triple("Chemical Kinetics", 2024, 12.0),
            Triple("Modern Physics", 2024, 4.0)
        )

        return records.mapIndexed { idx, (topic, year, marks) ->
            DataPoint(
                datasetId = datasetId,
                timestamp = year.toLong(),
                value = marks,
                label = topic,
                extraJson = """{"year":$year,"topic":"$topic"}"""
            )
        }
    }

    /**
     * Monthly Revenue / Sales figures over 36 consecutive months showing upward trend and Q4 seasonality.
     */
    fun getSalesSample(datasetId: Long): List<DataPoint> {
        val revenues = listOf(
            120.0, 115.0, 130.0, 125.0, 140.0, 135.0, 150.0, 145.0, 160.0, 175.0, 190.0, 230.0,
            150.0, 145.0, 165.0, 160.0, 180.0, 175.0, 195.0, 190.0, 210.0, 225.0, 250.0, 310.0,
            200.0, 195.0, 220.0, 215.0, 240.0, 235.0, 260.0, 255.0, 280.0, 300.0, 330.0, 420.0
        )
        val now = System.currentTimeMillis()
        val monthMillis = 30L * 86400000L

        return revenues.mapIndexed { idx, rev ->
            DataPoint(
                datasetId = datasetId,
                timestamp = now - (revenues.size - idx) * monthMillis,
                value = rev,
                label = "M${idx + 1}"
            )
        }
    }

    /**
     * Daily Weather Temperature records (30 days).
     */
    fun getWeatherSample(datasetId: Long): List<DataPoint> {
        val temps = listOf(
            22.5, 23.0, 22.8, 24.1, 25.0, 24.5, 23.8, 24.2, 25.6, 26.1,
            25.8, 26.5, 27.0, 26.8, 27.4, 28.0, 27.5, 28.2, 28.9, 29.5,
            29.1, 29.8, 30.2, 30.0, 30.5, 31.0, 30.8, 31.5, 32.0, 31.8
        )
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L

        return temps.mapIndexed { idx, temp ->
            DataPoint(
                datasetId = datasetId,
                timestamp = now - (temps.size - idx) * dayMillis,
                value = temp,
                label = "Day ${idx + 1}"
            )
        }
    }
}
