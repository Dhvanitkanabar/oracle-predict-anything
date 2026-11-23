package com.example.oraclepredictanything.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oraclepredictanything.data.model.PredictionResultEntity
import com.example.oraclepredictanything.ui.common.EmptyState
import com.example.oraclepredictanything.ui.common.GlassCard
import com.example.oraclepredictanything.ui.common.MetricBadge
import com.example.oraclepredictanything.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onPredictionSelected: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val predictions by viewModel.predictions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val filters = listOf(
        "ALL" to "All Predictions",
        "TRADING" to "Trading Signals",
        "HIGH_CONFIDENCE" to "High Confidence (≥70%)"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prediction History",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by dataset or model name…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { viewModel.setFilter(key) },
                        label = { Text(label) }
                    )
                }
            }

            // Results List
            if (predictions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = "📜",
                        title = "No Predictions Found",
                        message = if (searchQuery.isNotBlank()) "No records match your search filter." else "Generate your first forecast from the Datasets tab."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(predictions, key = { it.id }) { item ->
                        HistoryPredictionItem(
                            item = item,
                            onClick = { onPredictionSelected(item.datasetId) },
                            onDelete = { viewModel.deletePrediction(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPredictionItem(
    item: PredictionResultEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }
    val dateStr = remember(item.createdAt) { dateFormat.format(Date(item.createdAt)) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.datasetName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$dateStr · Best Model: ${item.modelUsed}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricBadge(
                label = "Confidence",
                value = "${(item.confidence * 100).toInt()}%",
                color = if (item.confidence >= 0.7) Color(0xFF00E676) else MaterialTheme.colorScheme.secondary
            )
            MetricBadge(
                label = "MAPE",
                value = String.format("%.1f%%", item.mape),
                color = MaterialTheme.colorScheme.primary
            )
            if (item.overallSignal.isNotBlank()) {
                val sigColor = when (item.overallSignal) {
                    "BULLISH" -> Color(0xFF00E676)
                    "BEARISH" -> Color(0xFFFF5252)
                    else -> Color(0xFFFFD600)
                }
                MetricBadge(
                    label = "Signal",
                    value = item.overallSignal,
                    color = sigColor
                )
            }
        }
    }
}
