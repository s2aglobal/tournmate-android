package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RegistrationAlreadyEnteredException
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TeamRepository
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.PlayingRole
import com.s2aglobal.tournmate.domain.model.RosterMember
import com.s2aglobal.tournmate.domain.model.Team
import com.s2aglobal.tournmate.domain.model.TeamRoles
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
    /** Captain, vice-captain, wicketkeeper and playing roles as chosen (see [TeamRegistrationViewModel.currentRoles]). */
    val roles: TeamRoles = TeamRoles(),
) {
    val selectedTeam: Team? get() = teams.firstOrNull { it.id == selectedTeamId }
    val rosterCount: Int get() = rosterIds.size + guestNames.size
}

/**
 * Team registration for a team-sport tournament (cricket). Anyone on a team
 * can register it (they needn't be captain, or play): pick one of your teams
 * (or create one), add players by email or phone, choose who plays in this
 * event, name guests without an account, and set roles (captain,
 * vice-captain, wicketkeeper, batter/bowler…) from each player's sheet. Layout: team summary
 * (count + pips for the side and substitutes), team chips, the event roster,
 * then players to add. Mirrors iOS `TeamRegistrationSheet`.
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
    /** Whoever is registering; the captain is chosen separately (`roles`). */
    private var registrant: Player = Player()
    /** Players already entered in this tournament (upper-cased id → team name). */
    private var enteredPlayers: Map<String, String> = emptyMap()

    /** The team this player already plays for in this tournament, if any. */
    fun takenBy(player: Player): String? = enteredPlayers[player.id.toString().uppercase()]

    /** Roster limit: a full side plus four substitutes. */
    val rosterLimit: Int get() = tournament.cricketRules.playersPerSide + 4

    fun start(tournament: Tournament, registrant: Player, enteredPlayers: Map<String, String> = emptyMap()) {
        this.tournament = tournament
        this.registrant = registrant
        this.enteredPlayers = enteredPlayers
        _state.value = TeamRegistrationUiState()
        viewModelScope.launch {
            try {
                val teams = teamRepo.myTeams(registrant.id.toString().uppercase(), tournament.sportType)
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
                roles = TeamRoles(),
            )
        }
    }

    fun toggleNewTeam() = _state.update { it.copy(showNewTeam = !it.showNewTeam) }

    /** Team players named for this event, in team order. */
    fun namedPlayers(team: Team, state: TeamRegistrationUiState = _state.value): List<Player> =
        team.players.filter { it.id.toString().uppercase() in state.rosterIds && takenBy(it) == null }

    /** Everyone playing in this event: team players, then guests. */
    fun members(team: Team, state: TeamRegistrationUiState = _state.value): List<RosterMember> =
        namedPlayers(team, state).map(RosterMember::of) + state.guestNames.map(RosterMember::guest)

    /**
     * Roles for the side as it stands (always a captain while anyone plays;
     * whoever registers is captain until someone else is chosen).
     */
    fun currentRoles(team: Team, state: TeamRegistrationUiState = _state.value): TeamRoles =
        state.roles.normalized(members(team, state).map { it.key }, preferredCaptain = registrant.id.toString())

    fun setRoles(roles: TeamRoles) = _state.update { it.copy(roles = roles) }

    /** Takes a player or guest out of this event. */
    fun remove(member: RosterMember) {
        val player = member.player
        if (player != null) removeFromRoster(player) else removeGuest(member.name)
    }

    /** "You", "TournMate player" or "Guest · no rating". */
    fun baseSubtitle(member: RosterMember): String = when {
        member.isGuest -> "Guest · no rating"
        member.player?.id == registrant.id -> "You"
        else -> "TournMate player"
    }

    /** "You · All-rounder", "Guest · Bowler", "Batter"; without a role the full [baseSubtitle]. */
    fun rowSubtitle(member: RosterMember, role: PlayingRole?): String {
        if (role == null) return baseSubtitle(member)
        val who = when {
            member.isGuest -> "Guest"
            member.player?.id == registrant.id -> "You"
            else -> null
        }
        return listOfNotNull(who, role.title).joinToString(" · ")
    }

    /** Puts a team player in this event (unless taken by another team or the roster is full). */
    fun addToRoster(player: Player) {
        val id = player.id.toString().uppercase()
        _state.update {
            if (id in it.rosterIds || takenBy(player) != null || it.rosterCount >= rosterLimit) it
            else it.copy(rosterIds = it.rosterIds + id)
        }
    }

    fun removeFromRoster(player: Player) {
        val id = player.id.toString().uppercase()
        _state.update { it.copy(rosterIds = it.rosterIds - id) }
    }

    fun removeGuest(name: String) = _state.update { it.copy(guestNames = it.guestNames - name) }

    fun createTeam(rawName: String, onCreated: () -> Unit) {
        val uid = registrant.firebaseUid ?: run {
            _state.update { it.copy(errorMessage = "Sign in again to create a team.") }
            return
        }
        val name = rawName.trim().take(Team.NAME_LIMIT)
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val team = teamRepo.createTeam(name, tournament.sportType, registrant, uid)
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

    /**
     * The single add field: an email or phone number looks up a TournMate
     * player (added to the team, and to this event unless they already play
     * for another team here); anything else is added as a guest name.
     * [onClear] runs when the field should be emptied.
     */
    fun addFromField(rawQuery: String, onClear: () -> Unit) {
        val team = _state.value.selectedTeam ?: return
        val query = rawQuery.trim()
        val digits = query.count { it.isDigit() }
        val isEmail = query.contains("@")
        val isPhone = !isEmail && digits >= 7 && digits >= query.count { !it.isWhitespace() } - 3
        if (!isEmail && !isPhone) {
            // Guest
            if (query.length < 2) return
            if (_state.value.guestNames.any { it.equals(query, ignoreCase = true) }) {
                _state.update { it.copy(lookupMessage = "$query is already on the list.") }
                return
            }
            _state.update { it.copy(guestNames = it.guestNames + query, lookupMessage = "$query added as a guest.") }
            onClear()
            return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val found = if (isEmail) playerRepo.findPlayerByEmail(query.lowercase()) else playerRepo.findPlayerByPhone(query)
                if (found == null) {
                    _state.update {
                        it.copy(
                            lookupMessage = "No TournMate player uses that ${if (isEmail) "email" else "phone number"}. " +
                                "Ask them to sign up, or type their name to add them as a guest.",
                        )
                    }
                } else {
                    val id = found.id.toString().uppercase()
                    if (!team.contains(found)) {
                        teamRepo.joinTeam(team, found)
                        _state.update { s ->
                            s.copy(teams = s.teams.map { t ->
                                if (t.id == team.id) t.copy(playerIds = t.playerIds + id, players = t.players + found) else t
                            })
                        }
                    }
                    onClear()
                    val otherTeam = takenBy(found)
                    _state.update { s ->
                        when {
                            // On the team, but not in this event's roster.
                            otherTeam != null -> s.copy(lookupMessage = "${found.name} is already playing for $otherTeam in this tournament.")
                            id in s.rosterIds -> s.copy(lookupMessage = "${found.name} is already playing in this event.")
                            else -> s.copy(
                                rosterIds = if (s.rosterCount < rosterLimit) s.rosterIds + id else s.rosterIds,
                                lookupMessage = "${found.name} added.",
                            )
                        }
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
                registrationRepo.createTeamRegistration(team, registrant, ordered, s.guestNames, currentRoles(team, s), tournament)
                _state.update { it.copy(isSaving = false, registered = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: RegistrationAlreadyEnteredException) {
                _state.update { it.copy(isSaving = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Registration failed. ${e.message.orEmpty()}") }
            }
        }
    }
}

// iOS system colours (the app is light-only, like the rest of this screen family).
private val GroupedBackground = Color(0xFFF2F2F7)
private val CardBackground = Color.White
private val Secondary = Color(0xFF8E8E93)
private val Gray2 = Color(0xFFAEAEB2)
private val Gray3 = Color(0xFFC7C7CC)
private val TertiaryFill = Color(0x1F767680)
private val Label = Color.Black
private val ShortOrange = Color(0xFFFF9500)
private val FullGreen = Color(0xFF34C759)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TeamRegistrationSheet(
    tournament: Tournament,
    /** Whoever is registering; the captain is chosen separately (roles). */
    registrant: Player,
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
    var lookup by remember { mutableStateOf("") }

    var roleTarget by remember { mutableStateOf<RosterMember?>(null) }

    LaunchedEffect(tournament.id, registrant.id) { viewModel.start(tournament, registrant, enteredPlayers) }
    LaunchedEffect(state.registered) { if (state.registered) onRegistered() }

    val shortBy = maxOf(0, rules.playersPerSide - state.rosterCount)
    val substituteSlots = rosterLimit - rules.playersPerSide

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GroupedBackground,
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
                Modifier.weight(1f).sheetScrollLikeIos().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                } else {
                    val selectedTeam = state.selectedTeam
                    if (selectedTeam != null) {
                        SummaryCard(selectedTeam, captainLine(viewModel, selectedTeam, state), state.rosterCount, rules.playersPerSide, substituteSlots, shortBy, accent)
                    } else {
                        IntroCard(rules.eventType.displayName, rules.playersPerSide, substituteSlots, accent)
                    }
                    TeamChips(
                        teams = state.teams,
                        selectedTeamId = state.selectedTeamId,
                        showNewTeam = state.showNewTeam,
                        newTeamName = newTeamName,
                        onNewTeamNameChange = { newTeamName = it.take(Team.NAME_LIMIT) },
                        createEnabled = newTeamName.trim().length >= 2 && !state.isSaving,
                        onSelect = viewModel::select,
                        onToggleNewTeam = viewModel::toggleNewTeam,
                        onCreate = { viewModel.createTeam(newTeamName) { newTeamName = "" } },
                        accent = accent,
                    )
                    if (selectedTeam != null) {
                        RosterSection(selectedTeam, state, viewModel, accent) { member ->
                            viewModel.setRoles(viewModel.currentRoles(selectedTeam)) // start from the side as shown
                            roleTarget = member
                        }
                        val addEnabled = lookup.trim().length >= 2 && !state.isSaving && state.rosterCount < rosterLimit
                        AddSection(
                            team = selectedTeam,
                            state = state,
                            viewModel = viewModel,
                            lookup = lookup,
                            onLookupChange = { lookup = it },
                            addEnabled = addEnabled,
                            onAdd = { if (addEnabled) viewModel.addFromField(lookup) { lookup = "" } },
                            rosterLimit = rosterLimit,
                            accent = accent,
                        )
                    }
                }

                state.errorMessage?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Red) }
            }

            RegisterBar(state, shortBy, accent, viewModel::register)
        }
    }

    roleTarget?.let { member ->
        RosterRoleSheet(
            member = member,
            subtitle = viewModel.baseSubtitle(member),
            roles = state.roles,
            onRolesChange = viewModel::setRoles,
            onRemove = { viewModel.remove(member) },
            onDismiss = { roleTarget = null },
        )
    }
}

