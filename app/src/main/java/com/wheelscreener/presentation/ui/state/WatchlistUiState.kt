package com.wheelscreener.presentation.ui.state

import com.wheelscreener.data.local.entity.WatchlistEntity

data class WatchlistUiState(
    val watchlist: List<WatchlistEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
