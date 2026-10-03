package com.s2aglobal.tournmate.ui.screen.tournament.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.ui.screen.tournament.RoundGroup
import com.s2aglobal.tournmate.ui.theme.AppAccent

/** Manual-draw pool and organizer-created pairings (iOS `ManualDrawView`). */
@Composable
fun ManualDrawView(rounds: List<RoundGroup>, teams: List<Registration>, onTapMatch: (Match) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.PanTool, null, Modifier.size(12.dp), tint = AppAccent)
            Text("Manually create matchups your way. Full control in your hands.", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
        }

        if (rounds.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TEAMS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Color.Gray)
                teams.forEach { team ->
                    val name = drawTeamName(team)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White)
                            .border(1.dp, DrawSeparator.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InitialAvatar(name)
                        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Menu, null, Modifier.size(12.dp), tint = Color.Gray.copy(alpha = 0.6f))
                    }
                }
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.ArrowCircleDown, null, Modifier.size(20.dp), tint = AppAccent)
                Text("Drag & drop to create matchups", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DrawGray6.copy(alpha = 0.5f)).padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Stadium, null, Modifier.size(28.dp), tint = Color.Gray.copy(alpha = 0.6f))
                Text("No matchups created yet", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                Text("Use the Manage tab to create matchups manually.", fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.7f))
            }
        } else {
            rounds.forEach { group ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.width(4.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(AppAccent))
                        Text("Round ${group.round}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppAccent)
                    }
                    group.matches.forEach { PairingRow(it, onTapMatch) }
                }
            }
        }
    }
}

@Composable
private fun PairingRow(match: Match, onTap: (Match) -> Unit) {
    val a = drawTeamName(match.teamA)
    val b = drawTeamName(match.teamB)
    Row(
        Modifier.fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.03f), spotColor = Color.Black.copy(alpha = 0.03f))
            .clip(RoundedCornerShape(12.dp)).background(Color.White)
            .border(1.dp, DrawSeparator.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable { onTap(match) }
            .padding(vertical = 12.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InitialAvatar(a)
            Text(a, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(28.dp, 20.dp).clip(RoundedCornerShape(6.dp)).background(AppAccent), contentAlignment = Alignment.Center) {
            Text("vs", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            Text(b, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            InitialAvatar(b)
        }
    }
}
