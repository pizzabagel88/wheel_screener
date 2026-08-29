package com.wheelscreener.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wheelscreener.domain.model.PaperPositionAnalytics
import com.wheelscreener.domain.model.PaperPositionStatus
import com.wheelscreener.presentation.viewmodel.PositionLedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionLedgerScreen(
    viewModel: PositionLedgerViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showExport by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Paper Position Ledger") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = { Button(onClick = { viewModel.exportCsv(); showExport = true }) { Text("Export CSV") } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.reminders.isNotEmpty()) item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Reminders", fontWeight = FontWeight.Bold)
                        state.reminders.forEach { Text("• ${it.message}") }
                    }
                }
            }
            items(state.positions, key = { it.id }) { position ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${position.underlyingSymbol} ${position.optionType} $${"%.2f".format(position.strike)}", fontWeight = FontWeight.Bold)
                            Text(position.status)
                        }
                        Text("Credit: $${"%.2f".format(position.entryCredit)} · Contracts: ${position.quantity}")
                        PaperPositionAnalytics.realizedPnl(position)?.let { Text("Realized P&L: $${"%.2f".format(it)}") }
                        if (position.status == PaperPositionStatus.OPEN.name) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.closeWorthless(position.id) }) { Text("Close @ $0") }
                                Button(onClick = { viewModel.markRolled(position.id) }) { Text("Mark rolled") }
                                Button(onClick = { viewModel.markAssigned(position.id) }) { Text("Mark assigned") }
                            }
                        }
                    }
                }
            }
            if (state.positions.isEmpty()) item { Text("No paper positions yet. Open one from a candidate detail screen.") }
        }
    }
    if (showExport && state.exportedCsv != null) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            confirmButton = { Button(onClick = { showExport = false }) { Text("Done") } },
            title = { Text("Position CSV") },
            text = { SelectionContainer { Text(state.exportedCsv!!) } }
        )
    }
}
