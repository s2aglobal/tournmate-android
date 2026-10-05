package com.s2aglobal.tournmate.ui.screen.tournament.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.ui.screen.tournament.RoundGroup
import com.s2aglobal.tournmate.ui.theme.AppAccent

/** Cross-match grid for round robin (iOS `RoundRobinDrawView`). */
@Composable
fun RoundRobinDrawView(rounds: List<RoundGroup>, pointsPerWin: Int, pointsPerLoss: Int, onTapMatch: (Match) -> Unit) {
    val allMatches = rounds.flatMap { it.matches }
    val teams = uniqueTeams(allMatches)
    val labels = teams.indices.map { ('A' + it).toString() }

    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatInfoRow("Everyone plays everyone. Top teams based on points.", allMatches)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SwipeHint("Swipe to see all matchups") }
        }

        Column(
            Modifier
                .horizontalScroll(rememberScrollState())
                .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, DrawSeparator.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        ) {
            Row {
                Spacer(Modifier.size(100.dp, 36.dp))
                labels.forEach { label ->
                    Box(Modifier.size(44.dp, 36.dp), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                    }
                }
            }
            teams.forEachIndexed { rowIdx, rowTeam ->
                Row(
                    Modifier.height(40.dp).background(if (rowIdx % 2 == 0) Color.White else DrawGray6.copy(alpha = 0.3f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.width(100.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Centred in its 18dp column like iOS; left-aligned it sat on the card border.
                        Text(labels[rowIdx], fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent, textAlign = TextAlign.Center, modifier = Modifier.width(18.dp))
                        Text(drawTeamName(rowTeam, firstNameOnly = true), fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    teams.forEachIndexed { colIdx, colTeam ->
                        GridCell(rowTeam, colTeam, rowIdx == colIdx, allMatches, onTapMatch)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendIcon(Icons.Default.Check, DrawGreen, "Win ($pointsPerWin pts)")
            LegendIcon(Icons.Default.Close, DrawRed, "Loss ($pointsPerLoss pts)")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("—", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray.copy(alpha = 0.6f))
                Text("Not played", fontSize = 10.sp, color = Color.Gray)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.EmojiEvents, null, Modifier.size(12.dp), tint = AppAccent)
            Text("Points decide the standings", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        }
    }
}

@Composable
private fun GridCell(rowTeam: Registration, colTeam: Registration, isSelf: Boolean, matches: List<Match>, onTap: (Match) -> Unit) {
    val rowId = rowTeam.id.toString().uppercase()
    val colId = colTeam.id.toString().uppercase()
    val match = if (isSelf) null else matches.firstOrNull {
        (it.teamAId == rowId && it.teamBId == colId) || (it.teamAId == colId && it.teamBId == rowId)
    }
    Box(
        Modifier.size(44.dp, 40.dp)
            .then(if (isSelf) Modifier.background(DrawGray5) else Modifier)
            .then(if (match != null) Modifier.clickable { onTap(match) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            match != null && match.status == MatchStatus.FINISHED -> when (match.winnerRegistrationId) {
                rowId -> Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = DrawGreen)
                null -> Text("=", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DrawOrange)
                else -> Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = DrawRed)
            }
            else -> Text("—", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun LegendIcon(icon: ImageVector, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(12.dp), tint = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}
