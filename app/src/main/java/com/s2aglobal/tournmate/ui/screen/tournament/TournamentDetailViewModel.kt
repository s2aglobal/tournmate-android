package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.GroupMatchEntry
import com.s2aglobal.tournmate.data.repository.MatchRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TournamentRepository
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.service.pairing.PairingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
) {
    val isCreator: Boolean
        get() {
            val uid = firebaseUid?.trim() ?: return false
            val createdBy = tournament?.createdBy?.trim() ?: return false
            return uid == createdBy
        }

    val isRegistered: Boolean
        get() = currentPlayerRegistration != null

    val canRegister: Boolean
        get() = tournament != null &&
                !tournament.isRegistrationClosed &&
                currentPlayer != null &&
                !isRegistered &&
                tournament.status.rawValue != "cancelled"

    val teamCount: Int
        get() = registrations.size

    val formedTeams: List<Registration>
        get() {
            val format = tournament?.format ?: return registrations
            return if (format.isSingles) registrations
            else registrations.filter { it.isTeamFormed }
        }

    val matchesByRound: List<RoundGroup>
        get() = matches
            .filter { it.groupLabel == null }
            .groupBy { it.round ?: 0 }
            .map { (round, matches) -> RoundGroup(round, matches) }
            .sortedBy { it.round }

    val currentRoundFullyFinished: Boolean
        get() {
            val lastGroup = matchesByRound.lastOrNull() ?: return false
            return lastGroup.matches.all { it.status == MatchStatus.FINISHED }
        }

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
            val maxRounds = tournament?.let {
                try { kotlinx.serialization.json.Json.decodeFromString<FormatConfig>(it.formatConfigData ?: "{}") } catch (_: Exception) { FormatConfig() }
            }?.swissRounds ?: 5
            return currentSwissRound >= maxRounds && isCurrentSwissRoundComplete
        }

    val isTournamentComplete: Boolean
        get() {
            if (matches.isEmpty()) return false
            val format = tournament?.matchFormat ?: return false
            return when (format) {
                MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION -> {
                    val maxRound = matches.mapNotNull { it.round }.maxOrNull() ?: 0
                    maxRound >= totalBracketRounds && matches.filter { it.round == maxRound }.all { it.status == MatchStatus.FINISHED }
                }
                MatchFormat.ROUND_ROBIN, MatchFormat.MANUAL_DRAW -> matches.all { it.status == MatchStatus.FINISHED }
                MatchFormat.GROUP_KNOCKOUT -> {
                    val knockoutMatches = matches.filter { it.groupLabel == null }
                    if (knockoutMatches.isEmpty()) false
                    else {
                        val maxRound = knockoutMatches.mapNotNull { it.round }.maxOrNull() ?: 0
                        maxRound >= totalBracketRounds && knockoutMatches.filter { it.round == maxRound }.all { it.status == MatchStatus.FINISHED }
                    }
                }
                MatchFormat.SWISS -> isSwissComplete
            }
        }
}

