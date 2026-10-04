package com.s2aglobal.tournmate.ui.screen.auth

import com.s2aglobal.tournmate.ui.component.LightSystemBarIcons
import com.s2aglobal.tournmate.ui.component.MultiSportArtworkRow
import com.s2aglobal.tournmate.ui.component.SportBrandMark
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.s2aglobal.tournmate.ui.theme.DarkNavy
import com.s2aglobal.tournmate.ui.theme.DarkNavyLight
import com.s2aglobal.tournmate.ui.theme.LimeAccent

@Composable
fun WelcomeScreen(
    onStartJourney: () -> Unit,
    onLogIn: () -> Unit,
    onContinueAsGuest: () -> Unit,
) {
    LightSystemBarIcons()
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { contentVisible = true }
    val content by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "contentVisible",
    )
    fun Modifier.entrance(distance: Float) = graphicsLayer {
        alpha = content
        translationY = (1f - content) * distance.dp.toPx()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavy),
    ) {
        // Sport-neutral court lines background
        CourtLines(
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 32.dp),
        ) {
            // Top section with bouncing brand mark
            Spacer(modifier = Modifier.height(80.dp))

            // Saved sport on its theme colour; trophy before any sport is saved (iOS SportBrandMark).
            SportBrandMark(
                size = 56.dp,
                cornerRadius = 16.dp,
                iconSize = 24.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .offset(y = (-8f * bounce).dp)
                    .rotate(-2f + 4f * bounce),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Green accent bar
            Surface(
                modifier = Modifier
                    .entrance(20f)
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
                modifier = Modifier.entrance(20f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tournaments for your sport.\nElevate your game.",
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.5f),
                lineHeight = 24.sp,
                modifier = Modifier.entrance(15f),
            )

            Spacer(modifier = Modifier.height(20.dp))

            MultiSportArtworkRow(size = 36.dp, spacing = 10.dp, modifier = Modifier.entrance(15f))

            Spacer(modifier = Modifier.weight(1f))

            // START JOURNEY button — lavender pill
            TextButton(
                onClick = onStartJourney,
                modifier = Modifier
                    .entrance(10f)
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
                modifier = Modifier.entrance(10f).fillMaxWidth(),
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
