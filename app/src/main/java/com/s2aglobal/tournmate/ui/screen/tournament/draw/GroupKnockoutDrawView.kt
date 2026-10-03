package com.s2aglobal.tournmate.ui.screen.tournament.draw

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.ui.screen.tournament.RoundGroup
import com.s2aglobal.tournmate.ui.theme.AppAccent

/** Group stage cards plus knockout bracket (iOS `GroupKnockoutDrawView`). */
@Composable
fun GroupKnockoutDrawView(
    allRounds: List<RoundGroup>,
    totalBracketRounds: Int,
    onTapMatch: (Match) -> Unit,
) {
    val allMatches = allRounds.flatMap { it.matches }
    val groupMatches = allMatches.filter { it.groupLabel != null }.groupBy { it.groupLabel!! }
    val knockoutRounds = allRounds.mapNotNull { g ->
        val ko = g.matches.filter { it.groupLabel == null }
        if (ko.isEmpty()) null else RoundGroup(g.round, ko)
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        FormatInfoRow(
            "Teams first compete in groups. Top teams advance to knockout rounds.",
            allMatches,
            Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
        )

        Column(Modifier.padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionMarker("GROUP STAGE", DrawOrange, DrawOrange, Modifier.padding(horizontal = 20.dp)) { SwipeHint() }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                groupMatches.keys.sorted().forEach { label -> GroupCard(label, groupMatches[label].orEmpty()) }
            }
        }

        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.ArrowCircleDown, null, Modifier.size(22.dp), tint = AppAccent)
            Text("Top teams qualify", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionMarker("KNOCKOUT STAGE", DrawGreen, DrawGreen, Modifier.padding(horizontal = 20.dp)) { SwipeHint() }
            if (knockoutRounds.isNotEmpty()) {
                BracketDrawView(knockoutRounds, totalBracketRounds, onTapMatch, showFormatInfo = false)
            } else {
                PreviewKnockout(groupMatches.keys.sorted())
            }
        }
    }
}

