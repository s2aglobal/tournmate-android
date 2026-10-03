package com.s2aglobal.tournmate.ui.screen.tournament.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.ui.screen.tournament.RoundGroup
import com.s2aglobal.tournmate.ui.theme.AppAccent

/** Double-elimination layout (iOS `DoubleEliminationBracketView`). */
@Composable
fun DoubleEliminationDrawView(
    rounds: List<RoundGroup>,
    totalRounds: Int,
    onTapMatch: (Match) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(DrawGray6).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        FormatInfoRow(
            "Lose twice before you're eliminated. Losers get a second chance.",
            rounds.flatMap { it.matches },
            Modifier.padding(horizontal = 20.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionMarker("WINNERS BRACKET", DrawGreen, DrawGreen, Modifier.padding(horizontal = 20.dp))
            BracketDrawView(rounds, totalRounds, onTapMatch)
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(DrawRed, CircleShape))
                Text("Lose", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DrawRed)
            }
            Icon(Icons.Default.ArrowDownward, null, Modifier.size(14.dp), tint = DrawRed.copy(alpha = 0.6f))
            Text("Drop to losers bracket", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionMarker("LOSERS BRACKET", DrawRed, DrawRed, Modifier.padding(horizontal = 20.dp))
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(DrawRed.copy(alpha = 0.03f)).border(1.dp, DrawRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Losers from winners bracket compete here.", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Text("Lose twice = eliminated", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DrawRed.copy(alpha = 0.7f))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionMarker("GRAND FINAL", DrawYellow, DrawOrange, Modifier.padding(horizontal = 20.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = DrawYellow.copy(alpha = 0.15f), spotColor = DrawYellow.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.5.dp, Brush.linearGradient(listOf(DrawYellow, DrawOrange)), RoundedCornerShape(14.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TrophyIcon(28)
                    Text("Winners Bracket Champion", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Text("VS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AppAccent)
                    Text("Losers Bracket Champion", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                }
            }
        }

        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(DrawGreen, "Win")
            LegendDot(DrawRed, "Lose (1st)")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(DrawRed, CircleShape), contentAlignment = Alignment.Center) {
                    Text("L", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("Lose (2nd)", fontSize = 10.sp, color = Color.Gray)
            }
            LegendDot(DrawGray4, "Eliminated")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}
