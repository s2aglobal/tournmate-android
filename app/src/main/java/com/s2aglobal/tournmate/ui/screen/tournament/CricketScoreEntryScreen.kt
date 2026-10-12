package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.component.sheetScrollLikeIos
import com.s2aglobal.tournmate.ui.theme.theme
import kotlinx.coroutines.launch

private data class InningsInput(val runs: String = "", val wickets: Int = 0, val overs: String = "")

/**
 * Scorecard entry for a cricket match: who batted first, then runs, wickets
 * and overs for each innings. The result ("won by 6 wickets") is derived live
 * with [CricketScoring], the same rules the server checks. Also covers no
 * result (abandoned), a rain-revised target, a super over, an optional
 * Player of the Match and optional batting/bowling figures. Mirrors iOS
 * `CricketScoreEntryView`.
 *
 * [isCreator]: the organizer's result is final; a captain's waits for confirmation.
 */
@Composable
fun CricketScoreEntryScreen(
    match: Match,
    isCreator: Boolean,
    existing: CricketResult? = match.cricketResult,
    existingPlayerOfMatch: PlayerOfMatch? = match.playerOfMatch,
    existingPlayerStats: CricketPlayerStats? = match.playerStats,
    /** Saves the result: `null` once the server accepted it, else the message to show. */
    onSubmit: suspend (CricketResult, PlayerOfMatch?, CricketPlayerStats?) -> String?,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val tournament = match.tournament
    val accent = tournament.sportType.theme.primary
    // Knockout matches always allow a super over, so a tie can be decided.
    val requiresWinner = cricketRequiresWinner(tournament, match)
    val rules = cricketRulesFor(tournament, match)
    val bpo = rules.ballsPerOver

    val idA = match.teamAId
    val idB = match.teamBId
    val nameA = match.teamA.displayName()
    val nameB = match.teamB.displayName()

    val existingFirst = existing?.innings?.getOrNull(0)
    val existingSecond = existing?.innings?.getOrNull(1)
    var teamABattedFirst by remember { mutableStateOf(existingFirst?.battingRegistrationId?.let { it == idA } ?: true) }
    var first by remember {
        mutableStateOf(existingFirst?.let { InningsInput("${it.runs}", it.wickets, CricketOvers.format(it.balls, bpo)) } ?: InningsInput())
    }
    var second by remember {
        mutableStateOf(existingSecond?.let { InningsInput("${it.runs}", it.wickets, CricketOvers.format(it.balls, bpo)) } ?: InningsInput())
    }
    var noResult by remember { mutableStateOf(existing?.isNoResult == true) }
    var revised by remember { mutableStateOf(existing?.revisedTarget != null) }
    var revisedRuns by remember { mutableStateOf(existing?.revisedTarget?.runs?.toString() ?: "") }
    var revisedOvers by remember { mutableStateOf(existing?.revisedTarget?.overs?.toString() ?: "") }
    var superOverWinnerIsA by remember { mutableStateOf(existing?.superOverWinnerRegistrationId?.let { it == idA }) }
    var playerOfMatch by remember { mutableStateOf(existingPlayerOfMatch) }
    var showPlayerOfMatchPicker by remember { mutableStateOf(false) }
    var playerStats by remember { mutableStateOf(existingPlayerStats ?: CricketPlayerStats()) }
    var figuresTarget by remember { mutableStateOf<FiguresTarget?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val firstId = if (teamABattedFirst) idA else idB
    val secondId = if (teamABattedFirst) idB else idA
    val firstName = if (teamABattedFirst) nameA else nameB
    val secondName = if (teamABattedFirst) nameB else nameA

    fun innings(input: InningsInput, side: String): CricketInnings? {
        val runs = input.runs.toIntOrNull() ?: return null
        val balls = CricketOvers.parse(input.overs, bpo) ?: return null
        return CricketInnings(side, runs, input.wickets, balls)
    }

    // Scores level (including one short of a revised target): a super over can decide it.
    fun isTie(r: CricketResult) = r.scoresLevel

    // The scorecard as entered, or null while required fields are empty or unreadable.
    val result: CricketResult? = run {
        if (noResult) {
            // A partial first innings is kept if it was filled in.
            return@run CricketResult(listOfNotNull(innings(first, firstId)), noResult = true)
        }
        val a = innings(first, firstId) ?: return@run null
        val b = innings(second, secondId) ?: return@run null
        var r = CricketResult(listOf(a, b))
        if (revised) {
            val runs = revisedRuns.toIntOrNull() ?: return@run null
            val overs = revisedOvers.toIntOrNull() ?: return@run null
            r = r.copy(revisedTarget = CricketRevisedTarget(runs, overs))
        }
        val aWon = superOverWinnerIsA
        if (aWon != null && isTie(r)) r = r.copy(superOverWinnerRegistrationId = if (aWon) idA else idB)
        r
    }

    val issues: List<CricketIssue> = run {
        val r = result ?: return@run emptyList()
        val found = CricketScoring.validate(r, rules, idA, idB)
        if (found.isNotEmpty()) return@run found
        knockoutIssue(tournament, match, r, rules)?.let { listOf(CricketIssue(if (r.isNoResult) "noResult" else "superOver", it)) } ?: emptyList()
    }
    // Problems with the player figures (only once the scorecard itself is valid).
    val statsIssues: List<CricketPlayerStats.Issue> = run {
        val r = result ?: return@run emptyList()
        if (issues.isNotEmpty() || playerStats.isEmpty) return@run emptyList()
        playerStats.validate(r, idA, idB) { if (it == idA) nameA else nameB }
    }
    // The innings as entered so far (0 = first, 1 = second), for the figures sheet.
    fun enteredInnings(index: Int): CricketInnings? =
        if (index == 0) innings(first, firstId) else if (noResult) null else innings(second, secondId)
    val previewText = result?.takeIf { issues.isEmpty() }?.let { r ->
        CricketScoring.outcome(r, rules).summary { if (it == idA) nameA else nameB }
    }
    val target: Int? = if (revised && revisedRuns.toIntOrNull() != null) revisedRuns.toInt() else first.runs.toIntOrNull()?.plus(1)
    val secondMaxOvers = if (revised) revisedOvers.toIntOrNull() ?: rules.overs else rules.overs

    FullScreenSheet(onDismiss = onDismiss) { close ->
        Column(Modifier.fillMaxSize()) {
            ScoreTopBar(if (isCreator) "Enter Result" else "Submit Result", close)
            Column(
                Modifier.weight(1f).sheetScrollLikeIos().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                MatchupHeader(nameA, nameB)
                Text(
                    rules.summary, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                )

                // Batted first
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Caption("BATTED FIRST")
                    Segmented(listOf(nameA, nameB), if (teamABattedFirst) 0 else 1, accent) { teamABattedFirst = it == 0; submitError = null }
                }

                InningsCard("1ST INNINGS", firstName, first, rules.overs, rules.maxWickets, accent, target = null) { first = it; submitError = null }

                ScoreCard {
                    ToggleLine(
                        "No result", "Match abandoned (weather, light, ground). 1 point each.", noResult, accent,
                    ) { noResult = it; submitError = null }
                }

                if (!noResult) {
                    ScoreCard {
                        ToggleLine(
                            "Revised target",
                            "Rain or a late start shortened the chase. Enter the target the umpires set.",
                            revised, accent,
                        ) { revised = it; submitError = null }
                        if (revised) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                NumberField("Target runs", revisedRuns, "124", accent, Modifier.weight(1f)) { revisedRuns = it.filter(Char::isDigit).take(4) }
                                NumberField("Overs", revisedOvers, "15", accent, Modifier.weight(1f)) { revisedOvers = it.filter(Char::isDigit).take(2) }
                            }
                        }
                    }
                    InningsCard("2ND INNINGS", secondName, second, secondMaxOvers, rules.maxWickets, accent, target) { second = it; submitError = null }

                    val r = result
                    if (rules.superOver && r != null && isTie(r)) {
                        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Caption("SCORES LEVEL · SUPER OVER WON BY")
                            val selected = when (superOverWinnerIsA) { null -> 0; true -> 1; false -> 2 }
                            Segmented(listOf("Not played", nameA, nameB), selected, accent) {
                                superOverWinnerIsA = when (it) { 1 -> true; 2 -> false; else -> null }
                            }
                        }
                    }

                    PlayerOfMatchPickerRow(playerOfMatch, if (playerOfMatch?.teamRegistrationId == idA) nameA else nameB) {
                        showPlayerOfMatchPicker = true
                    }
                }

                // Optional batting and bowling figures, per innings.
                val firstTeam = if (teamABattedFirst) match.teamA else match.teamB
                val secondTeam = if (teamABattedFirst) match.teamB else match.teamA
                val targets = buildList {
                    add("1st innings" to FiguresTarget(FiguresKind.BATTING, firstTeam, firstId, 0))
                    add("1st innings" to FiguresTarget(FiguresKind.BOWLING, secondTeam, secondId, 0))
                    if (!noResult) {
                        add("2nd innings" to FiguresTarget(FiguresKind.BATTING, secondTeam, secondId, 1))
                        add("2nd innings" to FiguresTarget(FiguresKind.BOWLING, firstTeam, firstId, 1))
                    }
                }
                PlayerStatsSection(targets, playerStats, accent) { figuresTarget = it }

                // Feedback
                Column(Modifier.padding(horizontal = 20.dp)) {
                    val issue = issues.firstOrNull()
                        ?: statsIssues.firstOrNull()?.let { CricketIssue(it.field, it.message) }
                    if (issue != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, null, Modifier.size(16.dp), tint = Color(0xFFFF9500))
                            Text(issue.message, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFF9500))
                        }
                    } else if (previewText != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, Modifier.size(18.dp), tint = Color(0xFF34C759))
                            Text(previewText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34C759))
                        }
                    }
                }
            }
            ScoreSubmitBar(
                if (isCreator) "SAVE RESULT" else "SUBMIT FOR CONFIRMATION",
                enabled = result != null && issues.isEmpty() && statsIssues.isEmpty(),
                isSubmitting = isSubmitting,
                submitError = submitError,
            ) {
                val r = result ?: return@ScoreSubmitBar
                focusManager.clearFocus()
                keyboard?.hide()
                submitError = null
                isSubmitting = true
                scope.launch {
                    val error = onSubmit(r, if (r.isNoResult) null else playerOfMatch, playerStats.takeUnless { it.isEmpty })
                    isSubmitting = false
                    if (error != null) submitError = error else close()
                }
            }
        }
        if (showPlayerOfMatchPicker) {
            PlayerOfMatchSheet(
                teams = listOf(match.teamA, match.teamB),
                selection = playerOfMatch,
                onSelect = { playerOfMatch = it },
                onDismiss = { showPlayerOfMatchPicker = false },
            )
        }
        figuresTarget?.let { t ->
            PlayerFiguresSheet(
                kind = t.kind,
                team = t.team,
                teamId = t.teamId,
                innings = enteredInnings(t.inningsIndex),
                ballsPerOver = bpo,
                accent = accent,
                stats = playerStats,
                onSave = { playerStats = it; submitError = null },
                onDismiss = { figuresTarget = null },
            )
        }
    }
}