/** "Captain · Ravi Kumar" for the side as it stands. */
private fun captainLine(viewModel: TeamRegistrationViewModel, team: Team, state: TeamRegistrationUiState): String {
    val key = viewModel.currentRoles(team, state).captain
    val captain = key?.let { k -> viewModel.members(team, state).firstOrNull { it.key == k } } ?: return "No captain yet"
    return "Captain · ${captain.name}"
}

// MARK: - Summary

@Composable
private fun SummaryCard(
    team: Team,
    captainLine: String,
    rosterCount: Int,
    playersPerSide: Int,
    substituteSlots: Int,
    shortBy: Int,
    accent: Color,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials(team.name), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(team.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(captainLine, fontSize = 14.sp, color = Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "$rosterCount of $playersPerSide players"
                },
            ) {
                Text(
                    "$rosterCount", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (shortBy > 0) ShortOrange else FullGreen,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.width(1.dp))
                Text(
                    "/$playersPerSide", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Secondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        RosterPips(rosterCount, playersPerSide, substituteSlots, accent)
        Row {
            Text(
                if (shortBy > 0) "$shortBy more for a full side" else "Full side",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = if (shortBy > 0) Secondary else FullGreen,
            )
            Spacer(Modifier.weight(1f))
            Text("Up to $substituteSlots substitutes", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
        }
    }
}

/** One pip per place in the side, then dashed pips for substitutes. */
@Composable
private fun RosterPips(rosterCount: Int, playersPerSide: Int, substituteSlots: Int, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(playersPerSide) { i ->
            val on = i < rosterCount
            Box(
                Modifier.weight(1f).height(10.dp).clip(CircleShape)
                    .background(if (on) accent else Color.Transparent)
                    .border(1.2.dp, if (on) accent else Gray3, CircleShape),
            )
        }
        Spacer(Modifier.width(2.dp)) // 4 + 2 + 4 ≈ the iOS 6pt gap between the groups
        repeat(substituteSlots) { i ->
            val filled = rosterCount > playersPerSide + i
            Box(
                Modifier.weight(1f).height(10.dp)
                    .clip(CircleShape)
                    .background(if (filled) accent.copy(alpha = 0.6f) else Color.Transparent)
                    .dashedOutline(Gray3, 1.2.dp, dashed = !filled, dash = 3.dp, gap = 2.dp),
            )
        }
    }
}

@Composable
private fun IntroCard(eventName: String, playersPerSide: Int, substituteSlots: Int, accent: Color) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CardBackground).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(eventName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = accent)
        Text(
            "Create your team, then name at least $playersPerSide players for a full side. You can add up to $substituteSlots substitutes.",
            fontSize = 15.sp, color = Secondary,
        )
    }
}

