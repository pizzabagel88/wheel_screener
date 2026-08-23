package com.wheelscreener.domain.model

import kotlinx.datetime.Instant

/**
 * Represents an option chain for a symbol
 */
data class OptionChain(
    val symbol: String,
    val underlyingPrice: Double,
    val contracts: List<OptionContract>,
    val lastUpdate: Instant,
    val isStale: Boolean = false
)