package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.SetScore
import com.s2aglobal.tournmate.ui.theme.AppAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetScoreEntryScreen(
    match: Match,
    isCreator: Boolean,
    existingScores: List<SetScore> = emptyList(),
    onSubmit: (List<SetScore>) -> Unit,
    onDismiss: () -> Unit,
) {
    var sets by remember {
        mutableStateOf(
            if (existingScores.isNotEmpty()) existingScores.map { it.teamAPoints.toString() to it.teamBPoints.toString() }
            else listOf("" to "")
        )
    }
    var showCelebration by remember { mutableStateOf(false) }

    val parsedSets = sets.mapNotNull { (a, b) ->
        val aInt = a.toIntOrNull() ?: return@mapNotNull null
        val bInt = b.toIntOrNull() ?: return@mapNotNull null
        if (aInt < 0 || bInt < 0 || aInt == bInt) return@mapNotNull null
        SetScore(aInt, bInt)
    }

    val setsWonA = parsedSets.count { it.teamAWon }
    val setsWonB = parsedSets.count { it.teamBWon }
    val hasWinner = setsWonA != setsWonB && parsedSets.size == sets.size
    val bestOf = match.sportType.defaultBestOf
    val maxSets = bestOf

    if (showCelebration) {
        val winnerName = if (setsWonA > setsWonB) match.teamA.player.name else match.teamB.player.name
        val scoreLine = parsedSets.joinToString(" ") { "${it.teamAPoints}-${it.teamBPoints}" }
        WinnerCelebrationScreen(
            winnerName = winnerName,
            scoreLine = scoreLine,
            isCreator = isCreator,
            onDone = {
                onSubmit(parsedSets)
                onDismiss()
            },
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("SET SCORES", fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Text("Best of $bestOf", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(20.dp))

            // Team headers
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(match.teamA.player.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    match.teamA.partner?.let { Text("& ${it.name}", fontSize = 10.sp, color = Color.Gray) }
                }
                Spacer(Modifier.width(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(match.teamB.player.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    match.teamB.partner?.let { Text("& ${it.name}", fontSize = 10.sp, color = Color.Gray) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Set rows
            sets.forEachIndexed { index, (a, b) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = a, onValueChange = { v -> sets = sets.toMutableList().apply { this[index] = v to this[index].second } },
                        modifier = Modifier.width(70.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                    )
                    Text("Set ${index + 1}", Modifier.padding(horizontal = 12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedTextField(
                        value = b, onValueChange = { v -> sets = sets.toMutableList().apply { this[index] = this[index].first to v } },
                        modifier = Modifier.width(70.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp), singleLine = true,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Add/Remove set
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (sets.size < maxSets) {
                    TextButton(onClick = { sets = sets + ("" to "") }) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Set")
                    }
                }
                if (sets.size > 1) {
                    TextButton(onClick = { sets = sets.dropLast(1) }) {
                        Icon(Icons.Default.Remove, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove Set")
                    }
                }
            }

            // Score summary
            if (parsedSets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Sets: $setsWonA - $setsWonB", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppAccent)
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { showCelebration = true },
                enabled = hasWinner,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
            ) {
                Text("SUBMIT SCORE", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}