// MARK: - Team chips

@Composable
private fun TeamChips(
    teams: List<Team>,
    selectedTeamId: String?,
    showNewTeam: Boolean,
    newTeamName: String,
    onNewTeamNameChange: (String) -> Unit,
    createEnabled: Boolean,
    onSelect: (Team) -> Unit,
    onToggleNewTeam: () -> Unit,
    onCreate: () -> Unit,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            teams.forEach { team ->
                val selected = selectedTeamId == team.id
                Row(
                    Modifier.height(46.dp).clip(CircleShape)
                        .background(if (selected) Label else CardBackground)
                        .clickable { onSelect(team) }
                        .semantics { this.selected = selected }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val fg = if (selected) Color.White else Label
                    if (selected) Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = fg)
                    Text(team.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
                }
            }
            Row(
                Modifier.height(46.dp).clip(CircleShape)
                    .dashedOutline(Gray3, 1.5.dp, dash = 5.dp, gap = 4.dp)
                    .clickable(onClick = onToggleNewTeam)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(if (showNewTeam && teams.isNotEmpty()) Icons.Default.Close else Icons.Default.Add, null, Modifier.size(18.dp), tint = Secondary)
                Text("New team", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
            }
        }
        AnimatedVisibility(
            visible = showNewTeam || teams.isEmpty(),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SheetField(
                        newTeamName, onNewTeamNameChange, "Team name, e.g. Austin Strikers", Modifier.weight(1f),
                        capitalize = true, onSubmit = { if (createEnabled) onCreate() },
                    )
                    FieldButton("Create", createEnabled, accent, onCreate)
                }
                if (teams.isNotEmpty()) {
                    Text("A new team is saved to your teams. Only the selected team is entered.", fontSize = 12.sp, color = Secondary)
                }
            }
        }
    }
}

