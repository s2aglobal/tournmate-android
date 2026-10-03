package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.WarningOrange
import kotlin.random.Random

@Composable
fun WinnerCelebrationScreen(
    winnerName: String,
    scoreLine: String,
    showSetsLabel: Boolean = false,
    isCreator: Boolean,
    onDone: () -> Unit,
) {
    val accent = AppAccent
    val confettiColors = listOf(
        accent, WarningOrange, Color(0xFFFF2D55), Color(0xFF007AFF),
        Color(0xFFFFCC00), Color(0xFFAF52DE), Color(0xFF00C7BE), Color(0xFFFF3B30),
    )
    val pieces = remember {
        List(40) { i ->
            ConfettiPiece(
                x = ((i * 47) % 100) / 100f,
                delay = Random.nextFloat(),
                speed = 0.6f + Random.nextFloat() * 0.6f,
                drift = Random.nextFloat() * 80f - 40f,
                spin = 180f + Random.nextFloat() * 540f,
                startRotation = Random.nextFloat() * 360f,
                size = if (i % 3 == 0) 12f else if (i % 3 == 1) 9f else 7f,
                shape = i % 3,
                color = confettiColors[i % confettiColors.size],
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "celebration")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "confetti",
    )
    val bounce by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "trophy",
    )

    Box(Modifier.fillMaxSize().background(Color.White)) {
        Canvas(Modifier.fillMaxSize()) {
            val density = density
            for (p in pieces) {
                val t = ((progress * p.speed) + p.delay) % 1f
                val y = -30f * density + t * (size.height + 60f * density)
                val x = p.x * size.width + p.drift * density * t
                val s = p.size * density
                rotate(p.startRotation + p.spin * t, Offset(x, y)) {
                    when (p.shape) {
                        0 -> drawRoundRect(p.color, Offset(x - s / 2, y - s / 4), Size(s, s * 0.5f), CornerRadius(2f * density))
                        1 -> drawCircle(p.color, radius = s * 0.35f, center = Offset(x, y))
                        else -> drawOval(p.color, Offset(x - s / 2, y - s * 0.2f), Size(s, s * 0.4f))
                    }
                }
            }
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(130.dp).background(accent.copy(alpha = 0.06f), CircleShape))
                Box(Modifier.size(100.dp).background(accent.copy(alpha = 0.1f), CircleShape))
                Box(
                    Modifier.offset(y = (-8 * bounce).dp).size(68.dp).clip(RoundedCornerShape(18.dp)).background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(36.dp), tint = Color.White)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("MATCH RECORDED", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, color = accent)
                Text(
                    "$winnerName\nWins!",
                    fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 34.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Row(
                Modifier.clip(RoundedCornerShape(14.dp)).background(ScoreGroupedBg).padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(scoreLine, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.width(1.5.dp).height(32.dp).background(ScoreDisabled))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("FINAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
                    if (showSetsLabel) {
                        Text("SETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
                    }
                }
            }

            if (!isCreator) {
                Row(
                    Modifier.clip(CircleShape).background(WarningOrange.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Default.Info, null, Modifier.size(14.dp), tint = WarningOrange)
                    Text("Awaiting admin confirmation", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = WarningOrange)
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier.padding(horizontal = 32.dp).padding(bottom = 40.dp).fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Text("DONE", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

private data class ConfettiPiece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val drift: Float,
    val spin: Float,
    val startRotation: Float,
    val size: Float,
    val shape: Int,
    val color: Color,
)
