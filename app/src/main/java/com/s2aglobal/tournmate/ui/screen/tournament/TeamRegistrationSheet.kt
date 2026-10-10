package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TeamRepository
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Team
import com.s2aglobal.tournmate.domain.model.Tournament
import com.s2aglobal.tournmate.ui.component.sheetScrollLikeIos
import com.s2aglobal.tournmate.ui.theme.theme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TeamRegistrationUiState(
    val teams: List<Team> = emptyList(),
    val selectedTeamId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Player ids playing in this event. */
    val rosterIds: Set<String> = emptySet(),
    val guestNames: List<String> = emptyList(),
    val lookupMessage: String? = null,
    val showNewTeam: Boolean = false,
    val registered: Boolean = false,
) {
    val selectedTeam: Team? get() = teams.firstOrNull { it.id == selectedTeamId }
    val rosterCount: Int get() = rosterIds.size + guestNames.size
}

/**
 * Team registration for a team-sport tournament (cricket). The captain picks
 * one of their teams (or creates one), adds players by exact email or phone,
 * chooses who plays in this event, and can name guests. Mirrors iOS
 * `TeamRegistrationSheet`.
 */
@HiltViewModel
class TeamRegistrationViewModel @Inject constructor(
    private val teamRepo: TeamRepository,
    private val playerRepo: PlayerRepository,
    private val registrationRepo: RegistrationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TeamRegistrationUiState())
    val state: StateFlow<TeamRegistrationUiState> = _state.asStateFlow()

    private var tournament: Tournament = Tournament()
    private var captain: Player = Player()
    /** Players already entered in this tournament (upper-cased id → team name). */
    private var enteredPlayers: Map<String, String> = emptyMap()

    /** The team this player already plays for in this tournament, if any. */
    fun takenBy(player: Player): String? = enteredPlayers[player.id.toString().uppercase()]

    /** Roster limit: a full side plus four substitutes. */
    val rosterLimit: Int get() = tournament.cricketRules.playersPerSide + 4

    fun start(tournament: Tournament, captain: Player, enteredPlayers: Map<String, String> = emptyMap()) {
        this.tournament = tournament
        this.captain = captain
        this.enteredPlayers = enteredPlayers
        _state.value = TeamRegistrationUiState()
        viewModelScope.launch {
            try {
                val teams = teamRepo.myTeams(captain.id.toString().uppercase(), tournament.sportType)
                _state.update { it.copy(teams = teams, isLoading = false) }
                teams.firstOrNull()?.let(::select)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = "Couldn't load your teams. ${e.message.orEmpty()}") }
            }
        }
    }

    fun select(team: Team) {
        // Default roster: everyone not already playing for another team in
        // this tournament, up to a full side plus substitutes.
        _state.update {
            it.copy(
                selectedTeamId = team.id,
                rosterIds = team.players.filter { p -> takenBy(p) == null }.take(rosterLimit)
                    .map { p -> p.id.toString().uppercase() }.toSet(),
                guestNames = emptyList(),
                lookupMessage = null,
            )
        }
    }

    fun showNewTeam() = _state.update { it.copy(showNewTeam = true) }

    fun toggleRoster(player: Player) {
        val id = player.id.toString().uppercase()
        _state.update {
            when {
                id in it.rosterIds -> it.copy(rosterIds = it.rosterIds - id)
                takenBy(player) != null -> it
                it.rosterCount < rosterLimit -> it.copy(rosterIds = it.rosterIds + id)
                else -> it
            }
        }
    }

    /** Returns true when the guest was added (so the field can be cleared). */
    fun addGuest(raw: String): Boolean {
        val name = raw.trim()
        val s = _state.value
        if (name.length < 2 || name in s.guestNames || s.rosterCount >= rosterLimit) return false
        _state.update { it.copy(guestNames = it.guestNames + name) }
        return true
    }

    fun removeGuest(name: String) = _state.update { it.copy(guestNames = it.guestNames - name) }

    fun createTeam(rawName: String, onCreated: () -> Unit) {
        val uid = captain.firebaseUid ?: run {
            _state.update { it.copy(errorMessage = "Sign in again to create a team.") }
            return
        }
        val name = rawName.trim().take(Team.NAME_LIMIT)
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val team = teamRepo.createTeam(name, tournament.sportType, captain, uid)
                _state.update { it.copy(teams = it.teams + team, showNewTeam = false, isSaving = false) }
                select(team)
                onCreated()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Couldn't create the team. ${e.message.orEmpty()}") }
            }
        }
    }

    fun addPlayer(rawQuery: String, onAdded: () -> Unit) {
        val team = _state.value.selectedTeam ?: return
        val query = rawQuery.trim()
        val byEmail = query.contains("@")
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val found = if (byEmail) playerRepo.findPlayerByEmail(query.lowercase()) else playerRepo.findPlayerByPhone(query)
                when {
                    found == null -> _state.update {
                        it.copy(lookupMessage = "No TournMate player uses that ${if (byEmail) "email" else "phone number"}. Add them as a guest instead.")
                    }
                    team.contains(found) -> _state.update { it.copy(lookupMessage = "${found.name} is already on the team.") }
                    else -> {
                        teamRepo.joinTeam(team, found)
                        val id = found.id.toString().uppercase()
                        // Already playing for another team here: on the team, but not in this event's roster.
                        val otherTeam = takenBy(found)
                        _state.update { s ->
                            s.copy(
                                teams = s.teams.map { t ->
                                    if (t.id == team.id) t.copy(playerIds = t.playerIds + id, players = t.players + found) else t
                                },
                                rosterIds = if (otherTeam == null && s.rosterCount < rosterLimit) s.rosterIds + id else s.rosterIds,
                                lookupMessage = if (otherTeam != null) {
                                    "${found.name} is already playing for $otherTeam in this tournament."
                                } else {
                                    "${found.name} added."
                                },
                            )
                        }
                        onAdded()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(lookupMessage = "Couldn't add the player. ${e.message.orEmpty()}") }
            }
            _state.update { it.copy(isSaving = false) }
        }
    }

    fun register() {
        val s = _state.value
        val team = s.selectedTeam ?: return
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                // Keep the roster in team order for a stable team sheet.
                val ordered = team.players.filter { takenBy(it) == null }.map { it.id.toString().uppercase() }.filter { it in s.rosterIds }
                registrationRepo.createTeamRegistration(team, captain, ordered, s.guestNames, tournament)
                _state.update { it.copy(isSaving = false, registered = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Registration failed. ${e.message.orEmpty()}") }
            }
        }
    }
}

