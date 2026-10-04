package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.ScoreValidationError
import com.s2aglobal.tournmate.domain.model.ScoreValidator
import com.s2aglobal.tournmate.domain.model.SetScore
import com.s2aglobal.tournmate.ui.component.FullScreenCover
import com.s2aglobal.tournmate.ui.component.scoringUnit
import com.s2aglobal.tournmate.ui.theme.AppAccent

private val setLabels = listOf("One", "Two", "Three", "Four", "Five", "Six", "Seven")
private const val MAX_GAMES = 7

@Composable
fun SetScoreEntryScreen(
    match: Match,
    isCreator: Boolean,
    existingScores: List<SetScore> = emptyList(),
    onSubmit: (List<SetScore>) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val tournament = match.tournament
    val enforces = tournament.enforcesScoringRules
    // The tournament's scoring rules (sport defaults for older tournaments).
    val config = tournament.scoringConfig
    val sport = tournament.sportType
    val unit = sport.scoringUnit

    val texts = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            repeat(MAX_GAMES) { i ->
                val sc = existingScores.getOrNull(i)
                add(if (sc != null) sc.teamAPoints.toString() to sc.teamBPoints.toString() else "" to "")
            }
        }
    }
    var visibleSets by remember { mutableIntStateOf(existingScores.size.coerceIn(1, MAX_GAMES)) }
    var celebration by remember { mutableStateOf<Pair<String, String>?>(null) }

    val teamAName = registrationFullName(match.teamA)
    val teamBName = registrationFullName(match.teamB)

    val parsed = (0 until visibleSets).mapNotNull { i ->
        val a = texts[i].first.toIntOrNull() ?: return@mapNotNull null
        val b = texts[i].second.toIntOrNull() ?: return@mapNotNull null
        if (a > 0 || b > 0) SetScore(a, b) else null
    }
    val aWins = parsed.count { it.teamAWon }
    val bWins = parsed.count { it.teamBWon }

    val isValid = when {
        parsed.isEmpty() || parsed.size != visibleSets -> false
        // Legacy tournaments: original checks (no ties, no negatives, a winner).
        !enforces -> parsed.all { it.teamAPoints >= 0 && it.teamBPoints >= 0 && it.teamAPoints != it.teamBPoints } && aWins != bWins
        else -> ScoreValidator.isValidMatch(parsed, config)
    }
    val isMatchDecided = aWins >= config.gamesToWin || bWins >= config.gamesToWin
    val canAddGame = if (!enforces) visibleSets < 3 else {
        visibleSets < minOf(config.gamesPerMatch, MAX_GAMES) && !isMatchDecided && parsed.size == visibleSets
    }

    fun gameError(index: Int): String? {
        if (!enforces) return null
        val a = texts[index].first.toIntOrNull() ?: return null
        val b = texts[index].second.toIntOrNull() ?: return null
        if (a == 0 && b == 0) return null // empty game, not a tie
        return ScoreValidator.validateGame(SetScore(a, b), config)?.message(sport)
    }

    // Match-level problem, shown only once every game is filled in and valid.
    val matchError: String? = run {
        if (!enforces || parsed.isEmpty() || parsed.size != visibleSets) return@run null
        val result = ScoreValidator.validateMatch(parsed, config)
        val error = result.matchError
        if (result.gameErrors.isNotEmpty() || error == null) return@run null
        if (error is ScoreValidationError.MatchNotDecided && canAddGame) {
            "Add ${unit.lowercase()} ${visibleSets + 1} — no one has won yet"
        } else error.message(sport)
    }

    // iOS presents the celebration with .fullScreenCover over the score sheet; Done dismisses both.
    val result = celebration
    if (result != null) {
        FullScreenCover(onDismissRequest = {}) {
            WinnerCelebrationScreen(
                winnerName = result.first,
                scoreLine = result.second,
                showSetsLabel = visibleSets > 1,
                isCreator = isCreator,
                onDone = onDismiss,
            )
        }
        return
    }

    FullScreenSheet(onDismiss = onDismiss) { close ->
        Column(Modifier.fillMaxSize()) {
            ScoreTopBar("Enter Scores", close)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                MatchupHeader(teamAName, teamBName)
                if (enforces) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                        ScoringHint(
                            sport, config,
                            modifier = Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (index in 0 until visibleSets) {
                        SetCard(
                            index = index,
                            unit = unit,
                            value = texts[index],
                            teamAName = teamAName,
                            teamBName = teamBName,
                            error = gameError(index),
                            onChange = { texts[index] = it },
                            onRemove = {
                                // Later games shift up.
                                texts.removeAt(index)
                                texts.add("" to "")
                                visibleSets -= 1
                            },
                        )
                    }
                }
                if (canAddGame) {
                    Surface(
                        onClick = { visibleSets += 1 },
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(44.dp)
                            .border(1.dp, AppAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = AppAccent.copy(alpha = 0.06f),
                    ) {
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddCircle, null, Modifier.size(16.dp), tint = AppAccent)
                            Spacer(Modifier.width(6.dp))
                            Text("ADD ${unit.uppercase()} ${visibleSets + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = AppAccent)
                        }
                    }
                }
            }
            ScoreSubmitBar("SUBMIT SCORE", isValid, message = matchError) {
                focusManager.clearFocus()
                val winner = if (aWins > bWins) teamAName else teamBName
                val line = if (visibleSets == 1) "${parsed[0].teamAPoints}-${parsed[0].teamBPoints}" else "$aWins-$bWins"
                onSubmit(parsed)
                celebration = winner to line
            }
        }
    }
}

@Composable
private fun SetCard(
    index: Int,
    unit: String,
    value: Pair<String, String>,
    teamAName: String,
    teamBName: String,
    error: String?,
    onChange: (Pair<String, String>) -> Unit,
    onRemove: () -> Unit,
) {
    ScoreCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberBadge("${index + 1}")
            Text("$unit ${setLabels[index]}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            val a = value.first.toIntOrNull()
            val b = value.second.toIntOrNull()
            if (a != null && b != null && (a > 0 || b > 0) && a != b) {
                Text(
                    if (a > b) teamAName else teamBName,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AppAccent,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).clip(CircleShape)
                        .background(AppAccent.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            if (index > 0) {
                Surface(onClick = onRemove, shape = CircleShape, color = ScoreGroupedBg, modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, Modifier.size(12.dp), tint = Color.Gray)
                    }
                }
            }
        }
        ScoreFieldPair(
            a = value.first, b = value.second,
            onA = { onChange(it to value.second) },
            onB = { onChange(value.first to it) },
            isError = error != null,
        )
        error?.let { GameErrorRow(it) }
    }
}
