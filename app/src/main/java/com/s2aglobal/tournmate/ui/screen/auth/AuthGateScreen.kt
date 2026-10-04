package com.s2aglobal.tournmate.ui.screen.auth

import com.s2aglobal.tournmate.ui.component.LightSystemBarIcons
import com.s2aglobal.tournmate.ui.component.sportIconPainter
import com.s2aglobal.tournmate.ui.component.TrophySpinner
import com.s2aglobal.tournmate.ui.component.TrophySpinnerStyle
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.domain.model.SportType
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.TournmatePurple
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

            TrophySpinner(style = TrophySpinnerStyle.LIGHT)
        }
    }
}
