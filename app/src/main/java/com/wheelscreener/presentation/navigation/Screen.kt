package com.wheelscreener.presentation.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object CspRanking : Screen("csp_ranking")
    object CcRanking : Screen("cc_ranking")
    object Watchlist : Screen("watchlist")
    object Settings : Screen("settings")
    object CandidateDetail : Screen("candidate_detail/{symbol}/{strike}/{expiration}/{type}") {
        fun createRoute(symbol: String, strike: Double, expirationEpoch: Long, type: String): String {
            return "candidate_detail/$symbol/$strike/$expirationEpoch/$type"
        }
    }
}
