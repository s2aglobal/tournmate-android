package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.CalorieRecordRepository
import com.s2aglobal.tournmate.data.repository.GroupMatchEntry
import com.s2aglobal.tournmate.data.repository.MatchRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TournamentRepository
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.service.calorie.BadmintonIntensity
import com.s2aglobal.tournmate.service.calorie.METEstimator
import com.s2aglobal.tournmate.service.pairing.PairingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.math.log2

data class StandingsEntry(
    val registrationId: String,
    val teamName: String,
    var played: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var draws: Int = 0,
    var points: Int = 0,
    var pointsFor: Int = 0,
    var pointsAgainst: Int = 0,
) {
    val pointDiff: Int get() = pointsFor - pointsAgainst
}

data class BracketStandingsEntry(
    val registrationId: String,
    val teamName: String,
    var matchesPlayed: Int = 0,
    var wins: Int = 0,
    var maxRound: Int = 0,
    var eliminated: Boolean = false,
    var eliminatedInRound: Int? = null,
)

data class RoundGroup(val round: Int, val matches: List<Match>)

data class TournamentDetailUiState(
    val tournament: Tournament? = null,
    val registrations: List<Registration> = emptyList(),
    val matches: List<Match> = emptyList(),
    val currentPlayer: Player? = null,
    val currentPlayerRegistration: Registration? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val firebaseUid: String? = null,
    val isRegistering: Boolean = false,
    val organizerName: String? = null,
    val totalBracketRounds: Int = 0,
    val didDelete: Boolean = false,
    val existingCalorieRecord: CalorieRecord? = null,
    val isLoggingCalories: Boolean = false,
    val calorieWeight: Double = 70.0,
) {
    val isCreator: Boolean
        get() {
            val uid = firebaseUid?.trim() ?: return false
            val createdBy = tournament?.createdBy?.trim() ?: return false
            return uid == createdBy
        }

    val isRegistered: Boolean
        get() = currentPlayerRegistration != null

    val isDoubles: Boolean
        get() = tournament?.format?.isDoubles == true

    val formedTeams: List<Registration>
        get() = if (tournament?.format?.isSingles == true) registrations else registrations.filter { it.isTeamFormed }

    val soloRegistrations: List<Registration>
        get() = if (tournament?.format?.isSingles == true) emptyList() else registrations.filter { !it.isTeamFormed }

    val matchesGenerated: Boolean
        get() = matches.isNotEmpty()

    val pairingsGenerated: Boolean
        get() {
            val t = tournament ?: return false
            return t.randomPairing && t.format.isDoubles && soloRegistrations.isEmpty() && registrations.isNotEmpty()
        }

    val needsPartnerPick: Boolean
        get() = tournament?.format?.isDoubles == true && tournament.randomPairing.not()

    val availablePartners: List<Player>
        get() {
            val t = tournament ?: return emptyList()
            if (!t.format.isDoubles || t.randomPairing) return emptyList()
            val myId = currentPlayer?.id
            return registrations.filter { !it.isTeamFormed && it.player.id != myId }.map { it.player }
        }

    val canWithdraw: Boolean
        get() = isRegistered && !matchesGenerated && tournament?.isPast == false

    val canPickPartnerFromTeams: Boolean
        get() = isRegistered && isDoubles && !matchesGenerated && tournament?.isPast == false

    val hasExistingPartner: Boolean
        get() = currentPlayerRegistration?.partner != null

    val currentTeammate: Player?
        get() {
            val reg = currentPlayerRegistration ?: return null
            val me = currentPlayer ?: return null
            return if (reg.player.id == me.id) reg.partner else reg.player
        }

    val canLogTournamentCalories: Boolean
        get() = isRegistered && tournament?.isPast == true

    val byeTeamNames: List<String>
        get() {
            if (tournament?.matchFormat != MatchFormat.SINGLE_ELIMINATION || formedTeams.isEmpty()) return emptyList()
            return PairingService.generateBracketFirstRound(formedTeams).byeTeams.map { registrationFullName(it) }
        }

    /** Knockout / non-group matches grouped by round (used by the admin round logic). */
    val matchesByRound: List<RoundGroup>
        get() = matches
            .filter { it.groupLabel == null }
            .groupBy { it.round ?: 0 }
            .map { (round, ms) -> RoundGroup(round, ms.sortedBy { it.bracketPosition ?: 0 }) }
            .sortedBy { it.round }

    /** Every match grouped by round, like iOS `matchesByRound` (draw views filter as needed). */
    val allMatchesByRound: List<RoundGroup>
        get() = matches
            .groupBy { it.round ?: 0 }
            .map { (round, ms) -> RoundGroup(round, ms.sortedBy { it.bracketPosition ?: 0 }) }
            .sortedBy { it.round }

    val currentRoundFullyFinished: Boolean
        get() {
            val lastGroup = matchesByRound.lastOrNull() ?: return false
            return lastGroup.matches.all { it.status == MatchStatus.FINISHED }
        }

    val currentMaxRound: Int
        get() = matches.filter { it.groupLabel == null }.mapNotNull { it.round }.maxOrNull() ?: 0

    val isBracketFullyComplete: Boolean
        get() = totalBracketRounds > 0 && currentMaxRound >= totalBracketRounds && currentRoundFullyFinished

    val isGroupStageComplete: Boolean
        get() {
            val groupMatches = matches.filter { it.groupLabel != null }
            return groupMatches.isNotEmpty() && groupMatches.all { it.status == MatchStatus.FINISHED }
        }

    val hasKnockoutMatches: Boolean
        get() = matches.any { it.groupLabel == null }

    val currentSwissRound: Int
        get() = matches.mapNotNull { it.round }.maxOrNull() ?: 0

    val isCurrentSwissRoundComplete: Boolean
        get() {
            if (currentSwissRound == 0) return true
            val roundMatches = matches.filter { it.round == currentSwissRound }
            return roundMatches.isNotEmpty() && roundMatches.all { it.status == MatchStatus.FINISHED }
        }

    val isSwissComplete: Boolean
        get() {
            val maxRounds = tournament?.formatConfig?.swissRounds ?: 5
            return currentSwissRound >= maxRounds && isCurrentSwissRoundComplete
        }

    /** True when every match is done (iOS `isTournamentActuallyComplete`). */
    val isTournamentComplete: Boolean
        get() {
            val t = tournament ?: return false
            if (matches.isEmpty()) return t.isPast
            return when (t.matchFormat) {
                MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> isBracketFullyComplete
                MatchFormat.GROUP_KNOCKOUT -> hasKnockoutMatches && isBracketFullyComplete
                MatchFormat.SWISS -> isSwissComplete
                MatchFormat.ROUND_ROBIN, MatchFormat.MANUAL_DRAW -> matches.all { it.status == MatchStatus.FINISHED }
            }
        }

    fun isPlayerInMatch(match: Match): Boolean {
        val uid = firebaseUid ?: return false
        return match.isParticipant(uid)
    }
}

