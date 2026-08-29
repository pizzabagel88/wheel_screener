package com.wheelscreener.presentation.ui.state

import com.wheelscreener.domain.scoring.CSPScoringResult
import com.wheelscreener.domain.scoring.CCScoringResult

data class CandidateDetailUiState(
    val isLoading: Boolean = false,
    val cspCandidate: CSPScoringResult? = null,
    val ccCandidate: CCScoringResult? = null,
    val error: String? = null,
    val isSavedInWatchlist: Boolean = false,
    val isSavingPosition: Boolean = false,
    val positionSaved: Boolean = false
)
