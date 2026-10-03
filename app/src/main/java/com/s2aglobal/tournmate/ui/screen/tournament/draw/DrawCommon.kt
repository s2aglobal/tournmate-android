package com.s2aglobal.tournmate.ui.screen.tournament.draw

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.ui.theme.AppAccent

internal val DrawGreen = Color(0xFF34C759)
internal val DrawRed = Color(0xFFFF3B30)
internal val DrawOrange = Color(0xFFFF9500)
internal val DrawYellow = Color(0xFFFFCC00)
internal val DrawSeparator = Color(0xFF3C3C43)
internal val DrawGray4 = Color(0xFFD1D1D6)
internal val DrawGray5 = Color(0xFFE5E5EA)
internal val DrawGray6 = Color(0xFFF2F2F7)

private val avatarColors = listOf(
    Color(0xFF007AFF), Color(0xFFFF3B30), Color(0xFF34C759), Color(0xFFFF9500), Color(0xFFAF52DE),
    Color(0xFFFF2D55), Color(0xFF30B0C7), Color(0xFF5856D6), Color(0xFF00C7BE), Color(0xFF32ADE6),
)

internal fun avatarColor(name: String): Color {
    val hash = name.sumOf { it.code }
    return avatarColors[kotlin.math.abs(hash) % avatarColors.size]
}

/** First-name team label used by every draw view ("John & Mike" / "John"). */
internal fun drawTeamName(reg: Registration, firstNameOnly: Boolean = false): String {
    val partner = reg.partner
    if (partner != null) {
        val first = reg.player.name.split(" ").first().ifEmpty { reg.player.name }
        val partnerFirst = partner.name.split(" ").first().ifEmpty { partner.name }
        return "$first & $partnerFirst"
    }
    return if (firstNameOnly) reg.player.name.split(" ").first().ifEmpty { reg.player.name } else reg.player.name
}

internal fun uniqueTeams(matches: List<Match>): List<Registration> {
    val seen = mutableSetOf<String>()
    val result = mutableListOf<Registration>()
    for (m in matches) {
        if (seen.add(m.teamAId)) result.add(m.teamA)
        if (seen.add(m.teamBId)) result.add(m.teamB)
    }
    return result
}

@Composable
internal fun FormatInfoRow(text: String, matches: List<Match>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.weight(1f))
        val done = matches.count { it.status == MatchStatus.FINISHED }
        Text("$done/${matches.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppAccent)
    }
}

@Composable
internal fun SwipeHint(label: String = "Swipe") {
    val transition = rememberInfiniteTransition(label = "swipe")
    val sway by transition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "sway",
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Default.SwipeLeft, null, Modifier.size(12.dp).offset(x = sway.dp), tint = AppAccent.copy(alpha = 0.6f))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.Gray.copy(alpha = 0.7f))
    }
}

@Composable
internal fun SectionMarker(title: String, barColor: Color, textColor: Color, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = textColor, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
internal fun InitialAvatar(name: String, size: Int = 24) {
    Box(Modifier.size(size.dp).background(avatarColor(name), CircleShape), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), fontSize = (size * 0.44f).sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
internal fun TrophyIcon(size: Int, tint: Color = DrawYellow) {
    Icon(Icons.Default.EmojiEvents, null, Modifier.size(size.dp), tint = tint)
}