/** One batting or bowling list to fill in: which team (and its registration id), for which innings. */
private data class FiguresTarget(val kind: FiguresKind, val team: Registration, val teamId: String, val inningsIndex: Int)

@Composable
private fun PlayerStatsSection(
    targets: List<Pair<String, FiguresTarget>>,
    stats: CricketPlayerStats,
    accent: Color,
    onOpen: (FiguresTarget) -> Unit,
) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Caption("PLAYER STATS · OPTIONAL")
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)) {
            targets.forEachIndexed { idx, (inningsLabel, t) ->
                if (idx > 0) HorizontalDivider(Modifier.padding(start = 16.dp), color = ScoreDivider)
                val summary = when (t.kind) {
                    FiguresKind.BATTING -> stats.batting.filter { it.teamRegistrationId == t.teamId }.let { f ->
                        if (f.isEmpty()) "Not entered" else "${f.size} ${if (f.size == 1) "batter" else "batters"} · ${f.sumOf { it.runs }} runs"
                    }
                    FiguresKind.BOWLING -> stats.bowling.filter { it.teamRegistrationId == t.teamId }.let { f ->
                        if (f.isEmpty()) "Not entered" else "${f.size} ${if (f.size == 1) "bowler" else "bowlers"} · ${f.sumOf { it.wickets }} wickets"
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clickable { onOpen(t) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (t.kind == FiguresKind.BATTING) Icons.Default.SportsCricket else Icons.Default.SportsBaseball,
                        null, Modifier.size(22.dp), tint = accent,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "${if (t.kind == FiguresKind.BATTING) "Batting" else "Bowling"} · ${t.team.displayName()}",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text("$inningsLabel · $summary", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = Color.Gray)
                }
            }
        }
        Text("Runs, balls and wickets per player build the Top performers and player profiles.", fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun PlayerOfMatchPickerRow(pick: PlayerOfMatch?, teamName: String, onClick: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Caption("PLAYER OF THE MATCH · OPTIONAL")
        Row(
            // White on the grouped background (iOS: systemGray6 on white).
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White)
                .clickable(onClick = onClick).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.MilitaryTech, null, Modifier.size(20.dp), tint = if (pick == null) Color.Gray else Color(0xFFFF9500))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    pick?.name ?: "Choose a player", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = if (pick == null) Color.Gray else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (pick != null) Text(teamName, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.UnfoldMore, null, Modifier.size(18.dp), tint = Color.Gray)
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray)
}

@Composable
private fun Segmented(options: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ScoreDivider).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }
                    .semantics { this.selected = on },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) accent else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ToggleLine(title: String, detail: String, checked: Boolean, accent: Color, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = accent))
    }
}

