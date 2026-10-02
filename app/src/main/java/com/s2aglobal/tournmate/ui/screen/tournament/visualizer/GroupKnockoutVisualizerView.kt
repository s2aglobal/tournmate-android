package com.s2aglobal.tournmate.ui.screen.tournament.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.PrizeGold
import com.s2aglobal.tournmate.ui.theme.SuccessGreen

@Composable
fun GroupKnockoutVisualizerView(
    state: TournamentVisualState,
    onMatchTap: ((MatchNode) -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Description + match count
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Teams first compete in groups. Top teams\nadvance to knockout rounds.",
                fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp, modifier = Modifier.weight(1f),
            )
            val allGroupMatches = state.groups.sumOf { g -> g.participants.size * (g.participants.size - 1) / 2 }
            val koMatches = state.knockoutRounds.sumOf { it.matches.size }
            val totalMatches = allGroupMatches + koMatches
            val finishedMatches = state.knockoutRounds.sumOf { r -> r.matches.count { it.isFinished } }
            Text("$finishedMatches/$totalMatches", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }

        // GROUP STAGE
        if (state.groups.isNotEmpty()) {
            Text("GROUP STAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent, letterSpacing = 1.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                state.groups.forEach { group ->
                    Column(Modifier.weight(1f)) {
                        Text("Group ${group.label}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                        Spacer(Modifier.height(8.dp))
                        group.participants.forEachIndexed { index, p ->
                            val isAdvancing = index < group.advancingCount
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Surface(
                                    modifier = Modifier.size(22.dp),
                                    shape = CircleShape,
                                    color = if (isAdvancing) AppAccent else Color.Gray.copy(alpha = 0.2f),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${group.label}${index + 1}",
                                            fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                            color = if (isAdvancing) Color.White else Color.Gray,
                                        )
                                    }
                                }
                                Text(p.teamName, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        Text("Top ${group.advancingCount} advance", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // Arrow
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = AppAccent.copy(alpha = 0.1f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ArrowDownward, null, Modifier.size(16.dp), tint = AppAccent)
                        }
                    }
                    Text("Top teams qualify", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // KNOCKOUT STAGE
        Text("KNOCKOUT STAGE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppAccent, letterSpacing = 1.sp)

        if (state.knockoutRounds.isNotEmpty()) {
            // Round name pills
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.knockoutRounds.forEach { round ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AppAccent.copy(alpha = 0.1f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            round.roundName.uppercase(),
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppAccent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Actual bracket tree
            SingleEliminationVisualizerView(
                state.copy(rounds = state.knockoutRounds, formatType = com.s2aglobal.tournmate.domain.model.MatchFormat.SINGLE_ELIMINATION, champion = state.champion),
                onMatchTap,
            )
        } else {
            // Placeholder bracket showing seeding slots
            val groupLabels = state.groups.map { it.label }.sorted()
            val advancingCount = state.groups.firstOrNull()?.advancingCount ?: 2

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = AppAccent.copy(alpha = 0.1f), modifier = Modifier.weight(1f)) {
                    Text("SEMI FINAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), textAlign = TextAlign.Center)
                }
                Surface(shape = RoundedCornerShape(50), color = AppAccent.copy(alpha = 0.1f), modifier = Modifier.weight(1f)) {
                    Text("FINAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bracket tree placeholder
            PlaceholderBracket(groupLabels, advancingCount)
        }
    }
}

@Composable
private fun PlaceholderBracket(groupLabels: List<String>, advancingPerGroup: Int) {
    val seeds = buildList {
        if (groupLabels.size >= 2 && advancingPerGroup >= 2) {
            add("1st Group ${groupLabels[0]}")
            add("2nd Group ${groupLabels[1]}")
            add("1st Group ${groupLabels[1]}")
            add("2nd Group ${groupLabels[0]}")
        } else {
            groupLabels.forEach { label ->
                for (i in 1..advancingPerGroup) {
                    add("${i}${ordinalSuffix(i)} Group $label")
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Semi-final column
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            seeds.forEach { seed ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8F8FA),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = Color.Gray.copy(alpha = 0.3f)) {}
                        Text(seed, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Connector lines
        Canvas(modifier = Modifier.width(32.dp).fillMaxHeight()) {
            val lineColor = Color.Gray.copy(alpha = 0.3f)
            val h = size.height
            val w = size.width
            val slotH = h / seeds.size.coerceAtLeast(1)
            val pairCount = seeds.size / 2

            for (i in 0 until pairCount) {
                val topY = slotH * (i * 2) + slotH / 2
                val bottomY = slotH * (i * 2 + 1) + slotH / 2
                val midX = w / 2
                val nextY = (topY + bottomY) / 2

                drawLine(lineColor, Offset(0f, topY), Offset(midX, topY), strokeWidth = 2f, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(midX, topY), Offset(midX, bottomY), strokeWidth = 2f, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(0f, bottomY), Offset(midX, bottomY), strokeWidth = 2f, cap = StrokeCap.Round)
                drawLine(lineColor, Offset(midX, nextY), Offset(w, nextY), strokeWidth = 2f, cap = StrokeCap.Round)
            }
        }

        // Final column
        Column(
            modifier = Modifier.weight(0.8f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val pairCount = seeds.size / 2
            repeat(pairCount.coerceAtMost(2)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8F8FA),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = Color.Gray.copy(alpha = 0.3f)) {}
                        Text("TBD", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        // Final connector to champion
        Canvas(modifier = Modifier.width(24.dp).fillMaxHeight()) {
            val lineColor = Color.Gray.copy(alpha = 0.3f)
            val midY = size.height / 2
            drawLine(lineColor, Offset(0f, size.height * 0.35f), Offset(size.width / 2, size.height * 0.35f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(lineColor, Offset(size.width / 2, size.height * 0.35f), Offset(size.width / 2, size.height * 0.65f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(lineColor, Offset(0f, size.height * 0.65f), Offset(size.width / 2, size.height * 0.65f), strokeWidth = 2f, cap = StrokeCap.Round)
            drawLine(lineColor, Offset(size.width / 2, midY), Offset(size.width, midY), strokeWidth = 2f, cap = StrokeCap.Round)
        }

        // Champion
        Column(
            modifier = Modifier.width(60.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.EmojiEvents, null, Modifier.size(28.dp), tint = PrizeGold)
            Text("Champion", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PrizeGold)
        }
    }
}

private fun ordinalSuffix(n: Int): String = when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}
