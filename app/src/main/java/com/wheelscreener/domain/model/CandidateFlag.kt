package com.wheelscreener.domain.model

/**
 * Flags that can be applied to candidates
 */
enum class CandidateFlag {
    // Trend flags
    BELOW_200_SMA,
    DOWNTREND_RISK, // 20-day SMA < 50-day SMA
    POSSIBLE_TREND_BREAKDOWN, // Down >20% over 20 days
    
    // Pullback flags
    SHARP_RECENT_BREAKDOWN,
    BROAD_MARKET_LED_PULLBACK,
    SYMBOL_SPECIFIC_PULLBACK,
    
    // Liquidity flags
    LOW_OPEN_INTEREST,
    LOW_VOLUME,
    WIDE_SPREAD,
    NO_WEEKLY_EXPIRATION,
    
    // Event flags
    EARNINGS_IN_EXPIRATION,
    MAJOR_BINARY_EVENT,
    DIVIDEND_ASSIGNMENT_RISK,
    
    // Fundamental flags
    NEGATIVE_FREE_CASH_FLOW,
    HIGH_NET_DEBT,
    BELOW_MARKET_CAP_THRESHOLD,
    
    // IV flags
    HIGH_IV_RANK, // IV rank > 65
    IV_DATA_MISSING,
    
    // Data quality flags
    STALE_QUOTE,
    EVENT_DATA_MISSING,
    FUNDAMENTAL_DATA_MISSING,
    UNRELIABLE_DATA
}