@Composable
private fun InningsCard(
    title: String,
    team: String,
    input: InningsInput,
    maxOvers: Int,
    maxWickets: Int,
    accent: Color,
    target: Int?,
    onChange: (InningsInput) -> Unit,
) {
    ScoreCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Caption(title)
            Spacer(Modifier.weight(1f))
            target?.let { Text("Target $it", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent) }
        }
        Text(team, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
            NumberField("Runs", input.runs, "0", accent, Modifier.weight(1f)) { onChange(input.copy(runs = it.filter(Char::isDigit).take(3))) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Wickets", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Row(Modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onChange(input.copy(wickets = (input.wickets - 1).coerceAtLeast(0))) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.RemoveCircle, "One wicket fewer", Modifier.size(24.dp), tint = accent)
                    }
                    Text("${input.wickets}", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 24.dp))
                    IconButton(onClick = { onChange(input.copy(wickets = (input.wickets + 1).coerceAtMost(maxWickets))) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.AddCircle, "One wicket more", Modifier.size(24.dp), tint = accent)
                    }
                }
            }
            NumberField("Overs", input.overs, "$maxOvers", accent, Modifier.weight(1f), decimal = true) {
                onChange(input.copy(overs = it.filter { c -> c.isDigit() || c == '.' }.take(5)))
            }
        }
        Row {
            Text(
                "All $maxOvers overs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent,
                modifier = Modifier.clickable { onChange(input.copy(overs = "$maxOvers")) }.padding(vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "All out", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent,
                modifier = Modifier.clickable { onChange(input.copy(wickets = maxWickets)) }.padding(vertical = 4.dp),
            )
        }
        Text("Overs as balls bowled, e.g. 18.2 = 18 overs and 2 balls.", fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    placeholder: String,
    accent: Color,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    onChange: (String) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.NumberPassword),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth().height(48.dp)
                .clip(RoundedCornerShape(10.dp)).background(ScoreGroupedBg)
                .border(0.5.dp, ScoreDivider, RoundedCornerShape(10.dp))
                .semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    inner()
                }
            },
        )
    }
}
