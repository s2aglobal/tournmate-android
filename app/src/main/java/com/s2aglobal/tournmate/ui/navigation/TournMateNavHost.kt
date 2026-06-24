package com.s2aglobal.tournmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.s2aglobal.tournmate.ui.screen.MainScreen
import com.s2aglobal.tournmate.ui.screen.auth.AuthGateScreen
import com.s2aglobal.tournmate.ui.screen.auth.LoginScreen
import com.s2aglobal.tournmate.ui.screen.auth.OnboardingScreen
import com.s2aglobal.tournmate.ui.screen.auth.ProfileSetupScreen
import com.s2aglobal.tournmate.ui.screen.auth.WelcomeScreen
import com.s2aglobal.tournmate.ui.screen.tournament.TournamentDetailScreen

@Composable
fun TournMateNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH_GATE,
    ) {
        composable(Routes.AUTH_GATE) {
            AuthGateScreen(
                onNavigateToWelcome = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(Routes.PROFILE_SETUP) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.WELCOME) {
            WelcomeScreen(
                onStartJourney = { navController.navigate(Routes.login(createMode = true)) },
                onLogIn = { navController.navigate(Routes.login(createMode = false)) },
                onContinueAsGuest = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.LOGIN) { backStackEntry ->
            val createMode = backStackEntry.arguments?.getString("createMode")?.toBoolean() ?: false
            LoginScreen(
                initialCreateMode = createMode,
                onSignInSuccess = { needsProfile ->
                    if (needsProfile) {
                        navController.navigate(Routes.PROFILE_SETUP) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PROFILE_SETUP) {
            ProfileSetupScreen(
                onComplete = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.PROFILE_SETUP) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            MainScreen(
                onSignOut = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onNavigateToTournamentDetail = { tournamentId ->
                    navController.navigate(Routes.tournamentDetail(tournamentId))
                },
            )
        }

        composable(Routes.TOURNAMENT_DETAIL) {
            TournamentDetailScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.MAIN) { inclusive = true }
                        }
                    }
                },
                onPlayerClick = { playerId ->
                    navController.navigate(Routes.playerProfile(playerId))
                },
            )
        }
    }
}