@Composable
private fun GroupCard(label: String, matches: List<Match>) {
    val teams = uniqueTeams(matches)
    val complete = matches.isNotEmpty() && matches.all { it.status == MatchStatus.FINISHED }
    Column(
        Modifier
            .width(180.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, DrawSeparator.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Group $label", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppAccent, modifier = Modifier.weight(1f))
            if (complete) Icon(Icons.Default.CheckCircle, null, Modifier.size(12.dp), tint = DrawGreen)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            teams.forEachIndexed { index, team ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Person, null, Modifier.size(10.dp), tint = AppAccent)
                    Text("$label${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = AppAccent, modifier = Modifier.width(22.dp))
                    Text(drawTeamName(team), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(6.dp).background(AppAccent, CircleShape))
            Text(if (teams.size <= 2) "All advance" else "Top 2 advance", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        }
    }
}

private const val PCardH = 64f
private const val PSpacing = 16f
private const val PHeaderH = 28f
private const val PTopPad = 12f

@Composable
private fun PreviewKnockout(groups: List<String>) {
    val placeholders = buildList {
        if (groups.size >= 2) {
            var i = 0
            while (i + 1 < groups.size) {
                add("1st Group ${groups[i]}" to "2nd Group ${groups[i + 1]}")
                add("1st Group ${groups[i + 1]}" to "2nd Group ${groups[i]}")
                i += 2
            }
            if (groups.size % 2 == 1) add("1st Group ${groups.last()}" to "2nd Group ${groups.last()}")
        } else if (groups.size == 1) {
            add("1st Group ${groups[0]}" to "2nd Group ${groups[0]}")
        }
    }
    val slotH = PCardH + PSpacing
    val sfTopPad = PTopPad + slotH / 2
    val sfSlotH = slotH * 2
    val many = placeholders.size > 2

    Row(Modifier.horizontalScroll(rememberScrollState()).padding(20.dp), verticalAlignment = Alignment.Top) {
        Column {
            PreviewHeader(if (many) "Quarter Final" else "Semi Final")
            Column(Modifier.padding(top = PTopPad.dp), verticalArrangement = Arrangement.spacedBy(PSpacing.dp)) {
                placeholders.forEach { PreviewMatch(it.first, it.second) }
            }
        }
        if (placeholders.size > 1) PreviewConnector(placeholders.size, PSpacing, PTopPad)

        Column {
            PreviewHeader(if (many) "Semi Final" else "Final")
            Column(Modifier.padding(top = sfTopPad.dp), verticalArrangement = Arrangement.spacedBy((sfSlotH - PCardH).dp)) {
                repeat(maxOf(1, placeholders.size / 2)) { PreviewMatch("TBD", "TBD") }
            }
        }

        if (many) {
            PreviewConnector(maxOf(1, placeholders.size / 2), sfSlotH - PCardH, sfTopPad)
            Column {
                PreviewHeader("Final")
                Box(Modifier.padding(top = (sfTopPad + sfSlotH / 2).dp)) { PreviewMatch("TBD", "TBD") }
            }
        }

        val lastCenter = if (many) PHeaderH + sfTopPad + sfSlotH / 2 + PCardH / 2 else PHeaderH + sfTopPad + PCardH / 2
        Column(Modifier.padding(start = 16.dp)) {
            Spacer(Modifier.height(maxOf(0f, lastCenter - 50f).dp))
            ChampionPlaceholder()
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PreviewHeader(title: String) {
    Box(Modifier.size(160.dp, PHeaderH.dp).clip(RoundedCornerShape(8.dp)).background(AppAccent.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
        Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp, color = AppAccent, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PreviewMatch(a: String, b: String) {
    Column(
        Modifier.size(160.dp, PCardH.dp)
            .shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(10.dp)).background(Color.White)
            .border(1.dp, DrawSeparator.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
    ) {
        PreviewTeam(a)
        Box(Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(1.dp).background(DrawSeparator.copy(alpha = 0.2f)))
        PreviewTeam(b)
    }
}

@Composable
private fun PreviewTeam(name: String) {
    val tbd = name == "TBD"
    Row(Modifier.fillMaxWidth().height(31.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(20.dp).background(if (tbd) DrawGray4 else AppAccent.copy(alpha = 0.3f), CircleShape))
        Text(name, fontSize = 11.sp, fontWeight = if (tbd) FontWeight.Normal else FontWeight.Medium, color = if (tbd) Color.Gray.copy(alpha = 0.6f) else Color.Gray, maxLines = 1)
    }
}

@Composable
private fun PreviewConnector(matchCount: Int, fromSpacing: Float, topPadding: Float) {
    val color = AppAccent.copy(alpha = 0.4f)
    val slotH = PCardH + fromSpacing
    val topOffset = PHeaderH + topPadding
    val totalH = topOffset + matchCount * PCardH + maxOf(0, matchCount - 1) * fromSpacing
    Canvas(Modifier.size(32.dp, totalH.dp)) {
        val d = density
        val stroke = 1.5f * d
        val midX = size.width / 2f
        for (i in 0 until maxOf(1, matchCount / 2)) {
            val topY = (topOffset + (i * 2) * slotH + PCardH / 2) * d
            val bottomY = (topOffset + (i * 2 + 1) * slotH + PCardH / 2) * d
            drawLine(color, Offset(0f, topY), Offset(midX, topY), stroke, StrokeCap.Round)
            drawLine(color, Offset(midX, topY), Offset(midX, bottomY), stroke, StrokeCap.Round)
            drawLine(color, Offset(midX, bottomY), Offset(0f, bottomY), stroke, StrokeCap.Round)
            val mid = (topY + bottomY) / 2f
            drawLine(color, Offset(midX, mid), Offset(size.width, mid), stroke, StrokeCap.Round)
        }
    }
}

@Composable
private fun ChampionPlaceholder() {
    val transition = rememberInfiniteTransition(label = "champion")
    val pulse by transition.animateFloat(
        initialValue = 0.85f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse",
    )
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(1.5.dp, Brush.linearGradient(listOf(DrawYellow, DrawOrange)), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(50.dp).scale(pulse).background(DrawYellow.copy(alpha = 0.12f), CircleShape))
            TrophyIcon(26)
        }
        Text("Champion", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
    }
}
