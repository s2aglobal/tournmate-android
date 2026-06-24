package com.s2aglobal.tournmate.ui.screen.tournament.visualizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.ui.theme.BrandPurple
import com.s2aglobal.tournmate.ui.theme.ErrorRed
import com.s2aglobal.tournmate.ui.theme.SuccessGreen

@Composable
fun RoundRobinVisualizerView(
    state: TournamentVisualState,
    onMatchTap: ((MatchNode) -> Unit)? = null,
) {
    val allMatches = state.rounds.flatMap { it.matches }
    if (allMatches.isEmpty()) {
        Text("No matches generated yet.", fontSize = 13.sp, color = Color.Gray)
        return
    }

    val allRealMatches = allMatches.map { it.match }
    val teams = extractTeams(allRealMatches)
    val labels = teams.indices.map { ('A' + it).toString() }
    val totalMatches = allRealMatches.size
    val finishedMatches = allRealMatches.count { it.status == MatchStatus.FINISHED }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Everyone plays everyone. Top teams based on points.", fontSize = 11.sp, color = Color.Gray)
            Text("$finishedMatches/$totalMatches", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("Swipe to see all matchups", fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.5f))
        }

        // Cross-match grid
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp,
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Column {
                    // Header row with column labels
                    Row {
                        Box(Modifier.width(100.dp).height(36.dp))
                        labels.forEach { label ->
                            Box(Modifier.width(44.dp).height(36.dp), contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPurple)
                            }
                        }
                    }

                    // Grid rows
                    teams.forEachIndexed { rowIdx, rowTeam ->
                        Row(
                            modifier = Modifier.background(
                                if (rowIdx % 2 == 0) Color.White else Color(0xFFF8F8FA)
                            ),
                        ) {
                            // Row label
                            Row(
                                Modifier.width(100.dp).height(40.dp).padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    labels[rowIdx], fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandPurple,
                                    modifier = Modifier.width(18.dp),
                                )
                                Text(
                                    shortName(rowTeam), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // Grid cells
                            teams.forEachIndexed { colIdx, colTeam ->
                                val isSelf = rowIdx == colIdx
                                Box(
                                    Modifier.width(44.dp).height(40.dp)
                                        .background(if (isSelf) Color(0xFFE8E8E8) else Color.Transparent)
                                        .then(
                                            if (!isSelf) {
                                                val match = findMatch(allRealMatches, rowTeam, colTeam)
                                                if (match != null) {
                                                    Modifier.clickable {
                                                        val node = allMatches.firstOrNull { it.match.id == match.id }
                                                        node?.let { onMatchTap?.invoke(it) }
                                                    }
                                                } else Modifier
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelf) {
                                        Text("—", fontSize = 14.sp, color = Color.Gray.copy(alpha = 0.3f))
                                    } else {
                                        val match = findMatch(allRealMatches, rowTeam, colTeam)
                                        if (match != null && match.status == MatchStatus.FINISHED) {
                                            val isWinner = match.winnerRegistrationId == rowTeam.id.toString().uppercase()
                                            val isLoser = match.winnerRegistrationId != null && match.winnerRegistrationId != rowTeam.id.toString().uppercase()
                                            if (isWinner) {
                                                Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = SuccessGreen)
                                            } else if (isLoser) {
                                                Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = ErrorRed)
                                            } else {
                                                Text("=", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                            }
                                        } else {
                                            Text("—", fontSize = 14.sp, color = Color.Gray.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Legend
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Check, null, Modifier.size(12.dp), tint = SuccessGreen)
                Text("Win (2 pts)", fontSize = 10.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Close, null, Modifier.size(12.dp), tint = ErrorRed)
                Text("Loss (0 pts)", fontSize = 10.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("—", fontSize = 12.sp, color = Color.Gray.copy(alpha = 0.3f))
                Text("Not played", fontSize = 10.sp, color = Color.Gray)
            }
        }

        // Standings note
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.EmojiEvents, null, Modifier.size(12.dp), tint = BrandPurple)
            Text("Points decide the standings", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

private fun extractTeams(matches: List<Match>): List<Registration> {
    val seen = mutableSetOf<String>()
    val result = mutableListOf<Registration>()
    for (match in matches) {
        val aId = match.teamA.id.toString().uppercase()
        val bId = match.teamB.id.toString().uppercase()
        if (aId !in seen) { seen.add(aId); result.add(match.teamA) }
        if (bId !in seen) { seen.add(bId); result.add(match.teamB) }
    }
    return result
}

private fun findMatch(matches: List<Match>, teamA: Registration, teamB: Registration): Match? {
    val aId = teamA.id.toString().uppercase()
    val bId = teamB.id.toString().uppercase()
    return matches.firstOrNull {
        (it.teamAId == aId && it.teamBId == bId) || (it.teamAId == bId && it.teamBId == aId)
    }
}

private fun shortName(reg: Registration): String {
    val firstName = reg.player.name.split(" ").firstOrNull() ?: reg.player.name
    val partner = reg.partner
    return if (partner != null) {
        val partnerFirst = partner.name.split(" ").firstOrNull() ?: partner.name
        "$firstName & $partnerFirst"
    } else firstName
}
