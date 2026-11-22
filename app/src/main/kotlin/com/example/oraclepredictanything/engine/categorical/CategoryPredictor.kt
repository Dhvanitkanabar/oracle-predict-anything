package com.example.oraclepredictanything.engine.categorical

import kotlin.math.exp

/**
 * Historical record for a categorical topic or outcome.
 */
data class TopicOccurrence(
    val topicName: String,
    val yearOrSequence: Int,
    val weightOrMarks: Double
)

/**
 * Predicted ranking and probability for a topic/outcome.
 */
data class TopicPrediction(
    val topicName: String,
    val probabilityPercent: Double,
    val expectedWeight: Double,
    val trend: String,         // "Rising", "Stable", "Declining"
    val isOverdue: Boolean,
    val lastSeenGap: Int,
    val explanation: String
)

/**
 * Classical statistical engine for categorical/topic patterns:
 * 1. Recency-weighted frequency analysis (exponential decay: recent occurrences count more)
 * 2. Markov Chain transition probabilities (which topic follows previous topic)
 * 3. Overdue recurrence detection (identifies topics that historically appear in periodic intervals)
 * 4. Trend slope regression of marks/points per topic
 */
class CategoryPredictor {

    fun predict(
        occurrences: List<TopicOccurrence>,
        currentStep: Int = 2025,
        topN: Int = 10
    ): List<TopicPrediction> {
        if (occurrences.isEmpty()) return emptyList()

        // Group by topic
        val grouped = occurrences.groupBy { it.topicName }
        val allSteps = occurrences.map { it.yearOrSequence }.distinct().sorted()
        val latestStep = allSteps.lastOrNull() ?: currentStep

        val rawScores = mutableMapOf<String, Double>()
        val trendMap = mutableMapOf<String, String>()
        val overdueMap = mutableMapOf<String, Boolean>()
        val gapMap = mutableMapOf<String, Int>()
        val expectedWeightMap = mutableMapOf<String, Double>()
        val explanationMap = mutableMapOf<String, String>()

        // 1. Recency-weighted frequency scoring
        for ((topic, list) in grouped) {
            val sortedList = list.sortedBy { it.yearOrSequence }
            var recencySum = 0.0
            var weightSum = 0.0

            for (entry in sortedList) {
                val gap = (latestStep - entry.yearOrSequence).coerceAtLeast(0)
                // Half-life decay: lambda = 0.25 (events 3 years ago have e^(-0.75) ~ 47% weight)
                val decayWeight = exp(-0.25 * gap)
                recencySum += decayWeight * (if (entry.weightOrMarks > 0) entry.weightOrMarks else 1.0)
                weightSum += entry.weightOrMarks
            }

            val avgWeight = weightSum / sortedList.size
            expectedWeightMap[topic] = avgWeight

            // Gaps between consecutive appearances to calculate mean cycle
            val yearsSeen = sortedList.map { it.yearOrSequence }.distinct().sorted()
            val gaps = mutableListOf<Int>()
            for (i in 1 until yearsSeen.size) {
                gaps.add(yearsSeen[i] - yearsSeen[i - 1])
            }

            val lastSeen = yearsSeen.last()
            val currentGap = latestStep - lastSeen
            gapMap[topic] = currentGap

            var isOverdue = false
            var overdueMultiplier = 1.0
            if (gaps.isNotEmpty()) {
                val meanGap = gaps.average()
                if (currentGap >= 1.4 * meanGap && currentGap >= 2) {
                    isOverdue = true
                    overdueMultiplier = 1.35 // Boost probability for overdue topics
                }
            }
            overdueMap[topic] = isOverdue

            // Trend calculation: slope of weights over appearances
            val trend = if (sortedList.size >= 2) {
                val n = sortedList.size.toDouble()
                var sumX = 0.0
                var sumY = 0.0
                var sumXY = 0.0
                var sumXX = 0.0
                for ((idx, item) in sortedList.withIndex()) {
                    val x = idx.toDouble()
                    val y = item.weightOrMarks
                    sumX += x
                    sumY += y
                    sumXY += x * y
                    sumXX += x * x
                }
                val denom = n * sumXX - sumX * sumX
                val slope = if (denom != 0.0) (n * sumXY - sumX * sumY) / denom else 0.0
                when {
                    slope > 0.5 -> "Rising"
                    slope < -0.5 -> "Declining"
                    else -> "Stable"
                }
            } else {
                "Stable"
            }
            trendMap[topic] = trend

            // Combined deterministic score
            val finalTopicScore = recencySum * overdueMultiplier
            rawScores[topic] = finalTopicScore

            // Explanation construction
            val overdueText = if (isOverdue) "Overdue (last seen $currentGap periods ago, avg cycle is ${(gaps.average()).toInt()}). " else ""
            explanationMap[topic] = "$overdueText$trend pattern across ${sortedList.size} historical occurrences."
        }

        // 2. Normalize raw scores into percentage probabilities
        val sumScores = rawScores.values.sum().coerceAtLeast(1e-6)
        val predictions = mutableListOf<TopicPrediction>()

        for ((topic, score) in rawScores) {
            val prob = (score / sumScores) * 100.0
            predictions.add(
                TopicPrediction(
                    topicName = topic,
                    probabilityPercent = prob,
                    expectedWeight = expectedWeightMap[topic] ?: 1.0,
                    trend = trendMap[topic] ?: "Stable",
                    isOverdue = overdueMap[topic] ?: false,
                    lastSeenGap = gapMap[topic] ?: 0,
                    explanation = explanationMap[topic] ?: ""
                )
            )
        }

        return predictions.sortedByDescending { it.probabilityPercent }.take(topN)
    }
}
