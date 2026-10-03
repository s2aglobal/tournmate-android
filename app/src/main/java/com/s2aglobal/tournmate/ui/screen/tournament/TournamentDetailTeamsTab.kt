package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.TournamentFormat
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.WarningOrange

@Composable
internal fun TeamsTabContent(
    state: TournamentDetailUiState,
    onPlayerClick: (String) -> Unit,
    onPartnerUp: (Player) -> Unit,
    onRegisterWithPartner: (Player) -> Unit,
) {
    val tournament = state.tournament ?: return
    val formed = state.formedTeams
    val solos = state.soloRegistrations

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("REGISTERED TEAMS (${formed.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.FilterList, null, Modifier.size(20.dp), tint = Color.Gray)
        }

        if (formed.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.PersonOff, null, Modifier.size(36.dp), tint = Color.Gray.copy(alpha = 0.3f))
                Text("No complete teams yet", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(
                    if (tournament.format.isDoubles) "Players need to pair up to form teams." else "Waiting for registrations to start.",
                    fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                formed.forEachIndexed { index, reg ->
                    TeamCard(reg, index, tournament.format, onPlayerClick)
                }
            }
        }

        if (solos.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color(0xFFE5E5EA))
            Text("AWAITING PARTNER (${solos.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                solos.forEachIndexed { index, reg ->
                    val isSelf = reg.player.id == state.currentPlayer?.id
                    SoloPlayerCard(
                        reg = reg,
                        index = index,
                        isSelf = isSelf,
                        canPick = state.canPickPartnerFromTeams && !isSelf,
                        canRegisterWith = !state.isRegistered && state.needsPartnerPick && !isSelf && state.currentPlayer != null,
                        onPlayerClick = onPlayerClick,
                        onPartnerUp = { onPartnerUp(reg.player) },
                        onRegisterWith = { onRegisterWithPartner(reg.player) },
                    )
                }
            }
        }

        state.statusMessage?.let {
            Text(it, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}

private fun Modifier.teamCardStyle(): Modifier = this
    .fillMaxWidth()
    .shadow(3.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.03f), spotColor = Color.Black.copy(alpha = 0.03f))
    .clip(RoundedCornerShape(16.dp))
    .background(Color.White)
    .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    .padding(12.dp)

@Composable
private fun SeedCircle(number: Int, color: Color) {
    Box(Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
        Text("$number", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PlayerLink(name: String, onClick: () -> Unit) {
    Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.clickable(onClick = onClick))
}

private val Player.genderLabel: String
    get() = gender.displayName

@Composable
private fun TeamCard(reg: Registration, index: Int, format: TournamentFormat, onPlayerClick: (String) -> Unit) {
    Row(Modifier.teamCardStyle(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SeedCircle(index + 1, AppAccent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PlayerLink(reg.player.name) { onPlayerClick(reg.playerId) }
                if (format.isDoubles) {
                    Text("&", fontSize = 14.sp, color = Color.Gray)
                    val partner = reg.partner
                    if (partner != null) {
                        PlayerLink(partner.name) { onPlayerClick(reg.partnerId ?: partner.id.toString()) }
                    } else {
                        Text("?", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
            val partner = reg.partner
            if (format == TournamentFormat.MIXED_DOUBLES && partner != null) {
                Text("${reg.player.genderLabel} + ${partner.genderLabel}", fontSize = 10.sp, color = Color.Gray)
            }
        }
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(AppAccent.copy(alpha = 0.05f)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Default.GppGood, null, Modifier.size(11.dp), tint = AppAccent)
            Text("PAID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppAccent)
        }
    }
}

@Composable
private fun SoloPlayerCard(
    reg: Registration,
    index: Int,
    isSelf: Boolean,
    canPick: Boolean,
    canRegisterWith: Boolean,
    onPlayerClick: (String) -> Unit,
    onPartnerUp: () -> Unit,
    onRegisterWith: () -> Unit,
) {
    Row(Modifier.teamCardStyle(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SeedCircle(index + 1, WarningOrange)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PlayerLink(reg.player.name) { onPlayerClick(reg.playerId) }
            Text(reg.player.genderLabel, fontSize = 10.sp, color = Color.Gray)
        }
        when {
            canPick -> Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(AppAccent.copy(alpha = 0.08f)).clickable(onClick = onPartnerUp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(11.dp), tint = AppAccent)
                Text("PARTNER UP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppAccent)
            }
            canRegisterWith -> StatusChip("NEEDS PARTNER", WarningOrange, onRegisterWith)
            else -> StatusChip(if (isSelf) "YOU" else "NEEDS PARTNER", if (isSelf) AppAccent else WarningOrange, null)
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color, onClick: (() -> Unit)?) {
    Text(
        text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.05f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
