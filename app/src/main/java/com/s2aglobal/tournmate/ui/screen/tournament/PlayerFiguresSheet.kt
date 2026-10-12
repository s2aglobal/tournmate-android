package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.component.sheetScrollLikeIos

enum class FiguresKind { BATTING, BOWLING }

private data class FiguresRow(
    val a: String = "",   // runs (batting) or overs (bowling)
    val b: String = "",   // balls (batting) or runs (bowling)
    val c: String = "",   // wickets (bowling)
    val notOut: Boolean = false,
)

private val Orange = Color(0xFFFF9500)

/**
 * Optional batting or bowling figures for one team in one innings, entered
 * under the cricket scorecard: runs, balls and not out for batters; overs,
 * runs and wickets for bowlers. Only rows with something typed are kept. A
 * running total shows how the figures compare with the innings (extras and run
 * outs make up any difference). Mirrors iOS `PlayerFiguresSheet`.
 *
 * [teamId] is the registration id the figures are stored under; [innings] is
 * the team's own innings when batting, the other team's when bowling.
 */
@Composable
internal fun PlayerFiguresSheet(
    kind: FiguresKind,
    team: Registration,
    teamId: String,
    innings: CricketInnings?,
    ballsPerOver: Int,
    accent: Color,
    stats: CricketPlayerStats,
    onSave: (CricketPlayerStats) -> Unit,
    onDismiss: () -> Unit,
) {
    val members = team.rosterMembers
    val rows = remember {
        mutableStateMapOf<String, FiguresRow>().apply {
            when (kind) {
                FiguresKind.BATTING -> stats.batting.filter { it.teamRegistrationId == teamId }.forEach {
                    put(TeamRoles.key(it.key), FiguresRow(a = "${it.runs}", b = "${it.balls}", notOut = !it.out))
                }
                FiguresKind.BOWLING -> stats.bowling.filter { it.teamRegistrationId == teamId }.forEach {
                    put(TeamRoles.key(it.key), FiguresRow(a = CricketOvers.format(it.balls, ballsPerOver), b = "${it.runs}", c = "${it.wickets}"))
                }
            }
        }
    }

    fun battingFigures(): List<BattingFigures> = members.mapNotNull { m ->
        val r = rows[m.key] ?: return@mapNotNull null
        if (r.a.isEmpty() && r.b.isEmpty()) return@mapNotNull null
        BattingFigures(teamId, m.key, m.name, r.a.toIntOrNull() ?: 0, r.b.toIntOrNull() ?: 0, out = !r.notOut)
    }

    fun bowlingFigures(): List<BowlingFigures> = members.mapNotNull { m ->
        val r = rows[m.key] ?: return@mapNotNull null
        if (r.a.isEmpty() && r.b.isEmpty() && r.c.isEmpty()) return@mapNotNull null
        BowlingFigures(
            teamId, m.key, m.name,
            balls = CricketOvers.parse(r.a, ballsPerOver) ?: 0,
            runs = r.b.toIntOrNull() ?: 0,
            wickets = r.c.toIntOrNull() ?: 0,
        )
    }

    val title = "${if (kind == FiguresKind.BATTING) "Batting" else "Bowling"} · ${team.displayName()}"

    FullScreenSheet(onDismiss = onDismiss) { close ->
        Column(Modifier.fillMaxSize()) {
            // Cancel / title / Done (iOS toolbar).
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Cancel", fontSize = 16.sp, color = accent,
                    modifier = Modifier.clickable { close() }.padding(vertical = 6.dp, horizontal = 4.dp),
                )
                Text(
                    title, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(
                    "Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent,
                    modifier = Modifier.clickable {
                        val updated = when (kind) {
                            FiguresKind.BATTING -> stats.copy(batting = stats.batting.filter { it.teamRegistrationId != teamId } + battingFigures())
                            FiguresKind.BOWLING -> stats.copy(bowling = stats.bowling.filter { it.teamRegistrationId != teamId } + bowlingFigures())
                        }
                        onSave(updated)
                        close()
                    }.padding(vertical = 6.dp, horizontal = 4.dp),
                )
            }
            Column(
                Modifier.weight(1f).sheetScrollLikeIos().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Running total against the innings.
                val (runs, balls, wickets) = when (kind) {
                    FiguresKind.BATTING -> battingFigures().let { f -> Triple(f.sumOf { it.runs }, f.sumOf { it.balls }, f.count { it.out }) }
                    FiguresKind.BOWLING -> bowlingFigures().let { f -> Triple(f.sumOf { it.runs }, f.sumOf { it.balls }, f.sumOf { it.wickets }) }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (kind == FiguresKind.BATTING) "BATTERS SO FAR" else "BOWLERS SO FAR",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray,
                    )
                    if (innings != null) {
                        val over = runs > innings.runs || balls > innings.balls || wickets > innings.wickets
                        val text = if (kind == FiguresKind.BATTING) {
                            "$runs of ${innings.runs} runs · $balls of ${innings.balls} balls · $wickets of ${innings.wickets} out"
                        } else {
                            "${CricketOvers.format(balls, ballsPerOver)} of ${CricketOvers.format(innings.balls, ballsPerOver)} overs · " +
                                "$runs of ${innings.runs} runs · $wickets of ${innings.wickets} wickets"
                        }
                        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (over) Orange else Color.Black)
                    } else {
                        Text("Enter this innings on the scorecard first.", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        HeaderText("PLAYER", Modifier.weight(1f), TextAlign.Start)
                        if (kind == FiguresKind.BATTING) {
                            HeaderText("RUNS", Modifier.width(56.dp))
                            HeaderText("BALLS", Modifier.width(56.dp))
                            HeaderText("NOT OUT", Modifier.width(60.dp))
                        } else {
                            HeaderText("OVERS", Modifier.width(56.dp))
                            HeaderText("RUNS", Modifier.width(56.dp))
                            HeaderText("WKTS", Modifier.width(48.dp))
                        }
                    }
                    if (members.isEmpty()) {
                        Text(
                            "No players named.", fontSize = 14.sp, color = Color.Gray,
                            modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center,
                        )
                    }
                    members.forEachIndexed { idx, member ->
                        if (idx > 0) HorizontalDivider(Modifier.padding(start = 16.dp), color = ScoreDivider)
                        val row = rows[member.key] ?: FiguresRow()
                        fun set(r: FiguresRow) { rows[member.key] = r }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(member.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (member.isGuest) Text("Guest", fontSize = 11.sp, color = Color.Gray)
                            }
                            if (kind == FiguresKind.BATTING) {
                                FigureField(row.a, "${member.name} runs", 56.dp, accent) { set(row.copy(a = it.filter(Char::isDigit).take(3))) }
                                FigureField(row.b, "${member.name} balls", 56.dp, accent) { set(row.copy(b = it.filter(Char::isDigit).take(3))) }
                                Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) {
                                    Switch(
                                        checked = row.notOut, onCheckedChange = { set(row.copy(notOut = it)) },
                                        colors = SwitchDefaults.colors(checkedTrackColor = accent),
                                        modifier = Modifier.semantics { contentDescription = "${member.name} not out" },
                                    )
                                }
                            } else {
                                FigureField(row.a, "${member.name} overs", 56.dp, accent, decimal = true) {
                                    set(row.copy(a = it.filter { c -> c.isDigit() || c == '.' }.take(5)))
                                }
                                FigureField(row.b, "${member.name} runs conceded", 56.dp, accent) { set(row.copy(b = it.filter(Char::isDigit).take(3))) }
                                FigureField(row.c, "${member.name} wickets", 48.dp, accent) { set(row.copy(c = it.filter(Char::isDigit).take(2))) }
                            }
                        }
                    }
                }
                Text(
                    if (kind == FiguresKind.BATTING) {
                        "Leave a player blank if they didn't bat. Extras make up any runs not credited to a batter."
                    } else {
                        "Overs as balls bowled, e.g. 3.2. Run outs aren't credited to a bowler."
                    },
                    fontSize = 12.sp, color = Color.Gray,
                )
                Spacer(Modifier.imePadding())
            }
        }
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier, align: TextAlign = TextAlign.Center) {
    Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = align, modifier = modifier)
}

@Composable
private fun FigureField(
    value: String,
    label: String,
    width: Dp,
    accent: Color,
    decimal: Boolean = false,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.NumberPassword),
        cursorBrush = SolidColor(accent),
        modifier = Modifier.width(width).height(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEFEFF0))
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text("–", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.LightGray)
                inner()
            }
        },
    )
}
