package com.s2aglobal.tournmate.ui.screen.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import com.s2aglobal.tournmate.ui.theme.DarkNavy
import com.s2aglobal.tournmate.ui.theme.DarkNavyLight
import com.s2aglobal.tournmate.ui.theme.LimeAccent

@Composable
fun WelcomeScreen(
    onStartJourney: () -> Unit,
    onLogIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounceY",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy),
    ) {
        // Court lines background
        CourtLines(
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 32.dp),
        ) {
            // Top section with bouncing shuttlecock icon
            Spacer(modifier = Modifier.height(80.dp))

            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = bounceOffset.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = BrandPurple.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                color = BrandPurple,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SportsHandball,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Green accent bar
            Surface(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp),
                shape = RoundedCornerShape(50),
                color = LimeAccent,
            ) {}

            Spacer(modifier = Modifier.height(16.dp))

            // Main headline
            Text(
                text = "THE\nCOURT\nIS YOURS.",
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 52.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Elevate your badminton game\nwith elite analytics.",
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.5f),
                lineHeight = 24.sp,
            )

            Spacer(modifier = Modifier.weight(1f))

            // START JOURNEY button — lavender pill
            TextButton(
                onClick = onStartJourney,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(LimeAccent, RoundedCornerShape(28.dp)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "START JOURNEY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkNavy,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = DarkNavy,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // LOG IN + GUEST — side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick = onLogIn,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(DarkNavyLight, RoundedCornerShape(26.dp)),
                ) {
                    Text(
                        text = "LOG IN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.5.sp,
                    )
                }

                TextButton(
                    onClick = onContinueAsGuest,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(DarkNavyLight, RoundedCornerShape(26.dp)),
                ) {
                    Text(
                        text = "GUEST",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.5.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
private fun CourtLines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val lineColor = Color.White.copy(alpha = 0.04f)
        val stroke = Stroke(width = 1f)

        val inset = 30.dp.toPx()
        val courtTop = h * 0.15f
        val courtBottom = h * 0.75f
        val courtLeft = inset
        val courtRight = w - inset
        val midX = w / 2
        val midY = (courtTop + courtBottom) / 2

        // Outer boundary
        drawRect(
            color = lineColor,
            topLeft = Offset(courtLeft, courtTop),
            size = Size(courtRight - courtLeft, courtBottom - courtTop),
            style = stroke,
        )
        // Center horizontal line
        drawLine(lineColor, Offset(courtLeft, midY), Offset(courtRight, midY), strokeWidth = 1f)
        // Center vertical line
        drawLine(lineColor, Offset(midX, courtTop), Offset(midX, courtBottom), strokeWidth = 1f)
        // Service line top
        val serviceTop = courtTop + (midY - courtTop) * 0.55f
        drawLine(lineColor, Offset(courtLeft, serviceTop), Offset(courtRight, serviceTop), strokeWidth = 1f)
        // Service line bottom
        val serviceBottom = midY + (courtBottom - midY) * 0.45f
        drawLine(lineColor, Offset(courtLeft, serviceBottom), Offset(courtRight, serviceBottom), strokeWidth = 1f)
    }
}
