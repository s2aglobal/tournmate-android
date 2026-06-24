package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WinnerCelebrationScreen(
    winnerName: String,
    scoreLine: String,
    isCreator: Boolean,
    onDone: () -> Unit,
) {
    val confettiColors = listOf(
        Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFE66D),
        Color(0xFF95E1D3), Color(0xFFF38181), Color(0xFFAA96DA),
        BrandPurple, Color(0xFFFF9800), Color(0xFF4CAF50),
    )

    val particles = remember {
        List(40) {
            ConfettiParticle(
                x = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.7f,
                size = 4f + Random.nextFloat() * 8f,
                color = confettiColors[it % confettiColors.size],
                wobble = Random.nextFloat() * 2f,
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "confettiProgress",
    )

    val trophyBounce by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "trophyBounce",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding(),
    ) {
        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (p in particles) {
                val y = ((progress * p.speed + p.x) % 1f) * h
                val x = (p.x * w + sin((progress + p.wobble) * 6.28f).toFloat() * 30f)
                drawCircle(p.color, radius = p.size, center = androidx.compose.ui.geometry.Offset(x % w, y))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))

            // Trophy
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(100.dp).offset(y = (-trophyBounce * 8).dp),
                    shape = CircleShape,
                    color = BrandPurple.copy(alpha = 0.15f),
                ) {}
                Surface(
                    modifier = Modifier.size(72.dp).offset(y = (-trophyBounce * 8).dp),
                    shape = CircleShape,
                    color = BrandPurple,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EmojiEvents, null, Modifier.size(36.dp), tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text("🎉", fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(winnerName, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("WINS!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandPurple, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Text(scoreLine, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            if (!isCreator) {
                Spacer(Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(imageVector = Icons.Filled.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF9800))
                        Text("Awaiting admin confirmation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFF9800))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
            ) {
                Text("DONE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

private data class ConfettiParticle(
    val x: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val wobble: Float,
)
