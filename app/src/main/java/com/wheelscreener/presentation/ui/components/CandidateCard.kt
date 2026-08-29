package com.wheelscreener.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wheelscreener.domain.scoring.CSPScoringResult
import com.wheelscreener.domain.scoring.CCScoringResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandidateCard(
    cspResult: CSPScoringResult? = null,
    ccResult: CCScoringResult? = null,
    onClick: () -> Unit
) {
    val symbol = cspResult?.underlying?.symbol ?: ccResult?.underlying?.symbol ?: ""
    val underlyingPrice = cspResult?.underlying?.price ?: ccResult?.underlying?.price ?: 0.0
    val strike = cspResult?.contract?.strike ?: ccResult?.contract?.strike ?: 0.0
    val dte = cspResult?.dte ?: ccResult?.dte ?: 0
    val delta = cspResult?.contract?.delta ?: ccResult?.contract?.delta ?: 0.0
    val bid = cspResult?.contract?.bid ?: ccResult?.contract?.bid ?: 0.0
    val ask = cspResult?.contract?.ask ?: ccResult?.contract?.ask ?: 0.0
    val score = cspResult?.scoreComponents?.compositeScore ?: ccResult?.scoreComponents?.compositeScore ?: 0.0
    val flags = cspResult?.flags ?: ccResult?.flags ?: emptyList()
    val isCore = cspResult?.isCore ?: ccResult?.isCore ?: false
    val type = if (cspResult != null) "CSP" else "CC"
    
    val scoreColor = when {
        score >= 75.0 -> Color(0xFF4CAF50)
        score >= 50.0 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Symbol, Expiration details, Composite Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(type) },
                            modifier = Modifier.height(24.dp)
                        )
                        if (isCore) {
                            Spacer(modifier = Modifier.width(4.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Core") },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    Text(
                        text = "Underlying: $${String.format("%.2f", underlyingPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Composite Score badge
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.1f", score),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Strike", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$${String.format("%.2f", strike)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "DTE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$dte", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Delta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = String.format("%.2f", delta), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Bid/Ask", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$${String.format("%.2f", bid)} / $${String.format("%.2f", ask)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                
                // Extra covered call details
                if (ccResult != null) {
                    Column {
                        Text(text = "Exit Return", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format("%.2f", ccResult.returnIfCalled * 100)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Flags
            if (flags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    flags.forEach { flag ->
                        FlagBadge(flag = flag)
                    }
                }
            }
        }
    }
}