// MARK: - Roster

@Composable
private fun RosterSection(
    team: Team,
    state: TeamRegistrationUiState,
    viewModel: TeamRegistrationViewModel,
    accent: Color,
    onOpenRoles: (RosterMember) -> Unit,
) {
    val all = viewModel.members(team, state)
    val current = viewModel.currentRoles(team, state)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("PLAYING IN THIS EVENT", Modifier.weight(1f))
            Text("${state.rosterCount} added", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
        }
        if (all.isEmpty()) {
            Text(
                "No one yet. Add players from your team, by email or phone, or as guests below.",
                fontSize = 14.sp, color = Secondary,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground).padding(18.dp),
            )
        } else {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)) {
                all.forEachIndexed { idx, member ->
                    RosterRow(
                        index = idx + 1,
                        member = member,
                        subtitle = viewModel.rowSubtitle(member, current.playingRole(member.key)),
                        badges = current.badges(member.key),
                        accent = accent,
                        onClick = { onOpenRoles(member) },
                    )
                    if (idx < all.size - 1) RowDivider(64.dp)
                }
            }
            Text("Tap a player to set captain, vice-captain, wicketkeeper and role.", fontSize = 12.sp, color = Secondary)
        }
    }
}

@Composable
private fun RosterRow(
    index: Int,
    member: RosterMember,
    subtitle: String,
    badges: List<String>,
    accent: Color,
    onClick: () -> Unit,
) {
    val badgeNames = badges.map { when (it) { "C" -> "captain"; "VC" -> "vice-captain"; else -> "wicketkeeper" } }
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClickLabel = "Opens roles for this player", onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${member.name}, ${(listOf(subtitle) + badgeNames).joinToString(", ")}"
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "$index", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Secondary,
            textAlign = TextAlign.Center, modifier = Modifier.width(20.dp),
        )
        Avatar(member.name, member.isGuest, accent)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    member.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                badges.forEach { badge ->
                    Text(
                        badge, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp, color = accent, maxLines = 1,
                        modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
            Text(subtitle, fontSize = 14.sp, color = Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(TertiaryFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.UnfoldMore, null, Modifier.size(18.dp), tint = Secondary)
        }
    }
}

@Composable
private fun Avatar(name: String, isGuest: Boolean, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(if (isGuest) Color.Transparent else accent)
            .then(if (isGuest) Modifier.dashedOutline(Gray2, 1.5.dp, dash = 3.dp, gap = 3.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials(name), fontSize = if (isGuest) 15.sp else 16.sp, fontWeight = FontWeight.Bold,
            color = if (isGuest) Secondary else Color.White,
        )
    }
}

