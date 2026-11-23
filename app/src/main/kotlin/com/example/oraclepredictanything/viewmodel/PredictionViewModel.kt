package com.example.oraclepredictanything.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oraclepredictanything.OracleApp
import com.example.oraclepredictanything.data.model.Dataset
import com.example.oraclepredictanything.data.model.PredictionCategory
import com.example.oraclepredictanything.data.model.PredictionResultEntity
import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.EnsembleResult
import com.example.oraclepredictanything.engine.PredictorRegistry
import com.example.oraclepredictanything.engine.categorical.CategoryPredictor
import com.example.oraclepredictanything.engine.categorical.TopicOccurrence
import com.example.oraclepredictanything.engine.categorical.TopicPrediction
import com.example.oraclepredictanything.engine.explanation.ExplanationGenerator
import com.example.oraclepredictanything.engine.quality.DataQualityChecker
import com.example.oraclepredictanything.engine.quality.QualityReport
import com.example.oraclepredictanything.engine.technical.TradingAnalysis
import com.example.oraclepredictanything.engine.technical.TradingSignalGenerator
import com.example.oraclepredictanything.util.JsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface PredictionUiState {
    data object Idle : PredictionUiState
    data object Loading : PredictionUiState
    data class Success(val result: FullPredictionResult) : PredictionUiState
    data class Error(val message: String) : PredictionUiState
}

data class FullPredictionResult(
    val dataset: Dataset,
    val historicalValues: List<Double>,
    val ensembleResult: EnsembleResult,
    val backtestResults: Map<String, BacktestResult>,
    val qualityReport: QualityReport,
    val tradingAnalysis: TradingAnalysis?,
    val categoricalPredictions: List<TopicPrediction>?,
    val explanationText: String,
    val confidenceScore: Double
)

class PredictionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = getApplication<OracleApp>().repository

    private val _uiState = MutableStateFlow<PredictionUiState>(PredictionUiState.Idle)
    val uiState: StateFlow<PredictionUiState> = _uiState.asStateFlow()

    fun runPrediction(datasetId: Long, horizon: Int = 7) {
        _uiState.value = PredictionUiState.Loading

        viewModelScope.launch {
            try {
                val dataset = repository.getDatasetById(datasetId)
                if (dataset == null) {
                    _uiState.value = PredictionUiState.Error("Dataset not found")
                    return@launch
                }

                val dataPoints = repository.getDataPointsList(datasetId)
                if (dataPoints.isEmpty()) {
                    _uiState.value = PredictionUiState.Error(
                        "No data points found. Add or import data first to generate predictions."
                    )
                    return@launch
                }

                // Execute all deterministic computations on Dispatchers.Default
                val result = withContext(Dispatchers.Default) {
                    val rawValues = dataPoints.map { it.value }

                    // 1. Data Quality Checks
                    val quality = DataQualityChecker.check(rawValues, dataset.category.minDataPoints)

                    // 2. Numeric Time-Series Ensemble & Backtesting
                    val backtests = PredictorRegistry.backtestAll(rawValues)
                    val ensemble = PredictorRegistry.ensemble(rawValues, horizon)

                    // 3. Technical Indicators (for trading)
                    val tradingAnalysis = if (dataset.category == PredictionCategory.TRADING) {
                        val highs = mutableListOf<Double>()
                        val lows = mutableListOf<Double>()
                        for (dp in dataPoints) {
                            if (dp.extraJson.isNotBlank()) {
                                try {
                                    val obj = JSONObject(dp.extraJson)
                                    highs.add(obj.optDouble("high", dp.value))
                                    lows.add(obj.optDouble("low", dp.value))
                                } catch (e: Exception) {
                                    highs.add(dp.value)
                                    lows.add(dp.value)
                                }
                            } else {
                                highs.add(dp.value)
                                lows.add(dp.value)
                            }
                        }
                        TradingSignalGenerator().analyze(rawValues, highs, lows)
                    } else null

                    // 4. Categorical / Topic Analysis (for exams or labelled topics)
                    val categoricalPredictions = if (dataset.category == PredictionCategory.EXAMS || dataPoints.any { it.label.isNotBlank() }) {
                        val occurrences = dataPoints.mapIndexed { idx, dp ->
                            val yearOrSeq = if (dp.timestamp > 3000) (2015 + (idx % 10)) else dp.timestamp.toInt()
                            TopicOccurrence(
                                topicName = dp.label.ifBlank { "Item ${idx + 1}" },
                                yearOrSequence = yearOrSeq,
                                weightOrMarks = dp.value
                            )
                        }
                        CategoryPredictor().predict(occurrences)
                    } else null

                    // 5. Deterministic Rule-Based Explanations
                    val explanation = when {
                        tradingAnalysis != null -> ExplanationGenerator.generateTrading(tradingAnalysis) +
                                "\n\n" + ExplanationGenerator.generateNumeric(rawValues, ensemble, quality)
                        categoricalPredictions != null && dataset.category == PredictionCategory.EXAMS ->
                            ExplanationGenerator.generateCategorical(categoricalPredictions) +
                                    "\n\n" + ExplanationGenerator.generateNumeric(rawValues, ensemble, quality)
                        else -> ExplanationGenerator.generateNumeric(rawValues, ensemble, quality)
                    }

                    // Combined confidence score
                    val bestMape = backtests[ensemble.bestModel]?.mape ?: 15.0
                    val accuracyFactor = (100.0 - bestMape.coerceAtMost(80.0)) / 100.0
                    val finalConfidence = (accuracyFactor * quality.adjustedConfidence).coerceIn(0.15, 0.98)

                    // 6. Save prediction snapshot to database
                    val entity = PredictionResultEntity(
                        datasetId = dataset.id,
                        datasetName = dataset.name,
                        createdAt = System.currentTimeMillis(),
                        forecastJson = JsonUtils.serializeForecast(ensemble),
                        modelUsed = ensemble.bestModel,
                        mae = backtests[ensemble.bestModel]?.mae ?: 0.0,
                        rmse = backtests[ensemble.bestModel]?.rmse ?: 0.0,
                        mape = bestMape,
                        confidence = finalConfidence,
                        explanationText = explanation,
                        overallSignal = tradingAnalysis?.overallSignal ?: ""
                    )
                    repository.savePredictionResult(entity)

                    FullPredictionResult(
                        dataset = dataset,
                        historicalValues = rawValues,
                        ensembleResult = ensemble,
                        backtestResults = backtests,
                        qualityReport = quality,
                        tradingAnalysis = tradingAnalysis,
                        categoricalPredictions = categoricalPredictions,
                        explanationText = explanation,
                        confidenceScore = finalConfidence
                    )
                }

                _uiState.value = PredictionUiState.Success(result)

            } catch (e: Exception) {
                _uiState.value = PredictionUiState.Error("Prediction failed: ${e.message}")
            }
        }
    }
}