private val TeamSheetGray6 = Color(0xFFF2F2F7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TeamRegistrationSheet(
    tournament: Tournament,
    captain: Player,
    onRegistered: () -> Unit,
    onCancel: () -> Unit,
    /** Players already entered in this tournament (`Registration.enteredPlayers`). */
    enteredPlayers: Map<String, String> = emptyMap(),
    viewModel: TeamRegistrationViewModel = hiltViewModel(key = "teamRegistration-${tournament.id}"),
) {
    val state by viewModel.state.collectAsState()
    val accent = tournament.sportType.theme.primary
    val rules = tournament.cricketRules
    val rosterLimit = viewModel.rosterLimit
    var newTeamName by remember { mutableStateOf("") }
    var guestDraft by remember { mutableStateOf("") }
    var lookup by remember { mutableStateOf("") }

    LaunchedEffect(tournament.id, captain.id) { viewModel.start(tournament, captain, enteredPlayers) }
    LaunchedEffect(state.registered) { if (state.registered) onRegistered() }

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.95f)) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                    Text("Cancel", color = accent, fontSize = 16.sp)
                }
                Text("Register a Team", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
            }
            Column(
                Modifier.weight(1f).sheetScrollLikeIos().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(rules.eventType.displayName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = accent)
                    Text(
                        "Name at least ${rules.playersPerSide} players for a full side. You can add up to ${rosterLimit - rules.playersPerSide} substitutes.",
                        fontSize = 14.sp, color = Color.Gray,
                    )
                }

                if (state.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                } else {
                    // Your team
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionCaption("YOUR TEAM")
                        state.teams.forEach { team ->
                            val selected = state.selectedTeamId == team.id
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TeamSheetGray6)
                                    .clickable { viewModel.select(team) }.padding(14.dp)
                                    .semantics { this.selected = selected },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                                    tint = if (selected) accent else Color.Gray,
                                )
                                Column {
                                    Text(team.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    val n = team.playerIds.size
                                    Text("$n ${if (n == 1) "player" else "players"}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                        if (state.showNewTeam || state.teams.isEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SheetField(
                                    newTeamName, { newTeamName = it.take(Team.NAME_LIMIT) }, "Team name, e.g. Austin Strikers",
                                    Modifier.weight(1f), capitalization = KeyboardCapitalization.Words,
                                )
                                TextButton(
                                    onClick = { viewModel.createTeam(newTeamName) { newTeamName = "" } },
                                    enabled = newTeamName.trim().length >= 2 && !state.isSaving,
                                ) { Text("Create", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent) }
                            }
                        } else {
                            Row(
                                Modifier.clickable { viewModel.showNewTeam() }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Default.AddCircle, null, Modifier.size(18.dp), tint = accent)
                                Text("Create a new team", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = accent)
                            }
                        }
                    }

                    state.selectedTeam?.let { team ->
                        // Playing in this event
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SectionCaption("PLAYING IN THIS EVENT", Modifier.weight(1f))
                                Text(
                                    "${state.rosterCount} of ${rules.playersPerSide}", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (state.rosterCount >= rules.playersPerSide) Color(0xFF34C759) else Color(0xFFFF9500),
                                )
                            }
                            team.players.forEach { player ->
                                val on = player.id.toString().uppercase() in state.rosterIds
                                val otherTeam = viewModel.takenBy(player)
                                Row(
                                    Modifier.fillMaxWidth().clickable(enabled = otherTeam == null) { viewModel.toggleRoster(player) }
                                        .alpha(if (otherTeam != null) 0.5f else 1f)
                                        .padding(vertical = 8.dp)
                                        .semantics { selected = on },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        if (on) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, null,
                                        tint = if (on) accent else Color.Gray,
                                    )
                                    Column {
                                        Text(player.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (otherTeam != null) {
                                            Text("Already playing for $otherTeam", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    if (team.isCaptain(player)) {
                                        Text(
                                            "CAPTAIN", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = accent,
                                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(accent.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }

                        // Guests
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionCaption("GUESTS (NO ACCOUNT)")
                            state.guestNames.forEach { name ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Default.PersonOutline, null, tint = Color.Gray)
                                    Text(name, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.removeGuest(name) }) {
                                        Icon(Icons.Default.Cancel, "Remove $name", tint = Color.Gray)
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SheetField(guestDraft, { guestDraft = it }, "Guest name", Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
                                TextButton(onClick = { if (viewModel.addGuest(guestDraft)) guestDraft = "" }) {
                                    Text("Add", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent)
                                }
                            }
                            Text("Guests appear on the team sheet but don't get a rating.", fontSize = 11.sp, color = Color.Gray)
                        }

                        // Add a player to the team
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionCaption("ADD A PLAYER TO ${team.name.uppercase()}")
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SheetField(
                                    lookup, { lookup = it }, "Email or phone of a TournMate player", Modifier.weight(1f),
                                    keyboardType = KeyboardType.Email,
                                )
                                TextButton(
                                    onClick = { viewModel.addPlayer(lookup) { lookup = "" } },
                                    enabled = lookup.trim().length >= 5 && !state.isSaving,
                                ) { Text("Add", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent) }
                            }
                            state.lookupMessage?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                        }
                    }
                }

                state.errorMessage?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Red) }
            }

            // Register bar
            val team = state.selectedTeam
            val ready = team != null && state.rosterCount >= 2 && !state.isSaving
            Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().imePadding()) {
                HorizontalDivider(color = ScoreDivider)
                Button(
                    onClick = viewModel::register,
                    enabled = ready,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        disabledContainerColor = if (state.isSaving) accent else Color.Gray,
                        disabledContentColor = Color.White,
                    ),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (team == null) "CHOOSE A TEAM" else "REGISTER ${team.name.uppercase()}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.Gray, modifier = modifier)
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray) },
        singleLine = true,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            autoCorrectEnabled = keyboardType != KeyboardType.Email,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = TeamSheetGray6,
            unfocusedContainerColor = TeamSheetGray6,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
