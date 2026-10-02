package com.s2aglobal.tournmate.ui.screen.auth

import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.domain.model.SportType
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.s2aglobal.tournmate.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
import com.s2aglobal.tournmate.ui.theme.DarkNavy

@Composable
fun AuthGateScreen(
    viewModel: AuthGateViewModel = hiltViewModel(),
    onNavigateToWelcome: () -> Unit,
    onNavigateToProfileSetup: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToMain: () -> Unit,
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.SIGNED_OUT -> onNavigateToWelcome()
            AuthState.NEEDS_PROFILE -> onNavigateToProfileSetup()
            AuthState.NEEDS_ONBOARDING -> onNavigateToOnboarding()
            AuthState.SIGNED_IN -> onNavigateToMain()
            AuthState.LOADING -> { /* show splash */ }
        }
    }

    // Splash screen matching iOS AuthGateView
    SplashScreen()
}

@Composable
private fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "trophyRotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Purple rounded rect with sport icon
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(20.dp, RoundedCornerShape(24.dp), ambientColor = TournmatePurple.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(24.dp),
                color = TournmatePurple,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = if (CurrentSport.sport == SportType.BADMINTON) rememberVectorPainter(Icons.Default.SportsHandball) else sportIconPainter(CurrentSport.sport),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "TournMate",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your sports companion",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Trophy spinner (simplified)
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .rotate(rotation),
                tint = Color.White.copy(alpha = 0.4f),
            )
        }
    }
}