// MARK: - Adding players

@Composable
private fun AddSection(
    team: Team,
    state: TeamRegistrationUiState,
    viewModel: TeamRegistrationViewModel,
    lookup: String,
    onLookupChange: (String) -> Unit,
    addEnabled: Boolean,
    onAdd: () -> Unit,
    rosterLimit: Int,
    accent: Color,
) {
    // Team players not in this event (bench), including those taken by another team.
    val bench = team.players.filter { it.id.toString().uppercase() !in state.rosterIds || viewModel.takenBy(it) != null }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("ADD PLAYERS")
        if (bench.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardBackground)) {
                bench.forEachIndexed { idx, player ->
                    val otherTeam = viewModel.takenBy(player)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(player.name, isGuest = false, accent = accent, modifier = Modifier.alpha(if (otherTeam == null) 1f else 0.4f))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(player.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                otherTeam?.let { "Already playing for $it" } ?: "On ${team.name}",
                                fontSize = 13.sp, color = Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (otherTeam == null) {
                            CircleIconButton(
                                Icons.Default.Add, "Add ${player.name} to this event", accent, accent.copy(alpha = 0.1f),
                                enabled = state.rosterCount < rosterLimit,
                                onClick = { viewModel.addToRoster(player) },
                            )
                        }
                    }
                    if (idx < bench.size - 1) RowDivider(70.dp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetField(lookup, onLookupChange, "Email, phone, or a guest's name", Modifier.weight(1f), capitalize = false, onSubmit = onAdd)
            FieldButton("Add", addEnabled, accent, onAdd)
        }
        Text(
            state.lookupMessage
                ?: "Players with a TournMate account are found by email or phone. Anyone else joins as a guest, without a rating.",
            fontSize = 12.sp,
            color = if (state.lookupMessage == null) Secondary else Label,
        )
    }
}

// MARK: - Register bar

@Composable
private fun RegisterBar(state: TeamRegistrationUiState, shortBy: Int, accent: Color, onRegister: () -> Unit) {
    val team = state.selectedTeam
    val ready = team != null && state.rosterCount >= 2 && !state.isSaving
    Column(
        Modifier.fillMaxWidth().background(GroupedBackground.copy(alpha = 0.96f))
            .navigationBarsPadding().imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (team != null && state.rosterCount >= 2 && shortBy > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(ShortOrange))
                Text("$shortBy short of a full side. You can still register.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ShortOrange)
            }
        } else if (team != null && state.rosterCount < 2) {
            Text("Add at least 2 players to register.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Secondary)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 58.dp)
                .then(if (ready) Modifier.shadow(12.dp, CircleShape, ambientColor = accent, spotColor = accent) else Modifier)
                .clip(CircleShape)
                .background(if (ready || state.isSaving) accent else Gray3)
                .clickable(enabled = ready, onClick = onRegister)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (team == null) "CHOOSE A TEAM" else "REGISTER ${team.name.uppercase()}",
                fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = Color.White,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// MARK: - Small views

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, color = Secondary, modifier = modifier)
}

@Composable
private fun RowDivider(inset: Dp) {
    HorizontalDivider(Modifier.padding(start = inset), color = ScoreDivider)
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    fill: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(fill)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(18.dp), tint = tint)
    }
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    capitalize: Boolean,
    onSubmit: () -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, color = Label),
        cursorBrush = SolidColor(Label),
        keyboardOptions = KeyboardOptions(
            capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            autoCorrectEnabled = capitalize,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier.height(50.dp),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(CardBackground).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = Gray2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                inner()
            }
        },
    )
}

/** Filled capsule next to an input, sized for an easy tap. */
@Composable
private fun FieldButton(title: String, enabled: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.height(50.dp).clip(CircleShape).background(if (enabled) accent else Gray3)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/** Capsule/circle outline (fully rounded), dashed unless [dashed] is false. */
private fun Modifier.dashedOutline(color: Color, width: Dp, dashed: Boolean = true, dash: Dp = 4.dp, gap: Dp = 3.dp): Modifier =
    drawBehind {
        val w = width.toPx()
        val r = (size.minDimension - w) / 2f
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
            size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = w, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())) else null),
        )
    }

private fun initials(name: String): String {
    val letters = name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().toString() }
    return (letters.ifEmpty { name.take(1) }).uppercase()
}
