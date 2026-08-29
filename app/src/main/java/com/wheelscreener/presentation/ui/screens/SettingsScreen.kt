package com.wheelscreener.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wheelscreener.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.config

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Strategy Configurations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveConfig() }) {
                        Text(
                            text = if (uiState.isSaving) "Saving..." else "Save",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.error != null) {
                Text(text = uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
            }
            if (uiState.isSaved) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text("Configuration saved successfully!", modifier = Modifier.padding(16.dp))
                }
            }

            // Expiration (DTE) Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Days to Expiration (DTE)", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Min DTE: ${config.dteMin} days")
                        Text("Max DTE: ${config.dteMax} days")
                    }
                    RangeSlider(
                        value = config.dteMin.toFloat()..config.dteMax.toFloat(),
                        onValueChange = { range ->
                            viewModel.updateConfig(config.copy(dteMin = range.start.toInt(), dteMax = range.endInclusive.toInt()))
                        },
                        valueRange = 0f..90f
                    )
                }
            }

            // CSP Delta range Section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CSP Target Delta (Core)", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Min Delta: ${String.format("%.2f", config.cspDeltaMinCore)}")
                        Text("Max Delta: ${String.format("%.2f", config.cspDeltaMaxCore)}")
                    }
                    RangeSlider(
                        value = config.cspDeltaMinCore.toFloat()..config.cspDeltaMaxCore.toFloat(),
                        onValueChange = { range ->
                            viewModel.updateConfig(config.copy(cspDeltaMinCore = range.start.toDouble(), cspDeltaMaxCore = range.endInclusive.toDouble()))
                        },
                        valueRange = 0.05f..0.50f
                    )
                }
            }

            // Scoring weights section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Scoring Component Weights", style = MaterialTheme.typography.titleMedium)
                    
                    Text("Liquidity: ${config.liquidityWeight.toInt()}")
                    Slider(
                        value = config.liquidityWeight.toFloat(),
                        onValueChange = { viewModel.updateConfig(config.copy(liquidityWeight = it.toDouble())) },
                        valueRange = 0f..50f
                    )
                    
                    Text("IV Opportunity: ${config.ivWeight.toInt()}")
                    Slider(
                        value = config.ivWeight.toFloat(),
                        onValueChange = { viewModel.updateConfig(config.copy(ivWeight = it.toDouble())) },
                        valueRange = 0f..50f
                    )

                    Text("Pullback quality: ${config.pullbackWeight.toInt()}")
                    Slider(
                        value = config.pullbackWeight.toFloat(),
                        onValueChange = { viewModel.updateConfig(config.copy(pullbackWeight = it.toDouble())) },
                        valueRange = 0f..50f
                    )
                }
            }

            // Scheduling section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily Scan Schedule", style = MaterialTheme.typography.titleMedium)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable daily scan")
                        Switch(
                            checked = config.scanEnabled,
                            onCheckedChange = { viewModel.updateConfig(config.copy(scanEnabled = it)) }
                        )
                    }
                    
                    if (config.scanEnabled) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Scan time:")
                            OutlinedTextField(
                                value = String.format("%02d", config.scanHourOfDay),
                                onValueChange = { 
                                    val hour = it.toIntOrNull() ?: 9
                                    viewModel.updateConfig(config.copy(scanHourOfDay = hour.coerceIn(0, 23)))
                                },
                                label = { Text("Hour (0-23)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = String.format("%02d", config.scanMinute),
                                onValueChange = { 
                                    val minute = it.toIntOrNull() ?: 30
                                    viewModel.updateConfig(config.copy(scanMinute = minute.coerceIn(0, 59)))
                                },
                                label = { Text("Minute (0-59)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Weekdays only")
                            Switch(
                                checked = config.scanWeekdaysOnly,
                                onCheckedChange = { viewModel.updateConfig(config.copy(scanWeekdaysOnly = it)) }
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Notify on scan complete")
                            Switch(
                                checked = config.notifyOnScanComplete,
                                onCheckedChange = { viewModel.updateConfig(config.copy(notifyOnScanComplete = it)) }
                            )
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Notify on high-quality candidates")
                            Switch(
                                checked = config.notifyOnHighQualityCandidates,
                                onCheckedChange = { viewModel.updateConfig(config.copy(notifyOnHighQualityCandidates = it)) }
                            )
                        }
                        
                        if (config.notifyOnHighQualityCandidates) {
                            Text("Min score for notification: ${config.minScoreForNotification.toInt()}")
                            Slider(
                                value = config.minScoreForNotification.toFloat(),
                                onValueChange = { viewModel.updateConfig(config.copy(minScoreForNotification = it.toDouble())) },
                                valueRange = 0f..100f
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.resetToDefault() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}