@HiltViewModel
class TournamentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tournamentRepo: TournamentRepository,
    private val registrationRepo: RegistrationRepository,
    private val matchRepo: MatchRepository,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val tournamentId: String = savedStateHandle["tournamentId"] ?: ""

    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val tid = UUID.fromString(tournamentId)
                val tournament = tournamentRepo.findTournament(tid)
                    ?: run {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Tournament not found")
                        return@launch
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
                    var teamCount = formedTeams.size
                    if (teamCount < 2) {
                        teamCount = (matches.filter { it.round == 1 }.size) * 2
                    }
                    if (teamCount >= 2) {
                        var v = 1
                        while (v < teamCount) v *= 2
                        totalRounds = log2(v.toDouble()).toInt()
                    }
                }

                _uiState.value = TournamentDetailUiState(
                    tournament = tournament,
                    registrations = registrations,
                    matches = matches,
                    currentPlayer = currentPlayer,
                    currentPlayerRegistration = myReg,
                    isLoading = false,
                    firebaseUid = firebaseUid,
                    organizerName = organizerName,
                    totalBracketRounds = totalRounds,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            val state = _uiState.value
            val player = state.currentPlayer ?: return@launch
            val tournament = state.tournament ?: return@launch
            _uiState.value = state.copy(isRegistering = true)
            try {
                registrationRepo.createRegistration(player, null, tournament)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRegistering = false, errorMessage = e.message)
            }
        }
    }

    fun unregister() {
        viewModelScope.launch {
            val reg = _uiState.value.currentPlayerRegistration ?: return@launch
            try { registrationRepo.deleteRegistration(reg); load() } catch (e: Exception) { setStatus(e.message) }
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
                load()
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
                load()
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
            val config = try { kotlinx.serialization.json.Json.decodeFromString<FormatConfig>(tournament.formatConfigData ?: "{}") } catch (_: Exception) { FormatConfig() }
            val schedule = PairingService.generateGroupStageSchedule(teams, config.groupCount, config.advancingPerGroup)
            val entries = schedule.map { GroupMatchEntry(it.teamA, it.teamB, it.round, it.group) }
            try {
                matchRepo.createGroupMatches(tournament, entries)
                load()
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
                setStatus("Not all matches in the current round are finished."); return@launch
            }

            var winners = lastGroup.matches.mapNotNull { it.winnerRegistration }

            if (currentRound == 1) {
                val byeResult = PairingService.generateBracketFirstRound(state.formedTeams)
                winners = byeResult.byeTeams + winners
            }

            if (winners.size < 2) { setStatus("Tournament complete!"); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val nextRound = currentRound + 1
            try {
                val half = winners.size / 2
                for (i in 0 until half) {
                    matchRepo.createMatch(tournament, winners[i], winners[winners.size - 1 - i], round = nextRound, bracketPosition = i)
                }
                load()
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

            val config = try { kotlinx.serialization.json.Json.decodeFromString<FormatConfig>(tournament.formatConfigData ?: "{}") } catch (_: Exception) { FormatConfig() }
            val standings = computeGroupStandings(state)
            val regMap = state.registrations.associateBy { it.id.toString().uppercase() }
            val advancingTeams = mutableListOf<Registration>()

            for (group in standings.keys.sorted()) {
                val entries = standings[group] ?: continue
                for (entry in entries.take(config.advancingPerGroup)) {
                    regMap[entry.registrationId]?.let { advancingTeams.add(it) }
                }
            }

            if (advancingTeams.size < 2) { setStatus("Not enough teams for knockout."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val result = PairingService.generateBracketFirstRound(advancingTeams)
            try {
                for ((teamA, teamB, pos) in result.matches) {
                    matchRepo.createMatch(tournament, teamA, teamB, round = 1, bracketPosition = pos)
                }
                load()
                setStatus("Knockout bracket generated with ${advancingTeams.size} teams!")
            } catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun generateNextSwissRound() {
        viewModelScope.launch {
            val state = _uiState.value
            val tournament = state.tournament ?: return@launch
            val teams = state.formedTeams
            if (teams.size < 2) { setStatus("Need at least 2 teams."); return@launch }

            val config = try { kotlinx.serialization.json.Json.decodeFromString<FormatConfig>(tournament.formatConfigData ?: "{}") } catch (_: Exception) { FormatConfig() }
            val nextRound = state.currentSwissRound + 1
            if (nextRound > 1 && !state.isCurrentSwissRoundComplete) { setStatus("Current round not complete."); return@launch }
            if (nextRound > config.swissRounds) { setStatus("All Swiss rounds completed."); return@launch }

            _uiState.value = state.copy(isLoading = true)
            val finished = state.matches.filter { it.status == MatchStatus.FINISHED }
            val pairings = PairingService.generateSwissPairings(teams, finished, nextRound)
            if (pairings.isEmpty()) { setStatus("Cannot generate pairings."); _uiState.value = _uiState.value.copy(isLoading = false); return@launch }

            val schedule = pairings.map { Triple(it.first, it.second, nextRound) }
            try {
                matchRepo.createMatches(tournament, schedule)
                load()
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
                load()
                setStatus("Match created!")
            } catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun resetMatches() {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                matchRepo.deleteMatches(tournament)
                load()
                setStatus("All matches reset.")
            } catch (e: Exception) { setStatus("Failed: ${e.message}") }
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
                    if (pairs.isEmpty()) { setStatus("Need male and female players."); _uiState.value = _uiState.value.copy(isLoading = false); return@launch }
                    for ((male, female) in pairs) {
                        registrationRepo.setPartner(female.player, male)
                        registrationRepo.deleteRegistration(female)
                    }
                } else {
                    val pairs = PairingService.generateRandomDoublesPairs(state.registrations)
                    if (pairs.isEmpty()) { setStatus("Not enough solo players."); _uiState.value = _uiState.value.copy(isLoading = false); return@launch }
                    for ((a, b) in pairs) {
                        registrationRepo.setPartner(b.player, a)
                        registrationRepo.deleteRegistration(b)
                    }
                }
                load()
                setStatus("Teams paired!")
            } catch (e: Exception) { setStatus("Pairing failed: ${e.message}") }
        }
    }

    // ── Score Flow ────────────────────────────────────

    fun recordScore(match: Match, scoreA: Int, scoreB: Int) {
        viewModelScope.launch {
            try { matchRepo.finalizeMatch(match, scoreA, scoreB); load(); setStatus("Score recorded!") }
            catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun submitSetScores(match: Match, setScores: List<SetScore>, submittedBy: String) {
        viewModelScope.launch {
            try { matchRepo.submitSetScores(match, setScores, submittedBy); load(); setStatus("Score submitted!") }
            catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun confirmScore(match: Match, confirmedBy: String) {
        viewModelScope.launch {
            try { matchRepo.confirmScore(match, confirmedBy); load(); setStatus("Score confirmed!") }
            catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun disputeScore(match: Match, disputedBy: String) {
        viewModelScope.launch {
            try { matchRepo.disputeScore(match, disputedBy); load(); setStatus("Score disputed.") }
            catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun resolveDispute(match: Match, setScores: List<SetScore>) {
        viewModelScope.launch {
            try { matchRepo.resolveDispute(match, setScores); load(); setStatus("Dispute resolved!") }
            catch (e: Exception) { setStatus("Failed: ${e.message}") }
        }
    }

    fun cancelTournament() {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            try { tournamentRepo.cancelTournament(tournament); load() } catch (e: Exception) { setStatus(e.message) }
        }
    }

    fun deleteTournament() {
        viewModelScope.launch {
            val tournament = _uiState.value.tournament ?: return@launch
            try { tournamentRepo.deleteTournament(tournament) } catch (e: Exception) { setStatus(e.message) }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(errorMessage = null) }
    fun clearStatus() { _uiState.value = _uiState.value.copy(statusMessage = null) }

    private fun setStatus(msg: String?) { _uiState.value = _uiState.value.copy(statusMessage = msg, isLoading = false) }

    fun computeGroupStandings(state: TournamentDetailUiState): Map<String, List<StandingsEntry>> {
        val config = try {
            kotlinx.serialization.json.Json.decodeFromString<FormatConfig>(state.tournament?.formatConfigData ?: "{}")
        } catch (_: Exception) { FormatConfig() }

        val groupMatches = state.matches.filter { it.groupLabel != null && it.status == MatchStatus.FINISHED }
        val entriesByGroup = mutableMapOf<String, MutableMap<String, StandingsEntry>>()

        for (reg in state.registrations) {
            if (!reg.isTeamFormed && state.tournament?.format?.isDoubles == true) continue
            val teamMatches = state.matches.filter { (it.teamAId == reg.id.toString().uppercase() || it.teamBId == reg.id.toString().uppercase()) && it.groupLabel != null }
            val groupLabel = teamMatches.firstOrNull()?.groupLabel ?: continue
            val name = listOfNotNull(reg.player.name, reg.partner?.name).joinToString(" & ")
            entriesByGroup.getOrPut(groupLabel) { mutableMapOf() }[reg.id.toString().uppercase()] = StandingsEntry(reg.id.toString().uppercase(), name)
        }

        for (match in groupMatches) {
            val group = match.groupLabel ?: continue
            val aId = match.teamAId; val bId = match.teamBId
            entriesByGroup[group]?.get(aId)?.apply { played++; pointsFor += (match.scoreA ?: 0); pointsAgainst += (match.scoreB ?: 0) }
            entriesByGroup[group]?.get(bId)?.apply { played++; pointsFor += (match.scoreB ?: 0); pointsAgainst += (match.scoreA ?: 0) }

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

        return entriesByGroup.mapValues { (_, entries) ->
            entries.values.sortedWith(compareByDescending<StandingsEntry> { it.points }.thenByDescending { it.pointDiff })
        }
    }

    fun computeRRStandings(state: TournamentDetailUiState): List<StandingsEntry> {
        val config = try {
            kotlinx.serialization.json.Json.decodeFromString<FormatConfig>(state.tournament?.formatConfigData ?: "{}")
        } catch (_: Exception) { FormatConfig() }

        val entries = mutableMapOf<String, StandingsEntry>()
        for (reg in state.formedTeams) {
            val name = listOfNotNull(reg.player.name, reg.partner?.name).joinToString(" & ")
            entries[reg.id.toString().uppercase()] = StandingsEntry(reg.id.toString().uppercase(), name)
        }

        for (match in state.matches.filter { it.status == MatchStatus.FINISHED }) {
            val aId = match.teamAId; val bId = match.teamBId
            entries[aId]?.apply { played++; pointsFor += (match.scoreA ?: 0); pointsAgainst += (match.scoreB ?: 0) }
            entries[bId]?.apply { played++; pointsFor += (match.scoreB ?: 0); pointsAgainst += (match.scoreA ?: 0) }

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
                load()
                setStatus("Tournament updated!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Update failed: ${e.message}")
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
