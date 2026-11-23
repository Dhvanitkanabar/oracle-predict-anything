package com.example.oraclepredictanything.ui.prediction

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oraclepredictanything.data.model.PredictionCategory
import com.example.oraclepredictanything.engine.BacktestResult
import com.example.oraclepredictanything.engine.categorical.TopicPrediction
import com.example.oraclepredictanything.engine.technical.TradingAnalysis
import com.example.oraclepredictanything.ui.common.*
import com.example.oraclepredictanything.viewmodel.FullPredictionResult
import com.example.oraclepredictanything.viewmodel.PredictionUiState
import com.example.oraclepredictanything.viewmodel.PredictionViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
    datasetId: Long,
    onBack: () -> Unit,
    viewModel: PredictionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Forecast", "Model Comparison", "Explanation & Audit")

    LaunchedEffect(datasetId) {
        viewModel.runPrediction(datasetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prediction Analysis",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is PredictionUiState.Success) {
                        val result = (uiState as PredictionUiState.Success).result
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Oracle Forecast: ${result.dataset.name}")
                                putExtra(Intent.EXTRA_TEXT, buildShareText(result))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Forecast"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is PredictionUiState.Idle, is PredictionUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Evaluating Statistical Algorithms…",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fitting SMA, Holt's Trend, Holt-Winters, and Regression models with cross-validated backtesting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                is PredictionUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.runPrediction(datasetId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PredictionUiState.Success -> {
                    val result = state.result
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Key Prediction Card with Confidence Gauge
                        item {
                            KeyPredictionCard(result)
                        }

                        // 2. Tab Navigation
                        item {
                            TabRow(
                                selectedTabIndex = selectedTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                                    )
                                }
                            }
                        }

                        // Tab 0: Forecast & Chart
                        if (selectedTab == 0) {
                            item {
                                ForecastChartCard(result)
                            }
                            item {
                                BacktestSummaryCard(result)
                            }
                            if (result.tradingAnalysis != null) {
                                item {
                                    TradingSignalCard(result.tradingAnalysis)
                                }
                            }
                            if (result.categoricalPredictions != null && result.dataset.category == PredictionCategory.EXAMS) {
                                item {
                                    TopicRankingCard(result.categoricalPredictions)
                                }
                            }
                        }

                        // Tab 1: Model Comparison
                        if (selectedTab == 1) {
                            item {
                                ModelComparisonCard(result.backtestResults, result.ensembleResult.weights, result.ensembleResult.bestModel)
                            }
                        }

                        // Tab 2: Explanation & Audit
                        if (selectedTab == 2) {
                            item {
                                ExplanationCard(result)
                            }
                            if (result.qualityReport.warnings.isNotEmpty()) {
                                item {
                                    QualityWarningsCard(result)
                                }
                            }
                        }

                        // Bottom disclaimer
                        item {
                            DisclaimerCard()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyPredictionCard(result: FullPredictionResult) {
    val nextValue = result.ensembleResult.predictions.firstOrNull() ?: 0.0
    val lastHistorical = result.historicalValues.lastOrNull() ?: 0.0
    val changePct = if (abs(lastHistorical) > 1e-9) ((nextValue - lastHistorical) / lastHistorical) * 100.0 else 0.0
    val changeColor = if (changePct >= 0) Color(0xFF00E676) else Color(0xFFFF5252)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Next Expected Value",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = String.format("%.2f", nextValue),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (changePct >= 0) "▲ +" else "▼ ") + String.format("%.2f%%", changePct),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = changeColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "vs current (${String.format("%.2f", lastHistorical)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ConfidenceGauge(
                confidence = result.confidenceScore,
                size = 96.dp
            )
        }
    }
}

@Composable
private fun ForecastChartCard(result: FullPredictionResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Statistical Forecast Curve",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Weighted ensemble projection with 80% confidence interval band",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        ForecastChart(
            historicalData = result.historicalValues,
            predictions = result.ensembleResult.predictions,
            lower80 = result.ensembleResult.lowerBound80,
            upper80 = result.ensembleResult.upperBound80,
            lower95 = result.ensembleResult.lowerBound95,
            upper95 = result.ensembleResult.upperBound95
        )
    }
}

