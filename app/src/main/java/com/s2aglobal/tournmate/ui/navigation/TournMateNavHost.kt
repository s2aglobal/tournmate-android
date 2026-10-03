package com.s2aglobal.tournmate.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.screen.MainScreen
import com.s2aglobal.tournmate.ui.screen.auth.AuthGateViewModel
import com.s2aglobal.tournmate.ui.screen.auth.AuthGateScreen
import com.s2aglobal.tournmate.ui.screen.auth.LoginScreen
import com.s2aglobal.tournmate.ui.screen.auth.OnboardingScreen
import com.s2aglobal.tournmate.ui.screen.auth.ProfileSetupScreen
import com.s2aglobal.tournmate.ui.screen.auth.WelcomeScreen
import com.s2aglobal.tournmate.ui.screen.openplay.OpenPlayDetailScreen
import com.s2aglobal.tournmate.ui.screen.openplay.OpenPlayDetailViewModel
import com.s2aglobal.tournmate.ui.screen.player.PlayerProfileScreen
import com.s2aglobal.tournmate.ui.screen.player.PlayerProfileViewModel
import com.s2aglobal.tournmate.ui.screen.player.RatePlayerSheet
import com.s2aglobal.tournmate.ui.screen.notification.NotificationInboxScreen
import com.s2aglobal.tournmate.ui.screen.tournament.TournamentDetailScreen

@Composable
fun TournMateNavHost(
    pendingDeepLink: DeepLinkParser.Target? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val authGateViewModel: AuthGateViewModel = hiltViewModel()
    val isGuestMode by authGateViewModel.isGuestMode.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH_GATE,
    ) {
        composable(Routes.AUTH_GATE) {
            AuthGateScreen(
                viewModel = authGateViewModel,
                onNavigateToWelcome = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(Routes.profileSetup()) {
                        popUpTo(Routes.AUTH_GATE) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
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
                    authGateViewModel.continueAsGuest()
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
                onSignInSuccess = { needsProfile, displayName ->
                    if (needsProfile) {
                        navController.navigate(Routes.profileSetup(displayName))
                    } else {
                        navController.navigate(Routes.MAIN) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PROFILE_SETUP,
            arguments = listOf(
                androidx.navigation.navArgument("name") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val initialName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("name") ?: "", "UTF-8"
            )
            ProfileSetupScreen(
                initialName = initialName,
                onBack = { navController.popBackStack() },
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
                isGuestMode = isGuestMode,
                onSignOut = {
                    authGateViewModel.signOut()
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onNavigateToTournamentDetail = { tournamentId ->
                    navController.navigate(Routes.tournamentDetail(tournamentId))
                },
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate(Routes.openPlayDetail(sessionId))
                },
                onNavigateToPlayerProfile = { playerId ->
                    navController.navigate(Routes.playerProfile(playerId))
                },
                onNavigateToNotifications = {
                    navController.navigate(Routes.NOTIFICATION_INBOX)
                },
            )

            LaunchedEffect(pendingDeepLink) {
                val target = pendingDeepLink ?: return@LaunchedEffect
                when (target.type) {
                    DeepLinkParser.Type.TOURNAMENT ->
                        navController.navigate(Routes.tournamentDetail(target.id))
                    DeepLinkParser.Type.SESSION ->
                        navController.navigate(Routes.openPlayDetail(target.id))
                }
                onDeepLinkConsumed()
            }
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
                isGuest = isGuestMode,
            )
        }

        composable(Routes.OPEN_PLAY_DETAIL) {
            val viewModel: OpenPlayDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(uiState.didDelete) {
                if (uiState.didDelete) navController.popBackStack()
            }

            val session = uiState.session
            if (session != null) {
                OpenPlayDetailScreen(
                    session = session,
                    attendees = uiState.attendees,
                    currentPlayerId = uiState.currentPlayerId,
                    firebaseUid = uiState.firebaseUid,
                    isLoading = uiState.isLoading,
                    isGuest = isGuestMode,
                    onBack = { navController.popBackStack() },
                    onJoin = { viewModel.join() },
                    onLeave = { viewModel.leave() },
                    onCancel = { viewModel.cancel() },
                    onFinish = { viewModel.finish() },
                    onUpdate = { title, date, duration, skill, game, ageGroup, cost, notes ->
                        viewModel.update(title, date, duration, skill, game, ageGroup, cost, notes)
                    },
                    onPlayerClick = { playerId ->
                        navController.navigate(Routes.playerProfile(playerId))
                    },
                )
            } else if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AppAccent)
                }
            }
        }

        composable(Routes.NOTIFICATION_INBOX) {
            NotificationInboxScreen(
                onBack = { navController.popBackStack() },
                onNavigateToTournament = { id ->
                    navController.navigate(Routes.tournamentDetail(id))
                },
                onNavigateToSession = { id ->
                    navController.navigate(Routes.openPlayDetail(id))
                },
            )
        }

        composable(Routes.PLAYER_PROFILE) {
            val viewModel: PlayerProfileViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            PlayerProfileScreen(
                player = uiState.player,
                ratings = uiState.ratings,
                averageRating = uiState.averageRating,
                matchStats = uiState.matchStats,
                currentPlayerId = uiState.currentPlayerId,
                hasRated = uiState.hasRated,
                isLoading = uiState.isLoading,
                viewerSport = uiState.viewerSport,
                onBack = { navController.popBackStack() },
                onRatePlayer = { viewModel.showRateSheet() },
            )

            if (uiState.showRateSheet && uiState.player != null) {
                RatePlayerSheet(
                    playerName = uiState.player!!.name,
                    onSubmit = { stars, comment -> viewModel.submitRating(stars, comment) },
                    onDismiss = { viewModel.hideRateSheet() },
                )
            }
        }
    }
}
