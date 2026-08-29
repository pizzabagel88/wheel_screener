package com.wheelscreener.presentation.ui.state

import com.wheelscreener.domain.scoring.CSPScoringResult

data class CspRankingUiState(
    val isLoading: Boolean = false,
    val candidates: List<CSPScoringResult> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val minScore: Float = 0f,
    val sortBy: CspSortOption = CspSortOption.SCORE
)

enum class CspSortOption {
    SCORE,
    LIQUIDITY,
    IV_RANK,
    DTE
}
