package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Stadium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.ui.theme.AppAccent
import com.s2aglobal.tournmate.ui.theme.WarningOrange

private val ManageGray6 = Color(0xFFF2F2F7)

@Composable
internal fun ManageTabContent(
    state: TournamentDetailUiState,
    viewModel: TournamentDetailViewModel,
    onResetMatches: () -> Unit,
    onCancelTournament: () -> Unit,
    onDeleteTournament: () -> Unit,
) {
    val tournament = state.tournament ?: return
    val complete = state.isTournamentComplete
    val busy = state.isLoading

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
        if (complete) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AppAccent.copy(alpha = 0.05f))
                    .border(1.dp, AppAccent.copy(alpha = 0.15f), RoundedCornerShape(24.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.SportsScore, null, Modifier.size(36.dp), tint = AppAccent)
                Text("Tournament Completed", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("This event has ended. Match results and standings are final.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
            if (tournament.matchFormat != MatchFormat.ROUND_ROBIN && tournament.matchFormat != MatchFormat.MANUAL_DRAW) {
                ChampionCard(state)
            }
        }

        if (!complete) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AppAccent.copy(alpha = 0.05f))
                    .border(1.dp, AppAccent.copy(alpha = 0.1f), RoundedCornerShape(24.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(24.dp), tint = AppAccent)
                    Text("BRACKET GENERATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Text(
                    "Ready to start the event? This will close registrations and generate the first round of matches based on seeded teams.",
                    fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp,
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (tournament.format.isDoubles && !state.pairingsGenerated && state.soloRegistrations.size >= 2) {
                        AdminActionButton("GENERATE RANDOM PAIRINGS", Icons.Default.Shuffle, busy) { viewModel.generateRandomPairs() }
                    }

                    if (tournament.matchFormat != MatchFormat.MANUAL_DRAW && !state.matchesGenerated && state.formedTeams.size >= 2) {
                        val (title, icon) = when (tournament.matchFormat) {
                            MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> "GENERATE BRACKET" to Icons.Default.PlayCircle
                            MatchFormat.ROUND_ROBIN -> "GENERATE ROUND-ROBIN" to Icons.Outlined.Stadium
                            MatchFormat.GROUP_KNOCKOUT -> "GENERATE GROUPS" to Icons.Default.ViewModule
                            MatchFormat.SWISS -> "GENERATE SWISS ROUND 1" to Icons.Default.FormatListNumbered
                            MatchFormat.MANUAL_DRAW -> "GENERATE MATCHES" to Icons.Outlined.Draw
                        }
                        AdminActionButton(title, icon, busy) {
                            when (tournament.matchFormat) {
                                MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> viewModel.generateBracket()
                                MatchFormat.ROUND_ROBIN -> viewModel.generateRoundRobinMatches()
                                MatchFormat.GROUP_KNOCKOUT -> viewModel.generateGroupKnockoutMatches()
                                MatchFormat.SWISS -> viewModel.generateNextSwissRound()
                                MatchFormat.MANUAL_DRAW -> Unit
                            }
                        }
                    }

                    if (tournament.matchFormat == MatchFormat.MANUAL_DRAW && state.formedTeams.size >= 2) {
                        ManualDrawSection(state, busy) { a, b, round -> viewModel.createManualMatch(a, b, round) }
                    }

                    val isElim = tournament.matchFormat == MatchFormat.SINGLE_ELIMINATION || tournament.matchFormat == MatchFormat.DOUBLE_ELIMINATION
                    if (isElim && state.matchesGenerated && state.currentRoundFullyFinished) {
                        if (state.currentMaxRound < state.totalBracketRounds) {
                            AdminActionButton("ADVANCE TO NEXT ROUND", Icons.Default.ArrowCircleRight, busy) { viewModel.advanceToNextRound() }
                        } else if (state.currentMaxRound == state.totalBracketRounds) {
                            ChampionCard(state)
                        }
                    }

                    if (tournament.matchFormat == MatchFormat.GROUP_KNOCKOUT && state.matchesGenerated) {
                        if (state.isGroupStageComplete && !state.hasKnockoutMatches) {
                            AdminActionButton("ADVANCE TO KNOCKOUT", Icons.Default.Bolt, busy) { viewModel.advanceGroupToKnockout() }
                        }
                        if (state.hasKnockoutMatches && state.currentRoundFullyFinished) {
                            if (state.currentMaxRound < state.totalBracketRounds) {
                                AdminActionButton("ADVANCE TO NEXT ROUND", Icons.Default.ArrowCircleRight, busy) { viewModel.advanceToNextRound() }
                            } else if (state.currentMaxRound == state.totalBracketRounds) {
                                ChampionCard(state)
                            }
                        }
                    }

                    if (tournament.matchFormat == MatchFormat.SWISS && state.matchesGenerated) {
                        if (!state.isSwissComplete && state.isCurrentSwissRoundComplete) {
                            AdminActionButton("GENERATE SWISS ROUND ${state.currentSwissRound + 1}", Icons.Default.FormatListNumbered, busy) { viewModel.generateNextSwissRound() }
                        }
                        if (state.isSwissComplete) ChampionCard(state)
                    }

                    if (state.matchesGenerated) {
                        OutlinedDangerButton("RESET ALL MATCHES", Icons.Outlined.Delete, busy, filled = false, onClick = onResetMatches)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("DANGER ZONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 2.sp)
                val cutoff = Tournament.CANCELLATION_CUTOFF_HOURS.toInt()
                when {
                    tournament.status == TournamentStatus.CANCELLED -> Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Red.copy(alpha = 0.04f))
                            .border(1.dp, Color.Red.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.Report, null, Modifier.size(28.dp), tint = Color.Red)
                        Text("Tournament Cancelled", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Text("This tournament has been cancelled. Registered players have been notified.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                    tournament.canCancelOrDelete -> OutlinedDangerButton("CANCEL TOURNAMENT", Icons.Outlined.Cancel, busy, filled = false, onClick = onCancelTournament)
                    else -> LockedActionCard("Cannot Cancel", "Tournaments can only be cancelled at least $cutoff hours before the start time.")
                }
                if (tournament.canCancelOrDelete) {
                    OutlinedDangerButton("DELETE TOURNAMENT", Icons.Outlined.Delete, busy, filled = true, onClick = onDeleteTournament)
                } else if (tournament.status != TournamentStatus.CANCELLED) {
                    LockedActionCard("Cannot Delete", "Tournaments can only be deleted at least $cutoff hours before the start time.")
                }
            }
        }

        state.statusMessage?.let { msg ->
            val success = isSuccessStatus(msg)
            val tint = if (success) AppAccent else WarningOrange
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.08f))
                    .border(1.dp, tint.copy(alpha = 0.15f), RoundedCornerShape(14.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(if (success) Icons.Default.CheckCircle else Icons.Default.Warning, null, Modifier.size(16.dp), tint = tint)
                Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
            }
        }
    }
}

private fun isSuccessStatus(msg: String): Boolean {
    val lower = msg.lowercase()
    return listOf("failed", "error", "could not", "cannot", "need", "not enough", "not all", "closed", "already").none { lower.contains(it) }
}

@Composable
private fun AdminActionButton(title: String, icon: ImageVector, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppAccent, disabledContainerColor = AppAccent.copy(alpha = 0.6f)),
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Icon(icon, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OutlinedDangerButton(title: String, icon: ImageVector, isLoading: Boolean, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(if (filled) Color.Red.copy(alpha = 0.05f) else Color.White)
            .then(if (filled) Modifier else Modifier.border(1.dp, Color.Red.copy(alpha = 0.2f), shape))
            .clickable(enabled = !isLoading, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.Red, strokeWidth = 2.dp)
        else Icon(icon, null, Modifier.size(18.dp), tint = Color.Red)
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
    }
}

@Composable
private fun LockedActionCard(title: String, message: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Gray.copy(alpha = 0.06f))
            .border(1.dp, Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Lock, null, Modifier.size(16.dp), tint = Color.Gray)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(message, fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f), lineHeight = 15.sp)
        }
    }
}

