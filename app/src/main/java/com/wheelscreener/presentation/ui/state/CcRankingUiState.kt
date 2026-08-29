package com.wheelscreener.presentation.ui.state

import com.wheelscreener.domain.scoring.CCScoringResult

data class CcRankingUiState(
    val isLoading: Boolean = false,
    val candidates: List<CCScoringResult> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val minScore: Float = 0f,
    val sortBy: CcSortOption = CcSortOption.SCORE
)

enum class CcSortOption {
    SCORE,
    RETURN_IF_CALLED,
    DTE,
    IV_RANK
}
