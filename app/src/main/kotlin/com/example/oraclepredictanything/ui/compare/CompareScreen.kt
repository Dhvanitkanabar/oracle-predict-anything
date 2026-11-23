package com.example.oraclepredictanything.ui.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oraclepredictanything.data.model.Dataset
import com.example.oraclepredictanything.ui.common.EmptyState
import com.example.oraclepredictanything.ui.common.GlassCard
import com.example.oraclepredictanything.viewmodel.DatasetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    onCompareDatasets: (Long, Long) -> Unit,
    viewModel: DatasetViewModel = viewModel()
) {
    val datasets by viewModel.datasets.collectAsState()

    var selectedId1 by remember { mutableStateOf<Long?>(null) }
    var selectedId2 by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare Forecasts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Cross-Series Comparative Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select two datasets to compare their mathematical trend slopes and model fitness side-by-side.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (datasets.size < 2) {
                EmptyState(
                    icon = "⚖️",
                    title = "Need At Least 2 Datasets",
                    message = "Create at least two datasets in the Datasets tab to enable side-by-side comparisons."
                )
            } else {
                Text(text = "Primary Dataset:", style = MaterialTheme.typography.labelMedium)
                DatasetSelector(datasets, selectedId1) { selectedId1 = it }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Secondary Dataset:", style = MaterialTheme.typography.labelMedium)
                DatasetSelector(datasets, selectedId2) { selectedId2 = it }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val d1 = selectedId1
                        val d2 = selectedId2
                        if (d1 != null && d2 != null) {
                            onCompareDatasets(d1, d2)
                        }
                    },
                    enabled = selectedId1 != null && selectedId2 != null && selectedId1 != selectedId2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Compare Models & Slopes")
                }
            }
        }
    }
}

@Composable
private fun DatasetSelector(
    datasets: List<Dataset>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = datasets.firstOrNull { it.id == selectedId }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(current?.let { "${it.category.icon} ${it.name}" } ?: "Select a dataset…")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            datasets.forEach { d ->
                DropdownMenuItem(
                    text = { Text("${d.category.icon} ${d.name}") },
                    onClick = {
                        onSelect(d.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
