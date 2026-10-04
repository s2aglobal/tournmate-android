package com.s2aglobal.tournmate.ui.screen.auth

import com.s2aglobal.tournmate.ui.component.LightSystemBarIcons
import com.s2aglobal.tournmate.ui.component.SportBrandMark
import com.s2aglobal.tournmate.ui.component.TrophySpinner
import com.s2aglobal.tournmate.ui.component.TrophySpinnerStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.DarkNavy

/** Branded splash shown while the auth state resolves; routing is driven by the nav host. */
@Composable
fun AuthGateScreen() {
    SplashScreen()
}

@Composable
private fun SplashScreen() {
    LightSystemBarIcons()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Saved sport on its theme colour; trophy before any sport is saved (iOS SportBrandMark).
            SportBrandMark(size = 80.dp, cornerRadius = 24.dp, iconSize = 36.dp, shadowElevation = 20.dp)

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

            TrophySpinner(style = TrophySpinnerStyle.LIGHT)
        }
    }
}
