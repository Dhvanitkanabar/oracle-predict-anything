package com.example.oraclepredictanything.engine

import com.example.oraclepredictanything.engine.categorical.CategoryPredictor
import com.example.oraclepredictanything.engine.categorical.TopicOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryPredictorTest {

    @Test
    fun testRecencyWeightedTopicPrediction() {
        val occurrences = listOf(
            TopicOccurrence("Topic A", 2020, 20.0),
            TopicOccurrence("Topic B", 2020, 10.0),
            TopicOccurrence("Topic A", 2021, 20.0),
            TopicOccurrence("Topic B", 2021, 10.0),
            TopicOccurrence("Topic A", 2022, 25.0),
            TopicOccurrence("Topic C", 2022, 30.0)
        )

        val predictor = CategoryPredictor()
        val predictions = predictor.predict(occurrences, currentStep = 2023)

        assertTrue(predictions.isNotEmpty())
        val top = predictions.first()
        // Probabilities should sum to approximately 100%
        val totalProb = predictions.sumOf { it.probabilityPercent }
        assertEquals(100.0, totalProb, 0.1)

        // Topics should have trends assigned
        predictions.forEach { p ->
            assertTrue(p.trend in listOf("Rising", "Stable", "Declining"))
        }
    }
}