internal fun registrationFullName(reg: Registration): String =
    listOfNotNull(reg.player.name, reg.partner?.name).joinToString(" & ")

/** First names only, e.g. "John & Mike" (iOS detail `teamDisplayName`). */
internal fun registrationShortName(reg: Registration): String {
    val partner = reg.partner ?: return reg.player.name
    val first = reg.player.name.split(" ").firstOrNull()?.ifEmpty { null } ?: reg.player.name
    val partnerFirst = partner.name.split(" ").firstOrNull()?.ifEmpty { null } ?: partner.name
    return "$first & $partnerFirst"
}

@HiltViewModel
class TournamentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tournamentRepo: TournamentRepository,
    private val registrationRepo: RegistrationRepository,
    private val matchRepo: MatchRepository,
    private val playerRepo: PlayerRepository,
    private val calorieRepo: CalorieRecordRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val tournamentId: String = savedStateHandle["tournamentId"] ?: ""

    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch { reload() }
    }

    private suspend fun reload() {
        _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = null)
        try {
            val tid = UUID.fromString(tournamentId)
            val tournament = tournamentRepo.findTournament(tid)
            if (tournament == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Tournament not found")
                return
            }

            val registrations = registrationRepo.listRegistrations(tournament)
            val matches = matchRepo.listMatches(tournament)
            val playerId = currentUserStore.currentPlayerId()
            val firebaseUid = currentUserStore.firebaseUid()
                ?: FirebaseAuth.getInstance().currentUser?.uid
            var currentPlayer: Player? = null
            var myReg: Registration? = null

            if (playerId != null) {
                currentPlayer = playerRepo.findPlayerById(playerId)
                myReg = registrations.firstOrNull { it.containsPlayerID(playerId) }
            }

            var organizerName: String? = _uiState.value.organizerName
            if (organizerName == null) {
                tournament.createdBy?.let { uid ->
                    organizerName = try { playerRepo.findPlayerByFirebaseUid(uid)?.name } catch (_: Exception) { null }
                }
            }

            var totalRounds = _uiState.value.totalBracketRounds
            if (matches.isNotEmpty()) {
                val formedTeams = if (tournament.format.isSingles) registrations else registrations.filter { it.isTeamFormed }
                var teamCount = when (tournament.matchFormat) {
                    MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> formedTeams.size
                    MatchFormat.GROUP_KNOCKOUT -> matches.filter { it.groupLabel == null }
                        .flatMap { listOf(it.teamAId, it.teamBId) }.toSet().size
                    else -> 0
                }
                if (teamCount < 2) {
                    teamCount = matches.count { it.round == 1 && it.groupLabel == null } * 2
                }
                if (teamCount >= 2) {
                    var v = 1
                    while (v < teamCount) v *= 2
                    totalRounds = log2(v.toDouble()).toInt()
                }
            }

            var calorieRecord = _uiState.value.existingCalorieRecord
            if (calorieRecord == null && myReg != null && tournament.isPast && playerId != null) {
                calorieRecord = try { calorieRepo.findRecord(tournament.id, playerId) } catch (_: Exception) { null }
            }

            _uiState.value = _uiState.value.copy(
                tournament = tournament,
                registrations = registrations,
                matches = matches,
                currentPlayer = currentPlayer,
                currentPlayerRegistration = myReg,
                isLoading = false,
                isRegistering = false,
                firebaseUid = firebaseUid,
                organizerName = organizerName,
                totalBracketRounds = totalRounds,
                existingCalorieRecord = calorieRecord,
                calorieWeight = if (_uiState.value.tournament == null) currentPlayer?.weightKg ?: 70.0 else _uiState.value.calorieWeight,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isLoading = false, isRegistering = false, errorMessage = e.message)
        }
    }

    // ── Registration ─────────────────────────────────

    fun register(partner: Player? = null) {
        viewModelScope.launch { registerCurrentPlayer(partner) }
    }

    private suspend fun registerCurrentPlayer(partner: Player?) {
        val state = _uiState.value
        val tournament = state.tournament ?: return
        val me = state.currentPlayer ?: run { setStatus("Could not find your player profile."); return }
        if (state.isRegistered) { setStatus("You are already registered."); return }
        if (tournament.isRegistrationClosed) { setStatus("Registration is closed."); return }

        if (tournament.ageGroup != AgeGroup.OPEN) {
            val age = me.age ?: run {
                setStatus("Please set your date of birth in your profile to register for age-restricted tournaments.")
                return
            }
            if (!tournament.ageGroup.isEligible(age)) {
                setStatus("This tournament is for ${tournament.ageGroup.displayName}. Your age ($age) does not qualify.")
                return
            }
        }

        _uiState.value = state.copy(isRegistering = true, isLoading = true)
        try {
            val openReg = if (!tournament.format.isSingles && !tournament.randomPairing && partner != null) {
                registrationRepo.findOpenRegistration(partner, tournament)
            } else null
            when {
                tournament.format.isSingles || tournament.randomPairing ->
                    registrationRepo.createRegistration(me, null, tournament)
                openReg != null -> registrationRepo.setPartner(me, openReg)
                else -> registrationRepo.createRegistration(me, partner, tournament)
            }
            reload()
            setStatus("Registered successfully! ✅")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isRegistering = false)
            setStatus("Registration failed: ${e.message}")
        }
    }

    /** Saves the DOB from the age-restricted prompt, then continues registration. */
    fun saveDateOfBirthAndRegister(dob: Date, partner: Player?, continueToPartnerPick: Boolean) {
        viewModelScope.launch {
            val player = _uiState.value.currentPlayer ?: return@launch
            try {
                playerRepo.updatePlayer(player.copy(dateOfBirth = dob))
                reload()
            } catch (e: Exception) {
                setStatus("Failed to save date of birth: ${e.message}")
                return@launch
            }
            if (!continueToPartnerPick) registerCurrentPlayer(partner)
        }
    }

    fun withdraw() {
        viewModelScope.launch {
            val state = _uiState.value
            val me = state.currentPlayer ?: run { setStatus("Could not find your player profile."); return@launch }
            val reg = state.currentPlayerRegistration ?: run { setStatus("You are not registered for this tournament."); return@launch }
            if (state.matchesGenerated) { setStatus("Cannot withdraw after matches have been generated."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            try {
                if (reg.player.id == me.id) registrationRepo.deleteRegistration(reg)
                else if (reg.partner?.id == me.id) registrationRepo.clearPartner(reg)
                reload()
                setStatus("You have withdrawn from this tournament.")
            } catch (e: Exception) { setStatus("Withdrawal failed: ${e.message}") }
        }
    }

    fun pickPartnerFromTeams(partner: Player) {
        viewModelScope.launch {
            val state = _uiState.value
            val me = state.currentPlayer ?: run { setStatus("Could not find your player profile."); return@launch }
            val myReg = state.currentPlayerRegistration ?: run { setStatus("You must be registered first."); return@launch }
            if (partner.id == me.id) return@launch

            _uiState.value = state.copy(isLoading = true)
            try {
                if (myReg.player.id == me.id) {
                    if (myReg.partner != null) registrationRepo.clearPartner(myReg)
                    registrationRepo.setPartner(partner, myReg)
                } else {
                    registrationRepo.clearPartner(myReg)
                    val targetReg = state.registrations.firstOrNull { it.player.id == partner.id }
                        ?: run { setStatus("Could not find that player's registration."); return@launch }
                    registrationRepo.setPartner(me, targetReg)
                }
                reload()
                setStatus("Partnered with ${partner.name}!")
            } catch (e: Exception) { setStatus("Failed to set partner: ${e.message}") }
        }
    }

    // ── Calories ─────────────────────────────────────

    fun setCalorieWeight(weight: Double) {
        _uiState.value = _uiState.value.copy(calorieWeight = weight)
    }

    fun logTournamentCalories() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            val player = state.currentPlayer ?: return@launch
            _uiState.value = state.copy(isLoggingCalories = true)
            val weight = state.calorieWeight
            if (player.weightKg != weight) {
                try { playerRepo.updatePlayer(player.copy(weightKg = weight)) } catch (_: Exception) { }
            }
            val duration = tournament.durationMinutes ?: 180
            val record = CalorieRecord(
                sessionId = tournament.id,
                playerId = player.id,
                calories = METEstimator.estimate(duration, weight, BadmintonIntensity.COMPETITIVE),
                source = CalorieSource.ESTIMATED,
                weightUsedKg = weight,
                durationMinutes = duration,
                date = tournament.date,
                sessionTitle = tournament.title,
                activityType = CalorieActivityType.TOURNAMENT,
            )
            try {
                calorieRepo.save(record)
                _uiState.value = _uiState.value.copy(existingCalorieRecord = record, isLoggingCalories = false)
                setStatus("Calories logged: ${record.formattedCalories}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoggingCalories = false)
                setStatus("Failed to log calories: ${e.message}")
            }
        }
    }

    // ── Match Generation ─────────────────────────────

    fun generateBracket() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            val teams = state.formedTeams
            if (teams.size < 2) { setStatus("Need at least 2 teams."); return@launch }
            if (state.matches.isNotEmpty()) { setStatus("Bracket already generated."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val result = PairingService.generateBracketFirstRound(teams)

            try {
                for ((teamA, teamB, pos) in result.matches) {
                    matchRepo.createMatch(tournament, teamA, teamB, round = 1, bracketPosition = pos)
                }
                reload()
                val msg = "Bracket generated! ${result.matches.size} first-round matches." +
                    if (result.byeTeams.isNotEmpty()) " ${result.byeTeams.size} team(s) received a bye." else ""
                setStatus(msg)
            } catch (e: Exception) { setStatus("Bracket generation failed: ${e.message}") }
        }
    }

    fun generateRoundRobinMatches() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            val teams = state.formedTeams
            if (teams.size < 2) { setStatus("Need at least 2 teams."); return@launch }
            if (state.matches.isNotEmpty()) { setStatus("Matches already generated."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val schedule = PairingService.generateRoundRobinSchedule(teams)
            try {
                matchRepo.createMatches(tournament, schedule)
                reload()
                setStatus("Generated ${schedule.size} round-robin matches!")
            } catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun generateGroupKnockoutMatches() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            val teams = state.formedTeams
            if (teams.size < 4) { setStatus("Need at least 4 teams."); return@launch }
            if (state.matches.isNotEmpty()) { setStatus("Group matches already generated."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val config = tournament.formatConfig
            val schedule = PairingService.generateGroupStageSchedule(teams, config.groupCount, config.advancingPerGroup)
            val entries = schedule.map { GroupMatchEntry(it.teamA, it.teamB, it.round, it.group) }
            try {
                matchRepo.createGroupMatches(tournament, entries)
                reload()
                setStatus("Generated ${schedule.size} group-stage matches!")
            } catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun advanceToNextRound() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            val roundGroups = state.matchesByRound
            val lastGroup = roundGroups.lastOrNull() ?: return@launch
            val currentRound = lastGroup.round

            if (!lastGroup.matches.all { it.status == MatchStatus.FINISHED }) {
                setStatus("Not all matches in the current round are finished yet."); return@launch
            }

            var winners = lastGroup.matches.mapNotNull { it.winnerRegistration }

            if (currentRound == 1) {
                val seededTeams = if (tournament.matchFormat == MatchFormat.GROUP_KNOCKOUT) advancingTeams(state) else state.formedTeams
                winners = PairingService.generateBracketFirstRound(seededTeams).byeTeams + winners
            }

            if (winners.size < 2) { setStatus("Tournament complete! 🏆"); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val nextRound = currentRound + 1
            try {
                val half = winners.size / 2
                for (i in 0 until half) {
                    matchRepo.createMatch(tournament, winners[i], winners[winners.size - 1 - i], round = nextRound, bracketPosition = i)
                }
                reload()
                val roundName = Match.bracketRoundName(nextRound, state.totalBracketRounds)
                setStatus("$roundName matches generated!")
            } catch (e: Exception) { setStatus("Failed to advance: ${e.message}") }
        }
    }

    fun advanceGroupToKnockout() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            if (!state.isGroupStageComplete) { setStatus("Group stage not complete."); return@launch }
            if (state.hasKnockoutMatches) { setStatus("Knockout already generated."); return@launch }

            val qualified = advancingTeams(state)

            if (qualified.size < 2) { setStatus("Not enough teams for knockout."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val result = PairingService.generateBracketFirstRound(qualified)
            try {
                for ((teamA, teamB, pos) in result.matches) {
                    matchRepo.createMatch(tournament, teamA, teamB, round = 1, bracketPosition = pos)
                }
                reload()
                setStatus("Knockout bracket generated with ${qualified.size} teams!")
            } catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    private fun advancingTeams(state: TournamentDetailUiState): List<Registration> {
        val perGroup = state.tournament?.formatConfig?.advancingPerGroup ?: 2
        val standings = computeGroupStandings(state)
        val regMap = state.registrations.associateBy { it.id.toString().uppercase() }
        return standings.keys.sorted().flatMap { group ->
            standings[group].orEmpty().take(perGroup).mapNotNull { regMap[it.registrationId] }
        }
    }

    fun generateNextSwissRound() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            val teams = state.formedTeams
            if (teams.size < 2) { setStatus("Need at least 2 teams."); return@launch }

            val config = tournament.formatConfig
            val nextRound = state.currentSwissRound + 1
            if (nextRound > 1 && !state.isCurrentSwissRoundComplete) { setStatus("Current round not complete."); return@launch }
            if (nextRound > config.swissRounds) { setStatus("All Swiss rounds completed."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val finished = state.matches.filter { it.status == MatchStatus.FINISHED }
            val pairings = PairingService.generateSwissPairings(teams, finished, nextRound)
            if (pairings.isEmpty()) { setStatus("Cannot generate pairings."); return@launch }

            val schedule = pairings.map { Triple(it.first, it.second, nextRound) }
            try {
                matchRepo.createMatches(tournament, schedule)
                reload()
                setStatus("Swiss Round $nextRound generated!")
            } catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun createManualMatch(teamA: Registration, teamB: Registration, round: Int) {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                matchRepo.createMatch(tournament, teamA, teamB, round = round)
                reload()
                setStatus("Match created!")
            } catch (e: Exception) { setStatus("Failed to create match: ${e.message}") }
        }
    }

    fun resetMatches() {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                matchRepo.deleteMatches(tournament)
                reload()
                setStatus("All matches cleared. You can regenerate the bracket.")
            } catch (e: Exception) { setStatus("Failed to reset matches: ${e.message}") }
        }
    }

    fun generateRandomPairs() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            if (!tournament.format.isDoubles) { setStatus("Only for doubles."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            try {
                if (tournament.format == TournamentFormat.MIXED_DOUBLES) {
                    val pairs = PairingService.generateMixedDoublesPairs(state.registrations)
                    if (pairs.isEmpty()) { setStatus("Need male and female players."); return@launch }
                    for ((male, female) in pairs) {
                        registrationRepo.setPartner(female.player, male)
                        registrationRepo.deleteRegistration(female)
                    }
                } else {
                    val pairs = PairingService.generateRandomDoublesPairs(state.registrations)
                    if (pairs.isEmpty()) { setStatus("Not enough solo players."); return@launch }
                    for ((a, b) in pairs) {
                        registrationRepo.setPartner(b.player, a)
                        registrationRepo.deleteRegistration(b)
                    }
                }
                reload()
                setStatus("Teams paired!")
            } catch (e: Exception) { setStatus("Pairing failed: ${e.message}") }
        }
    }

    // ── Score Flow ────────────────────────────────────

    fun recordScore(match: Match, scoreA: Int, scoreB: Int) {
        viewModelScope.launch {
            try { matchRepo.finalizeMatch(match, scoreA, scoreB); reload(); setStatus("Score recorded!") }
            catch (e: Exception) { setStatus("Failed to record score: ${e.message}") }
        }
    }

    /** Players submit for confirmation; the organizer's submission is confirmed straight away. */
    fun submitSetScores(match: Match, setScores: List<SetScore>, submittedBy: String, autoConfirm: Boolean) {
        viewModelScope.launch {
            try {
                matchRepo.submitSetScores(match, setScores, submittedBy)
                if (autoConfirm) {
                    val aWins = setScores.count { it.teamAWon }
                    val bWins = setScores.count { it.teamBWon }
                    val winner = when {
                        aWins > bWins -> match.teamAId
                        bWins > aWins -> match.teamBId
                        else -> null
                    }
                    matchRepo.confirmScore(match.copy(setScores = setScores, winnerRegistrationId = winner), submittedBy)
                    reload()
                    afterConfirmStatus()
                } else {
                    reload()
                    setStatus("Score submitted! Awaiting confirmation from the other team.")
                }
            } catch (e: Exception) { setStatus("Failed to submit score: ${e.message}") }
        }
    }

    fun confirmScore(match: Match, confirmedBy: String) {
        viewModelScope.launch {
            try { matchRepo.confirmScore(match, confirmedBy); reload(); afterConfirmStatus() }
            catch (e: Exception) { setStatus("Failed to confirm score: ${e.message}") }
        }
    }

    private fun afterConfirmStatus() {
        val state = _uiState.value
        if (state.tournament?.matchFormat == MatchFormat.SINGLE_ELIMINATION && state.currentRoundFullyFinished) {
            setStatus("Round complete! Tap 'Next Round' to advance winners.")
        } else {
            setStatus("Score confirmed! ✅")
        }
    }

    fun disputeScore(match: Match, disputedBy: String) {
        viewModelScope.launch {
            try { matchRepo.disputeScore(match, disputedBy); reload(); setStatus("Score disputed. The organizer will resolve this.") }
            catch (e: Exception) { setStatus("Failed to dispute score: ${e.message}") }
        }
    }

    fun resolveDispute(match: Match, setScores: List<SetScore>) {
        viewModelScope.launch {
            try { matchRepo.resolveDispute(match, setScores); reload(); setStatus("Dispute resolved! Score finalized.") }
            catch (e: Exception) { setStatus("Failed to resolve dispute: ${e.message}") }
        }
    }

    fun cancelTournament() {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            try { tournamentRepo.cancelTournament(tournament); reload(); setStatus("Tournament cancelled.") }
            catch (e: Exception) { setStatus("Failed to cancel: ${e.message}") }
        }
    }

    fun deleteTournament() {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            try {
                tournamentRepo.deleteTournament(tournament)
                _uiState.value = _uiState.value.copy(didDelete = true, statusMessage = "Tournament deleted.")
            } catch (e: Exception) { setStatus("Failed to delete: ${e.message}") }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
    fun clearStatus() { _uiState.value = _uiState.value.copy(statusMessage = null) }

    private fun setStatus(msg: String?) { _uiState.value = _uiState.value.copy(statusMessage = msg, isLoading = false) }

    private fun scorePair(match: Match): Pair<Int, Int> =
        if (match.setScores.isNotEmpty()) match.setScores.sumOf { it.teamAPoints } to match.setScores.sumOf { it.teamBPoints }
        else (match.scoreA ?: 0) to (match.scoreB ?: 0)

    fun computeGroupStandings(state: TournamentDetailUiState): Map<String, List<StandingsEntry>> {
        val config = state.tournament?.formatConfig ?: FormatConfig()

        val groupMatches = state.matches.filter { it.groupLabel != null && it.status == MatchStatus.FINISHED }
        val entriesByGroup = mutableMapOf<String, MutableMap<String, StandingsEntry>>()

        for (reg in state.registrations) {
            if (!reg.isTeamFormed && state.tournament?.format?.isDoubles == true) continue
            val rid = reg.id.toString().uppercase()
            val groupLabel = state.matches.firstOrNull { (it.teamAId == rid || it.teamBId == rid) && it.groupLabel != null }?.groupLabel ?: continue
            entriesByGroup.getOrPut(groupLabel) { mutableMapOf() }[rid] = StandingsEntry(rid, registrationFullName(reg))
        }

        for (match in groupMatches) {
            val group = match.groupLabel ?: continue
            val aId = match.teamAId; val bId = match.teamBId
            val (a, b) = scorePair(match)
            entriesByGroup[group]?.get(aId)?.apply { played++; pointsFor += a; pointsAgainst += b }
            entriesByGroup[group]?.get(bId)?.apply { played++; pointsFor += b; pointsAgainst += a }

            val winnerId = match.winnerRegistrationId
            if (winnerId != null) {
                val loserId = if (winnerId == aId) bId else aId
                entriesByGroup[group]?.get(winnerId)?.apply { wins++; points += config.pointsPerWin }
                entriesByGroup[group]?.get(loserId)?.apply { losses++; points += config.pointsPerLoss }
            } else {
                entriesByGroup[group]?.get(aId)?.apply { draws++; points += config.pointsPerDraw }
                entriesByGroup[group]?.get(bId)?.apply { draws++; points += config.pointsPerDraw }
            }
        }

        if (groupMatches.isEmpty()) return emptyMap()
        return entriesByGroup.mapValues { (_, entries) ->
            entries.values.sortedWith(compareByDescending<StandingsEntry> { it.points }.thenByDescending { it.pointDiff })
        }
    }

    fun computeRRStandings(state: TournamentDetailUiState): List<StandingsEntry> {
        val config = state.tournament?.formatConfig ?: FormatConfig()
        val finished = state.matches.filter { it.status == MatchStatus.FINISHED }
        if (finished.isEmpty()) return emptyList()

        val entries = mutableMapOf<String, StandingsEntry>()
        for (reg in state.formedTeams) {
            val rid = reg.id.toString().uppercase()
            entries[rid] = StandingsEntry(rid, registrationFullName(reg))
        }

        for (match in finished) {
            val aId = match.teamAId; val bId = match.teamBId
            val (a, b) = scorePair(match)
            entries[aId]?.apply { played++; pointsFor += a; pointsAgainst += b }
            entries[bId]?.apply { played++; pointsFor += b; pointsAgainst += a }

            val winnerId = match.winnerRegistrationId
            if (winnerId != null) {
                val loserId = if (winnerId == aId) bId else aId
                entries[winnerId]?.apply { wins++; points += config.pointsPerWin }
                entries[loserId]?.apply { losses++; points += config.pointsPerLoss }
            } else {
                entries[aId]?.apply { draws++; points += config.pointsPerDraw }
                entries[bId]?.apply { draws++; points += config.pointsPerDraw }
            }
        }

        return entries.values.sortedWith(compareByDescending<StandingsEntry> { it.points }.thenByDescending { it.pointDiff })
    }

    fun computeBracketProgress(state: TournamentDetailUiState): List<BracketStandingsEntry> {
        val knockout = state.matches.filter { it.groupLabel == null }
        if (knockout.isEmpty()) return emptyList()
        val entries = mutableMapOf<String, BracketStandingsEntry>()
        for (reg in state.formedTeams) {
            val rid = reg.id.toString().uppercase()
            entries[rid] = BracketStandingsEntry(rid, registrationFullName(reg))
        }
        for (match in knockout) {
            val round = match.round ?: 1
            entries[match.teamAId]?.let { it.maxRound = maxOf(it.maxRound, round) }
            entries[match.teamBId]?.let { it.maxRound = maxOf(it.maxRound, round) }
            if (match.status == MatchStatus.FINISHED) {
                entries[match.teamAId]?.apply { matchesPlayed++ }
                entries[match.teamBId]?.apply { matchesPlayed++ }
                val wId = match.winnerRegistrationId ?: continue
                val lId = if (wId == match.teamAId) match.teamBId else match.teamAId
                entries[wId]?.apply { wins++ }
                entries[lId]?.apply { eliminated = true; eliminatedInRound = round }
            }
        }
        return entries.values.sortedWith(
            compareBy<BracketStandingsEntry> { it.eliminated }
                .thenByDescending { it.maxRound }
                .thenByDescending { it.wins }
        )
    }

    // ── Edit Tournament ──────────────────────────────

    fun updateTournament(
        title: String, date: java.util.Date,
        location: String, locationAddress: String,
        locationLatitude: Double?, locationLongitude: Double?,
        format: TournamentFormat, matchFormat: MatchFormat,
        formatConfig: FormatConfig?,
        randomPairing: Boolean,
        registrationDeadline: java.util.Date,
        entryFee: Double?, currency: String, paymentInfo: String?,
        prizeInfo: String?, durationMinutes: Int?, ageGroup: AgeGroup,
    ) {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val (countryCode, postalCode) = resolveRegionFromCoordinates(locationLatitude, locationLongitude)
                tournamentRepo.updateTournament(
                    tournament = tournament,
                    title = title, date = date,
                    location = location, locationAddress = locationAddress,
                    locationLatitude = locationLatitude, locationLongitude = locationLongitude,
                    countryCode = countryCode, postalCode = postalCode,
                    format = format, matchFormat = matchFormat, formatConfig = formatConfig,
                    randomPairing = randomPairing, registrationDeadline = registrationDeadline,
                    entryFee = entryFee, currency = currency, paymentInfo = paymentInfo,
                    prizeInfo = prizeInfo, durationMinutes = durationMinutes, ageGroup = ageGroup,
                )
                reload()
                setStatus("Tournament updated successfully!")
            } catch (e: Exception) {
                setStatus("Failed to update: ${e.message}")
            }
        }
    }

    private suspend fun resolveRegionFromCoordinates(lat: Double?, lng: Double?): Pair<String?, String?> {
        if (lat == null || lng == null) return null to null
        return try {
            val geocoder = android.location.Geocoder(
                com.s2aglobal.tournmate.TournMateApp.appContext, java.util.Locale.getDefault()
            )
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            val address = addresses?.firstOrNull()
            address?.countryCode to address?.postalCode
        } catch (_: Exception) {
            null to null
        }
    }
}
