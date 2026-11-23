package com.example.oraclepredictanything.data.model

/**
 * Supported prediction domains with domain-specific metadata.
 */
enum class PredictionCategory(
    val displayName: String,
    val icon: String,
    val description: String,
    val minDataPoints: Int,
    val defaultFields: List<String>
) {
    TRADING(
        displayName = "Trading / Stocks",
        icon = "📈",
        description = "Price series forecasting with classical technical indicators and momentum analysis.",
        minDataPoints = 15,
        defaultFields = listOf("Date", "Close", "Open", "High", "Low", "Volume")
    ),
    EXAMS(
        displayName = "Exam Predictions",
        icon = "📚",
        description = "Topic and chapter weightage forecasting with overdue recurrence detection (e.g. JEE/NEET).",
        minDataPoints = 5,
        defaultFields = listOf("Year", "Chapter / Topic", "Marks / Weightage")
    ),
    SALES(
        displayName = "Sales / Revenue",
        icon = "💰",
        description = "Revenue and sales volume forecasting with trend regression and seasonal decomposition.",
        minDataPoints = 8,
        defaultFields = listOf("Period", "Revenue")
    ),
    SPORTS(
        displayName = "Sports & Match Scores",
        icon = "⚽",
        description = "Sequential score forecasting and performance trend analysis.",
        minDataPoints = 6,
        defaultFields = listOf("Match #", "Score / Metric")
    ),
    WEATHER(
        displayName = "Weather & Climate",
        icon = "🌤️",
        description = "Temperature, rainfall, and climate metric forecasting using cyclic smoothing.",
        minDataPoints = 10,
        defaultFields = listOf("Date", "Measurement")
    ),
    PERSONAL(
        displayName = "Personal Metrics",
        icon = "📊",
        description = "Study hours, weight, expenses, habits, or fitness milestones.",
        minDataPoints = 7,
        defaultFields = listOf("Date", "Value")
    ),
    CUSTOM(
        displayName = "Custom Series",
        icon = "⚙️",
        description = "User-defined statistical series with flexible fields.",
        minDataPoints = 5,
        defaultFields = listOf("Index", "Value")
    );

    companion object {
        fun fromName(name: String): PredictionCategory {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: CUSTOM
        }
    }
}
