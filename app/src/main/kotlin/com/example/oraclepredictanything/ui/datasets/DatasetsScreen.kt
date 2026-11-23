package com.example.oraclepredictanything.ui.datasets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oraclepredictanything.data.model.Dataset
import com.example.oraclepredictanything.data.model.PredictionCategory
import com.example.oraclepredictanything.ui.common.EmptyState
import com.example.oraclepredictanything.ui.common.GlassCard
import com.example.oraclepredictanything.viewmodel.DatasetViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetsScreen(
    onDatasetSelected: (Long) -> Unit,
    initialCategory: PredictionCategory? = null,
    viewModel: DatasetViewModel = viewModel()
) {
    val datasets by viewModel.datasets.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Datasets",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Dataset")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (datasets.isEmpty()) {
                EmptyState(
                    icon = "📁",
                    title = "No Datasets Yet",
                    message = "Create a dataset to enter historical series or import CSV data for prediction.",
                    actionLabel = "Create Dataset",
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(datasets, key = { it.id }) { dataset ->
                        DatasetListItem(
                            dataset = dataset,
                            onClick = { onDatasetSelected(dataset.id) },
                            onDelete = { viewModel.deleteDataset(dataset) }
                        )
                    }
                }
            }

            if (showCreateDialog) {
                CreateDatasetDialog(
                    initialCategory = initialCategory ?: PredictionCategory.TRADING,
                    onDismiss = { showCreateDialog = false },
                    onConfirm = { name, cat ->
                        viewModel.createDataset(name, cat) { newId ->
                            showCreateDialog = false
                            onDatasetSelected(newId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DatasetListItem(
    dataset: Dataset,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val dateStr = remember(dataset.updatedAt) { dateFormat.format(Date(dataset.updatedAt)) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = dataset.category.icon, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = dataset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${dataset.category.displayName} · Updated $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