@Composable
private fun ChampionCard(state: TournamentDetailUiState) {
    val winner = state.matches.lastOrNull { it.round == state.currentMaxRound && it.groupLabel == null }?.winnerRegistration
        ?: state.matches.lastOrNull { it.round == state.currentSwissRound }?.winnerRegistration
        ?: return
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFCC00).copy(alpha = 0.12f), WarningOrange.copy(alpha = 0.08f))))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.EmojiEvents, null, Modifier.size(44.dp), tint = Color(0xFFFFCC00))
        Text("🏆 Champion 🏆", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(registrationShortName(winner), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ManualDrawSection(state: TournamentDetailUiState, busy: Boolean, onCreate: (Registration, Registration, Int) -> Unit) {
    var teamA by remember { mutableStateOf<Registration?>(null) }
    var teamB by remember { mutableStateOf<Registration?>(null) }
    val teams = state.formedTeams
    val nextRound = (state.matches.mapNotNull { it.round }.maxOrNull() ?: 0) + 1

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CREATE A MATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 2.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TeamMenu(teams, teamA, "Select Team A") {
                teamA = it
                if (teamB?.id == it.id) teamB = null
            }
            TeamMenu(teams.filter { it.id != teamA?.id }, teamB, "Select Team B") { teamB = it }
        }
        val ready = teamA != null && teamB != null
        Button(
            onClick = {
                val a = teamA ?: return@Button
                val b = teamB ?: return@Button
                onCreate(a, b, nextRound)
                teamA = null
                teamB = null
            },
            enabled = !busy && ready,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppAccent, disabledContainerColor = if (busy) AppAccent.copy(alpha = 0.6f) else Color.Gray, disabledContentColor = Color.White),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.AddCircle, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("CREATE MATCH", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TeamMenu(teams: List<Registration>, selected: Registration?, placeholder: String, onSelect: (Registration) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ManageGray6).clickable { expanded = true }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selected?.let { registrationFullName(it) } ?: placeholder, fontSize = 15.sp, color = if (selected != null) Color.Black else Color.Gray, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp), tint = Color.Gray)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            teams.forEach { team ->
                DropdownMenuItem(text = { Text(registrationFullName(team)) }, onClick = { onSelect(team); expanded = false })
            }
        }
    }
}
