package com.wheelscreener.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wheelscreener.presentation.ui.components.FlagBadge
import com.wheelscreener.presentation.ui.components.ScoreProgress
import com.wheelscreener.presentation.viewmodel.CandidateDetailViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CandidateDetailScreen(
    symbol: String,
    strike: Double,
    expirationEpoch: Long,
    type: String,
    viewModel: CandidateDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(symbol, strike, expirationEpoch, type) {
        viewModel.loadCandidate(symbol, strike, expirationEpoch, type)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$symbol - Candidate Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val scoreComponents = uiState.cspCandidate?.scoreComponents ?: uiState.ccCandidate?.scoreComponents
            val flags = uiState.cspCandidate?.flags ?: uiState.ccCandidate?.flags ?: emptyList()
            val confidence = uiState.cspCandidate?.confidence ?: uiState.ccCandidate?.confidence
            val dte = uiState.cspCandidate?.dte ?: uiState.ccCandidate?.dte ?: 0
            val bid = uiState.cspCandidate?.contract?.bid ?: uiState.ccCandidate?.contract?.bid ?: 0.0
            val ask = uiState.cspCandidate?.contract?.ask ?: uiState.ccCandidate?.contract?.ask ?: 0.0
            val delta = uiState.cspCandidate?.contract?.delta ?: uiState.ccCandidate?.contract?.delta ?: 0.0
            val iv = uiState.cspCandidate?.contract?.iv ?: uiState.ccCandidate?.contract?.iv ?: 0.0
            val ivRank = uiState.cspCandidate?.contract?.ivRank ?: uiState.ccCandidate?.contract?.ivRank ?: 0.0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Key metrics card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Symbol: $symbol", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Type: $type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Strike: $${String.format("%.2f", strike)}")
                            Text("DTE: $dte days")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bid: $${String.format("%.2f", bid)}")
                            Text("Ask: $${String.format("%.2f", ask)}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Delta: ${String.format("%.2f", delta)}")
                            Text("IV: ${String.format("%.1f", iv * 100)}% (Rank: ${String.format("%.1f", ivRank)})")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Confidence: ${confidence?.name}")
                        }
                    }
                }

                // Exclusions
                val exclusion = uiState.cspCandidate?.exclusionReason ?: uiState.ccCandidate?.exclusionReason
                if (exclusion != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Exclusion: $exclusion",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = viewModel::openPaperPosition,
                    enabled = !uiState.isSavingPosition && !uiState.positionSaved,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            uiState.positionSaved -> "Paper position opened"
                            uiState.isSavingPosition -> "Opening paper position..."
                            else -> "Open paper position"
                        }
                    )
                }

                // Warnings / Flags
                if (flags.isNotEmpty()) {
                    Text("Flags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        flags.forEach { flag ->
                            FlagBadge(flag = flag)
                        }
                    }
                }

                // Scoring breakdown
                if (scoreComponents != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Score Breakdown (Total: ${String.format("%.1f", scoreComponents.compositeScore)})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Divider()
                            ScoreProgress(name = "Liquidity", score = scoreComponents.liquidityScore, maxScore = 25.0)
                            ScoreProgress(name = "IV & Events", score = scoreComponents.ivScore, maxScore = 20.0)
                            ScoreProgress(name = "Pullback", score = scoreComponents.pullbackScore, maxScore = 20.0)
                            ScoreProgress(name = "Fundamentals", score = scoreComponents.fundamentalScore, maxScore = 15.0)
                            ScoreProgress(name = "Technical Trend", score = scoreComponents.technicalScore, maxScore = 10.0)
                            ScoreProgress(name = "Diversification", score = scoreComponents.diversificationScore, maxScore = 10.0)
                        }
                    }
                }
            }
        }
    }
}
