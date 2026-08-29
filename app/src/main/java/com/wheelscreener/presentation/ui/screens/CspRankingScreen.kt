package com.wheelscreener.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wheelscreener.presentation.ui.components.CandidateCard
import com.wheelscreener.presentation.ui.state.CspSortOption
import com.wheelscreener.presentation.viewmodel.CspRankingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CspRankingScreen(
    viewModel: CspRankingViewModel = hiltViewModel(),
    onNavigateToDetail: (String, Double, Long, String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val candidates = viewModel.getFilteredAndSortedCandidates()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash-Secured Puts (CSP)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.runScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Scan")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search and filters
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by symbol...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Min Score: ${uiState.minScore.toInt()}")
                Slider(
                    value = uiState.minScore,
                    onValueChange = { viewModel.setMinScore(it) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Sort Options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort by:", style = MaterialTheme.typography.bodyMedium)
                CspSortOption.values().forEach { option ->
                    FilterChip(
                        selected = uiState.sortBy == option,
                        onClick = { viewModel.setSortBy(option) },
                        label = { Text(option.name) }
                    )
                }
            }

            Divider()

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (candidates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching candidates found.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates) { candidate ->
                        CandidateCard(
                            cspResult = candidate,
                            onClick = {
                                onNavigateToDetail(
                                    candidate.underlying.symbol,
                                    candidate.contract.strike,
                                    candidate.contract.expiration.toEpochMilliseconds(),
                                    "PUT"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
