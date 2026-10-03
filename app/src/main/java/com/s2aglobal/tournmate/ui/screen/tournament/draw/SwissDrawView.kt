package com.s2aglobal.tournmate.ui.screen.tournament.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.ui.screen.tournament.RoundGroup
import com.s2aglobal.tournmate.ui.theme.AppAccent

/** Swiss round columns with in-place "generate next round" (iOS `SwissDrawView`). */
@Composable
fun SwissDrawView(
    rounds: List<RoundGroup>,
    currentSwissRound: Int,
    isOrganizer: Boolean,
    canGenerateNextRound: Boolean,
    isLoading: Boolean,
    onTapMatch: (Match) -> Unit,
    onGenerateNextRound: () -> Unit,
) {
    val allMatches = rounds.flatMap { it.matches }
    val lastComplete = rounds.lastOrNull()?.matches?.let { it.isNotEmpty() && it.all { m -> m.status == MatchStatus.FINISHED } } == true

    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatInfoRow("No elimination. Players with similar records face each other each round.", allMatches)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SwipeHint("Swipe to see all rounds") }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            rounds.forEach { group ->
                val complete = group.matches.all { it.status == MatchStatus.FINISHED }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(
                        Modifier.width(140.dp).clip(RoundedCornerShape(8.dp)).background(AppAccent.copy(alpha = 0.08f)).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Round ${group.round}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                        if (complete) Icon(Icons.Default.CheckCircle, null, Modifier.size(10.dp), tint = DrawGreen)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        group.matches.forEachIndexed { index, match ->
                            PairingCard(match, pairingLabel(group.round, index), onTapMatch)
                        }
                    }
                }
            }
        }

        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LetterLegend("W", DrawGreen, "W = Win")
            LetterLegend("L", DrawRed, "L = Loss")
            Spacer(Modifier.weight(1f))
            Text("Balanced matchups every round", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.Gray.copy(alpha = 0.7f))
        }

        if (isOrganizer && lastComplete && canGenerateNextRound) {
            Button(
                onClick = onGenerateNextRound,
                enabled = !isLoading,
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
            ) {
                Icon(Icons.Default.ArrowCircleRight, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("GENERATE ROUND ${currentSwissRound + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun pairingLabel(round: Int, index: Int): String =
    if (round == 1) "${index * 2 + 1} vs ${index * 2 + 2}" else listOf("W vs W", "L vs L", "W vs L")[index % 3]

@Composable
private fun PairingCard(match: Match, label: String, onTap: (Match) -> Unit) {
    Column(
        Modifier.width(140.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
            .border(1.dp, if (match.status == MatchStatus.FINISHED) DrawGreen.copy(alpha = 0.3f) else DrawSeparator.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable { onTap(match) },
    ) {
        Text(label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp))
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(drawTeamName(match.teamA, true), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text("vs", fontSize = 8.sp, fontWeight = FontWeight.Black, color = AppAccent)
            Text(drawTeamName(match.teamB, true), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LetterLegend(letter: String, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(12.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            Text(letter, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}
