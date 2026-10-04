package com.s2aglobal.tournmate.ui.screen.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.SuccessGreen
import com.s2aglobal.tournmate.ui.theme.WarningOrange

internal val InfoBlue = Color(0xFF2196F3)
internal val LossRed = Color(0xFFE53935)

/** Next Seed tier milestone: Seed 4 (<1200), Seed 3 (1200), Seed 2 (1400), Seed 1 (1600+). */
fun rankProgressHint(elo: Double): String = when {
    elo < 1200 -> "${(1200 - elo).toInt()} points to Seed 3"
    elo < 1400 -> "${(1400 - elo).toInt()} points to Seed 2"
    elo < 1600 -> "${(1600 - elo).toInt()} points to Seed 1"
    else -> "Top tier — Seed 1"
}

/** Full / half / empty star per position (iOS `starIcon(index:average:)`). */
fun starIcon(index: Int, average: Double): ImageVector = when {
    index <= average -> Icons.Filled.Star
    index - 0.5 <= average -> Icons.AutoMirrored.Filled.StarHalf
    else -> Icons.Filled.StarBorder
}

@Composable
fun SportsmanshipStars(average: Double, size: Dp = 24.dp) {
    val rounded = Math.round(average).toInt()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..5) {
            Icon(
                starIcon(i, average), null, Modifier.size(size),
                tint = if (i <= rounded) WarningOrange else Color.Gray.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
fun EloExplainerCard(modifier: Modifier = Modifier, cornerRadius: Dp = 16.dp) {
    Surface(
        modifier.fillMaxWidth(),
        RoundedCornerShape(cornerRadius),
        AppAccent.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Lightbulb, null, Modifier.size(18.dp), tint = Color(0xFFFFCC00))
                Text("What is Elo Rating?", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "The Elo rating system, created by physicist Arpad Elo, is a method for calculating the relative skill level of players. It's widely used in chess, esports, and competitive sports including racket sports.",
                fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp,
            )
            Spacer(Modifier.height(14.dp)); Divider(color = Color.Gray.copy(alpha = 0.15f)); Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EloInfoRow(Icons.Filled.ArrowCircleUp, SuccessGreen, "Win a match", "Your rating increases. Beat a higher-rated opponent for a bigger boost.")
                EloInfoRow(Icons.Filled.ArrowCircleDown, LossRed, "Lose a match", "Your rating decreases. Losing to a lower-rated opponent costs more points.")
                EloInfoRow(Icons.Filled.Scale, InfoBlue, "K-Factor = 24", "Controls how much ratings change per match. Higher K = faster swings.")
                EloInfoRow(Icons.Filled.People, AppAccent, "Doubles", "Team Elo is the average of both partners. Rating change is split equally.")
            }

            Spacer(Modifier.height(14.dp)); Divider(color = Color.Gray.copy(alpha = 0.15f)); Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Skill Tiers", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                EloTierRow(Icons.Filled.WorkspacePremium, "Seed 1", "1600+", SuccessGreen)
                EloTierRow(Icons.Filled.MilitaryTech, "Seed 2", "1400 – 1599", InfoBlue)
                EloTierRow(Icons.Filled.Star, "Seed 3", "1200 – 1399", WarningOrange)
                EloTierRow(Icons.Filled.Eco, "Seed 4", "Below 1200", Color.Gray)
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.PlayCircle, null, Modifier.size(14.dp), tint = AppAccent)
                Text(
                    buildAnnotatedString {
                        append("All new players start at ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("1200") }
                        append(" Elo (Seed 3).")
                    },
                    fontSize = 12.sp, color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun EloInfoRow(icon: ImageVector, color: Color, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp), tint = color)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun EloTierRow(icon: ImageVector, tier: String, range: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(14.dp), tint = color)
        Text(tier, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.width(50.dp))
        Text(range, fontSize = 11.sp, color = Color.Gray)
    }
}
