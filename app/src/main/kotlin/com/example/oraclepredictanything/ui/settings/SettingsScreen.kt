package com.example.oraclepredictanything.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oraclepredictanything.ui.common.DisclaimerCard
import com.example.oraclepredictanything.ui.common.GlassCard
import com.example.oraclepredictanything.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val horizon by viewModel.defaultHorizon.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMsg = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Theme Mode
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Appearance & Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hero Dark mode optimized for OLED screens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("DARK" to "Dark", "LIGHT" to "Light", "SYSTEM" to "System").forEach { (mode, label) ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Default Forecast Horizon
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Default Forecast Horizon",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Number of future steps to project: $horizon periods",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = horizon.toFloat(),
                        onValueChange = { viewModel.setDefaultHorizon(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 29
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 step", style = MaterialTheme.typography.labelSmall)
                        Text("15 steps", style = MaterialTheme.typography.labelSmall)
                        Text("30 steps", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Data Management
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "All data is stored exclusively in local SQLite on your device. Zero cloud sync or telemetry.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Clear All Local Data & Predictions")
                    }
                }
            }

            // About Section
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "About Oracle: Predict Anything",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0 · Non-AI Statistical Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Embedded Algorithms:\n" +
                                "• Simple Moving Average (SMA)\n" +
                                "• Weighted Moving Average (WMA)\n" +
                                "• Single Exponential Smoothing (EMA)\n" +
                                "• Holt's Double Exponential Trend\n" +
                                "• Holt-Winters Seasonal Smoothing\n" +
                                "• Ordinary Least Squares Linear Regression\n" +
                                "• Degree-2 Polynomial Curve Fitting\n" +
                                "• RSI & MACD Momentum Indicators\n" +
                                "• Bollinger Bands & Stochastic Oscillator\n" +
                                "• Recency-Weighted Markov Categorical Analysis\n\n" +
                                "No API keys. No network requests. 100% offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            // Mandatory Disclaimer
            item {
                DisclaimerCard()
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear All Data?") },
                text = { Text("This will permanently remove all saved datasets, imported points, and historical predictions. This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllData {
                                showClearDialog = false
                                snackbarMsg = "All data cleared successfully."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
