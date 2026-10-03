package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.ScoreValidator
import com.s2aglobal.tournmate.domain.model.SetScore

@Composable
fun SimpleScoreEntryScreen(
    match: Match,
    isCreator: Boolean,
    onSubmit: (scoreA: Int, scoreB: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var scoreA by remember { mutableStateOf("") }
    var scoreB by remember { mutableStateOf("") }
    var winnerName by remember { mutableStateOf<String?>(null) }

    val teamAName = registrationFullName(match.teamA)
    val teamBName = registrationFullName(match.teamB)
    val a = scoreA.toIntOrNull()
    val b = scoreB.toIntOrNull()
    val tournament = match.tournament
    val sport = tournament.sportType
    // Round-robin matches are a single game played to the tournament's target.
    val gameConfig = tournament.scoringConfig.copy(gamesPerMatch = 1)
    val gameError = if (tournament.enforcesScoringRules && a != null && b != null) {
        ScoreValidator.validateGame(SetScore(a, b), gameConfig)?.message(sport)
    } else null
    val isValid = a != null && b != null && a >= 0 && b >= 0 && a != b && (a > 0 || b > 0) && gameError == null

    FullScreenSheet(onDismiss = { if (winnerName == null) onDismiss() }) {
        val winner = winnerName
        if (winner != null) {
            WinnerCelebrationScreen(
                winnerName = winner,
                scoreLine = "$scoreA-$scoreB",
                showSetsLabel = false,
                isCreator = isCreator,
                onDone = onDismiss,
            )
            return@FullScreenSheet
        }

        Column(Modifier.fillMaxSize()) {
            ScoreTopBar("Enter Score", onDismiss)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                MatchupHeader(teamAName, teamBName)
                ScoreCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberBadge("1")
                        Text("Game Score", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (tournament.enforcesScoringRules) {
                            ScoringHint(sport, gameConfig, badgeSize = 14.dp, fontSize = 11, modifier = Modifier.weight(1f, fill = false))
                        }
                    }
                    ScoreFieldPair(scoreA, scoreB, onA = { scoreA = it }, onB = { scoreB = it }, isError = gameError != null)
                    gameError?.let { GameErrorRow(it) }
                }
            }
            ScoreSubmitBar(if (isCreator) "FINALIZE SCORE" else "SUBMIT SCORE", isValid) {
                focusManager.clearFocus()
                if (a == null || b == null) return@ScoreSubmitBar
                onSubmit(a, b)
                winnerName = if (a > b) teamAName else teamBName
            }
        }
    }
}
