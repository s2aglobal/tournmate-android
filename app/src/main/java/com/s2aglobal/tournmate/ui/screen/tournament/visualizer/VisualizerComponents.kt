package com.s2aglobal.tournmate.ui.screen.tournament.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.ui.theme.*

@Composable
fun RoundHeader(roundName: String) {
    Text(
        roundName.uppercase(),
        fontSize = 12.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, color = AppAccent,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun MatchCard(
    node: MatchNode,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val match = node.match
    val statusColor = when (match.status) {
        MatchStatus.FINISHED -> SuccessGreen
        MatchStatus.SCORE_SUBMITTED -> WarningOrange
        MatchStatus.DISPUTED -> ErrorRed
        MatchStatus.SCHEDULED -> Color.Gray
    }

    Surface(
        onClick = { onTap?.invoke() },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color.Gray.copy(alpha = 0.1f)), width = 1.dp,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            // Status badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    match.status.rawValue.uppercase().replace("_", " "),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                match.round?.let {
                    Text("R$it", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Team A
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val isWinnerA = node.match.winnerRegistrationId == node.match.teamAId
                if (isWinnerA && node.isFinished) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(14.dp), tint = PrizeGold)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    node.teamAName, fontSize = 14.sp,
                    fontWeight = if (isWinnerA && node.isFinished) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${match.scoreA ?: "-"}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (isWinnerA && node.isFinished) AppAccent else Color.Black,
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Color(0xFFF2F2F7))

            // Team B
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val isWinnerB = node.match.winnerRegistrationId == node.match.teamBId
                if (isWinnerB && node.isFinished) {
                    Icon(Icons.Default.EmojiEvents, null, Modifier.size(14.dp), tint = PrizeGold)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    node.teamBName, fontSize = 14.sp,
                    fontWeight = if (isWinnerB && node.isFinished) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${match.scoreB ?: "-"}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = if (isWinnerB && node.isFinished) AppAccent else Color.Black,
                )
            }
        }
    }
}

@Composable
fun ChampionBadge(name: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = PrizeGold.copy(alpha = 0.1f),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Default.EmojiEvents, null, Modifier.size(24.dp), tint = PrizeGold)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("CHAMPION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrizeGold, letterSpacing = 1.sp)
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ParticipantRow(node: ParticipantNode) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        node.seedNumber?.let { seed ->
            Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = AppAccent.copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$seed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                }
            }
        }
        Text(
            node.teamName, fontSize = 13.sp,
            fontWeight = if (node.isWinner) FontWeight.Bold else FontWeight.Normal,
            color = if (node.isEliminated) Color.Gray else Color.Black,
        )
        if (node.isWinner) {
            Icon(Icons.Default.EmojiEvents, null, Modifier.size(14.dp), tint = PrizeGold)
        }
    }
}
