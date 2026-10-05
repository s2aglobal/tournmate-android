package com.s2aglobal.tournmate.ui.screen.tournament.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.ui.screen.tournament.RoundGroup
import com.s2aglobal.tournmate.ui.theme.AppAccent

private val CardWidth = 200.dp
private val CardHeight = 80.dp
private val ConnectorWidth = 40.dp
private val HeaderHeight = 36.dp
private const val BaseSpacing = 16f

private fun spacingFor(round: Int): Float = BaseSpacing * (1 shl (round - 1).coerceAtLeast(0))

private fun topPaddingFor(round: Int): Float =
    if (round <= 1) 0f else spacingFor(round - 1) / 2f + topPaddingFor(round - 1)

private fun columnHeight(matchCount: Int, round: Int): Dp {
    val top = topPaddingFor(round) + 12f
    return (top + matchCount * CardHeight.value + (matchCount - 1).coerceAtLeast(0) * spacingFor(round)).dp
}

/** Single-elimination bracket tree (iOS `BracketView`). */
@Composable
fun BracketDrawView(
    rounds: List<RoundGroup>,
    totalRounds: Int,
    onTapMatch: (Match) -> Unit,
    showFormatInfo: Boolean = true,
) {
    val allMatches = rounds.flatMap { it.matches }
    Column(Modifier.fillMaxWidth().background(DrawGray6)) {
        if (showFormatInfo) {
            FormatInfoRow(
                "Lose once and you're out. Winner advances to the next round.",
                allMatches,
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        // Full width so the Swipe hint sits at the right edge (iOS overlay), not over the round header.
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rounds.forEachIndexed { index, group ->
                    val level = index + 1
                    Column {
                        Text(
                            Match.bracketRoundName(group.round, totalRounds).uppercase(),
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp, color = AppAccent,
                            modifier = Modifier.width(CardWidth).height(HeaderHeight).clip(RoundedCornerShape(8.dp))
                                .background(AppAccent.copy(alpha = 0.08f)).wrapContentHeight(Alignment.CenterVertically),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Column(
                            Modifier.padding(top = (topPaddingFor(level) + 12f).dp),
                            verticalArrangement = Arrangement.spacedBy(spacingFor(level).dp),
                        ) {
                            group.matches.forEachIndexed { i, match ->
                                BracketMatchCard(match, globalMatchNumber(rounds, index, i), onTapMatch)
                            }
                        }
                    }
                    if (index < rounds.size - 1) {
                        Column {
                            Spacer(Modifier.height(HeaderHeight))
                            BracketConnector(level, group.matches.size)
                        }
                    }
                }
                champion(rounds, totalRounds)?.let { ChampionEnd(it) }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                SwipeHint()
            }
        }
    }
}

/** Only once the final itself is finished, never the winner of an earlier round that happens to be the last one so far. */
private fun champion(rounds: List<RoundGroup>, totalRounds: Int): Registration? {
    val last = rounds.lastOrNull() ?: return null
    return Match.bracketChampion(last.round, last.matches, totalRounds)
}

private fun globalMatchNumber(rounds: List<RoundGroup>, roundIndex: Int, index: Int): Int =
    rounds.take(roundIndex).sumOf { it.matches.size } + index + 1

@Composable
private fun BracketConnector(level: Int, matchCount: Int) {
    val accent = AppAccent.copy(alpha = 0.5f)
    Canvas(Modifier.width(ConnectorWidth).height(columnHeight(matchCount, level))) {
        val d = density
        val top = (topPaddingFor(level) + 12f) * d
        val slot = (CardHeight.value + spacingFor(level)) * d
        val nextTop = (topPaddingFor(level + 1) + 12f) * d
        val nextSlot = (CardHeight.value + spacingFor(level + 1)) * d
        val half = CardHeight.value * d / 2f
        val stroke = 2f * d
        val midX = size.width / 2f
        for (i in 0 until matchCount / 2) {
            val topCenter = top + (i * 2) * slot + half
            val bottomCenter = top + (i * 2 + 1) * slot + half
            val nextCenter = nextTop + i * nextSlot + half
            drawLine(accent, Offset(0f, topCenter), Offset(midX, topCenter), stroke, StrokeCap.Round)
            drawLine(accent, Offset(0f, bottomCenter), Offset(midX, bottomCenter), stroke, StrokeCap.Round)
            drawLine(accent, Offset(midX, topCenter), Offset(midX, bottomCenter), stroke, StrokeCap.Round)
            drawLine(accent, Offset(midX, (topCenter + bottomCenter) / 2f), Offset(size.width, nextCenter), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun BracketMatchCard(match: Match, number: Int, onTap: (Match) -> Unit) {
    val border = when (match.status) {
        MatchStatus.FINISHED -> DrawGreen.copy(alpha = 0.5f)
        MatchStatus.SCORE_SUBMITTED -> DrawOrange.copy(alpha = 0.5f)
        MatchStatus.DISPUTED -> DrawRed.copy(alpha = 0.5f)
        MatchStatus.SCHEDULED -> DrawSeparator.copy(alpha = 0.2f)
    }
    val finished = match.status == MatchStatus.FINISHED
    Column(
        Modifier
            .size(CardWidth, CardHeight)
            .shadow(3.dp, RoundedCornerShape(12.dp), ambientColor = if (finished) DrawGreen.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.5.dp, border, RoundedCornerShape(12.dp))
            .clickable { onTap(match) },
    ) {
        // Fixed 20dp label row (the "- 20" in the team-row height) so both team rows
        // fit inside the 80dp card; font padding made it taller and clipped team B.
        Row(Modifier.fillMaxWidth().height(20.dp).padding(start = 10.dp, end = 10.dp, top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Match $number", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.weight(1f))
            StatusBadge(match.status)
        }
        TeamRow(match.teamA, match.bracketPosition?.let { it * 2 + 1 }, teamScore(match, true), finished && match.winnerRegistrationId == match.teamAId)
        Box(Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(1.dp).background(DrawSeparator.copy(alpha = 0.2f)))
        TeamRow(match.teamB, match.bracketPosition?.let { it * 2 + 2 }, teamScore(match, false), finished && match.winnerRegistrationId == match.teamBId)
    }
}

@Composable
private fun StatusBadge(status: MatchStatus) {
    val (label, color) = when (status) {
        MatchStatus.FINISHED -> "DONE" to DrawGreen
        MatchStatus.SCORE_SUBMITTED -> "PENDING" to DrawOrange
        MatchStatus.DISPUTED -> "DISPUTED" to DrawRed
        MatchStatus.SCHEDULED -> return
    }
    Text(
        label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White,
        // Tight line height so the badge fits the 20dp label row like iOS.
        style = TextStyle(lineHeight = 10.sp, platformStyle = PlatformTextStyle(includeFontPadding = false)),
        modifier = Modifier.clip(CircleShape).background(color).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

@Composable
private fun TeamRow(reg: Registration, seed: Int?, score: String?, isWinner: Boolean) {
    val isTbd = reg.player.name == "TBD"
    Row(
        // 20dp label row + 1dp divider leave the rest for the two team rows.
        Modifier.fillMaxWidth().height(((CardHeight.value - 21f) / 2f).dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        seed?.let {
            Text("$it", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Gray, modifier = Modifier.width(16.dp))
        }
        if (isTbd) Box(Modifier.size(24.dp).background(DrawGray4, CircleShape))
        else InitialAvatar(reg.player.name)
        Text(
            if (isTbd) "TBD" else drawTeamName(reg),
            fontSize = 12.sp,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
            color = when { isTbd -> Color.Gray.copy(alpha = 0.6f); isWinner -> Color.Black; else -> Color.Gray },
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        score?.let {
            Text(
                it, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                color = if (isWinner) Color.White else Color.Gray,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (isWinner) AppAccent else DrawGray5)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

private fun teamScore(match: Match, isTeamA: Boolean): String? {
    if (match.setScores.isNotEmpty()) return "${if (isTeamA) match.setsWonByA else match.setsWonByB}"
    val a = match.scoreA ?: return null
    val b = match.scoreB ?: return null
    return if (isTeamA) "$a" else "$b"
}

@Composable
private fun ChampionEnd(winner: Registration) {
    Column(
        Modifier
            .padding(start = 16.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = DrawYellow.copy(alpha = 0.2f), spotColor = DrawYellow.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(2.dp, Brush.linearGradient(listOf(DrawYellow, DrawOrange)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(60.dp).background(DrawYellow.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
            TrophyIcon(26)
        }
        Text(drawTeamName(winner), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text("CHAMPION", fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, color = AppAccent)
    }
}
