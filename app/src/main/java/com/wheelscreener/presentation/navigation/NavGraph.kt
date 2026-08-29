package com.wheelscreener.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.wheelscreener.presentation.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToCsp = { navController.navigate(Screen.CspRanking.route) },
                onNavigateToCc = { navController.navigate(Screen.CcRanking.route) },
                onNavigateToWatchlist = { navController.navigate(Screen.Watchlist.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLedger = { navController.navigate(Screen.PositionLedger.route) }
            )
        }
        
        composable(Screen.CspRanking.route) {
            CspRankingScreen(
                onNavigateToDetail = { symbol, strike, expiration, type ->
                    navController.navigate(Screen.CandidateDetail.createRoute(symbol, strike, expiration, type))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.CcRanking.route) {
            CcRankingScreen(
                onNavigateToDetail = { symbol, strike, expiration, type ->
                    navController.navigate(Screen.CandidateDetail.createRoute(symbol, strike, expiration, type))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PositionLedger.route) {
            PositionLedgerScreen(onBack = { navController.popBackStack() })
        }
        
        composable(
            route = Screen.CandidateDetail.route,
            arguments = listOf(
                navArgument("symbol") { type = NavType.StringType },
                navArgument("strike") { type = NavType.FloatType },
                navArgument("expiration") { type = NavType.LongType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.getString("symbol") ?: ""
            val strike = backStackEntry.arguments?.getFloat("strike")?.toDouble() ?: 0.0
            val expiration = backStackEntry.arguments?.getLong("expiration") ?: 0L
            val type = backStackEntry.arguments?.getString("type") ?: "PUT"
            
            CandidateDetailScreen(
                symbol = symbol,
                strike = strike,
                expirationEpoch = expiration,
                type = type,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