@Composable
private fun BacktestSummaryCard(result: FullPredictionResult) {
    val bestModel = result.ensembleResult.bestModel
    val bt = result.backtestResults[bestModel]

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Backtest Performance Audit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Evaluated against holdout test sample to measure real-world precision.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricBox("Best Model", bestModel, MaterialTheme.colorScheme.primary)
            MetricBox("MAPE", if (bt != null) String.format("%.1f%%", bt.mape) else "N/A", MaterialTheme.colorScheme.secondary)
            MetricBox("MAE", if (bt != null) String.format("%.2f", bt.mae) else "N/A", MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun RowScope.MetricBox(label: String, value: String, color: Color) {
    Surface(
        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ModelComparisonCard(
    results: Map<String, BacktestResult>,
    weights: Map<String, Double>,
    bestModel: String
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Algorithm Accuracy Comparison",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "All 7 classical models are tested independently. Ensemble assigns weights inversely proportional to backtest error (MAPE).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))

        results.entries.sortedBy { it.value.mape }.forEach { (name, bt) ->
            val weightPct = (weights[name] ?: 0.0) * 100.0
            val isBest = name == bestModel

            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isBest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "BEST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "MAPE ${String.format("%.1f%%", bt.mape)} · Weight ${String.format("%.1f%%", weightPct)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { (weightPct / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TradingSignalCard(analysis: TradingAnalysis) {
    val signalColor = when (analysis.overallSignal) {
        "BULLISH" -> Color(0xFF00E676)
        "BEARISH" -> Color(0xFFFF5252)
        else -> Color(0xFFFFD600)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trading Momentum Signal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = signalColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = analysis.overallSignal,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = signalColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Score: ${String.format("%+.2f", analysis.compositeScore)} (-1.0 to +1.0) · Key Support: ${String.format("%.2f", analysis.supportLevel)} · Resistance: ${String.format("%.2f", analysis.resistanceLevel)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        analysis.indicators.forEach { ind ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = ind.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                val indColor = when (ind.signal) {
                    "BULLISH" -> Color(0xFF00E676)
                    "BEARISH" -> Color(0xFFFF5252)
                    else -> Color(0xFFFFD600)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = indColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = ind.signal,
                        style = MaterialTheme.typography.labelSmall,
                        color = indColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicRankingCard(predictions: List<TopicPrediction>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Exam Chapter Priority Ranking",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ranked by recency-weighted frequency, trend slope, and overdue recurrence cycle.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        predictions.take(8).forEachIndexed { idx, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "#${idx + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.topicName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (item.isOverdue) {
                            Text(
                                text = "⏰ Overdue (cycle exceeded)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD600)
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format("%.1f", item.probabilityPercent)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Exp: ${String.format("%.1f", item.expectedWeight)} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(result: FullPredictionResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Deterministic Statistical Explanation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = result.explanationText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun QualityWarningsCard(result: FullPredictionResult) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Data Quality Audit Warnings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD600)
        )
        Spacer(modifier = Modifier.height(6.dp))
        result.qualityReport.warnings.forEach { warning ->
            Text(
                text = "• $warning",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildShareText(result: FullPredictionResult): String {
    val next = result.ensembleResult.predictions.firstOrNull() ?: 0.0
    return """
        🔮 Oracle Statistical Forecast
        Dataset: ${result.dataset.name} (${result.dataset.category.displayName})
        Next Value: ${String.format("%.2f", next)}
        Model: ${result.ensembleResult.bestModel}
        Confidence: ${(result.confidenceScore * 100).toInt()}%
        Backtest MAPE: ${String.format("%.1f%%", result.backtestResults[result.ensembleResult.bestModel]?.mape ?: 0.0)}
        
        Generated 100% offline via deterministic statistical algorithms in Oracle: Predict Anything.
    """.trimIndent()
}
