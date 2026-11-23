package com.example.oraclepredictanything.ui.dataentry

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oraclepredictanything.data.model.DataPoint
import com.example.oraclepredictanything.data.model.PredictionCategory
import com.example.oraclepredictanything.ui.common.EmptyState
import com.example.oraclepredictanything.ui.common.GlassCard
import com.example.oraclepredictanything.util.CsvParser
import com.example.oraclepredictanything.viewmodel.DataEntryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataEntryScreen(
    datasetId: Long,
    onBack: () -> Unit,
    onRunPrediction: (Long) -> Unit,
    viewModel: DataEntryViewModel = viewModel()
) {
    val context = LocalContext.current
    val dataset by viewModel.currentDataset.collectAsState()
    val dataPoints by viewModel.dataPoints.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Manual", "Import CSV", "Paste Data", "Samples")

    LaunchedEffect(datasetId) {
        viewModel.loadDataset(datasetId)
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (msg.contains("removed")) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = dataset?.name ?: "Dataset",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dataset?.category?.displayName ?: ""} · ${dataPoints.size} points",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (dataPoints.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onRunPrediction(datasetId) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Run Prediction") },
                    text = { Text("Generate Prediction", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> ManualEntryTab(
                        category = dataset?.category ?: PredictionCategory.CUSTOM,
                        onAdd = { value, label -> viewModel.addDataPoint(value, label) }
                    )
                    1 -> CsvImportTab(
                        onContentRead = { csv -> viewModel.importCsv(csv) }
                    )
                    2 -> PasteDataTab(
                        onPaste = { text -> viewModel.importCsv(text) }
                    )
                    3 -> SampleDataTab(
                        category = dataset?.category ?: PredictionCategory.TRADING,
                        onLoadSample = { viewModel.loadSampleData() }
                    )
                }
            }

            // Bottom List of Current Data Points
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Series (${dataPoints.size} points)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val minPts = dataset?.category?.minDataPoints ?: 5
                val statusText = if (dataPoints.size >= minPts) "✓ Ready for prediction" else "Need ${minPts - dataPoints.size} more"
                val statusColor = if (dataPoints.size >= minPts) Color(0xFF00E676) else Color(0xFFFFD600)
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (dataPoints.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No points added yet. Use any tab above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    itemsIndexed(dataPoints, key = { _, dp -> dp.id }) { index, point ->
                        DataPointRow(
                            index = index + 1,
                            point = point,
                            onDelete = { viewModel.deleteDataPoint(point) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualEntryTab(
    category: PredictionCategory,
    onAdd: (Double, String) -> Unit
) {
    var valueStr by remember { mutableStateOf("") }
    var labelStr by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = valueStr,
                onValueChange = { valueStr = it },
                label = { Text("Value (Numeric)") },
                placeholder = { Text("e.g. 2450.50") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = labelStr,
                onValueChange = { labelStr = it },
                label = { Text("Label / Date") },
                placeholder = { Text(if (category == PredictionCategory.EXAMS) "Chapter" else "e.g. Day 1") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val num = valueStr.toDoubleOrNull()
                if (num != null) {
                    onAdd(num, labelStr)
                    valueStr = ""
                    labelStr = ""
                }
            },
            enabled = valueStr.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Data Point")
        }
    }
}

@Composable
private fun CsvImportTab(
    onContentRead: (String) -> Int
) {
    val context = LocalContext.current
    var previewResult by remember { mutableStateOf<String?>(null) }
    var parsedCount by remember { mutableIntStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    previewResult = text
                }
            } catch (e: Exception) {
                // Ignore read errors
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📁", fontSize = 42.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Import Historical CSV / TSV",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Auto-detects numeric series and timestamp columns.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { filePickerLauncher.launch("*/*") },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Browse Device File")
        }

        previewResult?.let { csv ->
            Spacer(modifier = Modifier.height(12.dp))
            val preview = CsvParser.parse(csv)
            Text(
                text = "Detected ${preview.rows.size} rows with headers: ${preview.headers.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    parsedCount = onContentRead(csv)
                    previewResult = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Confirm Import (${preview.rows.size} rows)")
            }
        }
    }
}

@Composable
private fun PasteDataTab(
    onPaste: (String) -> Int
) {
    var rawText by remember { mutableStateOf("") }
    var importedCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            label = { Text("Paste Data Values") },
            placeholder = { Text("Paste numbers separated by commas, tabs, or new lines:\ne.g.\n100.5\n102.3\n105.8\n103.2") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                importedCount = onPaste(rawText)
                rawText = ""
            },
            enabled = rawText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Parse & Import Data")
        }
    }
}

@Composable
private fun SampleDataTab(
    category: PredictionCategory,
    onLoadSample: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = category.icon, fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Realistic Demo Dataset Available",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Populate this dataset with realistic historical data designed specifically for ${category.displayName}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLoadSample,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Load ${category.displayName} Demo Data")
        }
    }
}

@Composable
private fun DataPointRow(
    index: Int,
    point: DataPoint,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = String.format("%.2f", point.value),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (point.label.isNotBlank()) {
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
