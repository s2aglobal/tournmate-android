package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TournamentRepository
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.service.region.RegionNormalizer
import com.s2aglobal.tournmate.service.validation.EventRateLimiter
import com.s2aglobal.tournmate.service.validation.EventType
import com.s2aglobal.tournmate.service.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

enum class TournamentFilter(val displayName: String) {
    ALL("All"),
    MINE("My Tournaments"),
    COMPLETED("Completed"),
}

data class TournamentListUiState(
    val allUpcoming: List<Tournament> = emptyList(),
    val allPast: List<Tournament> = emptyList(),
    val filter: TournamentFilter = TournamentFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Create / cancel / delete failures — shown as the "Unable to Post" alert, never as the load-error state. */
    val createError: String? = null,
    val isCreating: Boolean = false,
    val currentPlayer: Player? = null,
    val myRegisteredTournamentIds: Set<UUID> = emptySet(),
    val firebaseUid: String? = null,
    val preferredSport: SportType = SportType.BADMINTON,
) {
    val upcomingTournaments: List<Tournament>
        get() = when (filter) {
            TournamentFilter.ALL -> allUpcoming.filter { passesSport(it) && passesRegion(it) }
            TournamentFilter.MINE -> allUpcoming.filter { isMine(it) && passesRegionOrMine(it) }
            TournamentFilter.COMPLETED -> emptyList()
        }

    val inProgressTournaments: List<Tournament>
        get() = when (filter) {
            TournamentFilter.ALL -> allUpcoming.filter { it.isRegistrationClosed && !it.isPast && passesSport(it) && passesRegion(it) }
            TournamentFilter.MINE -> allUpcoming.filter { it.isRegistrationClosed && !it.isPast && isMine(it) }
            TournamentFilter.COMPLETED -> emptyList()
        }

    val trulyUpcomingTournaments: List<Tournament>
        get() = upcomingTournaments.filter { !it.isRegistrationClosed }

    val myPostedTournaments: List<Tournament>
        get() {
            if (filter != TournamentFilter.MINE) return emptyList()
            val uid = firebaseUid ?: return emptyList()
            return allUpcoming.filter { it.createdBy == uid }
        }

    val myEnrolledTournaments: List<Tournament>
        get() {
            if (filter != TournamentFilter.MINE) return emptyList()
            return allUpcoming
                .filter { myRegisteredTournamentIds.contains(it.id) }
                .filter { it.createdBy != firebaseUid }
        }

    val pastTournaments: List<Tournament>
        get() = when (filter) {
            TournamentFilter.ALL -> emptyList()
            TournamentFilter.MINE -> emptyList()
            TournamentFilter.COMPLETED -> allPast.filter { passesSport(it) && passesRegion(it) }
        }

    val isEmpty: Boolean
        get() = upcomingTournaments.isEmpty() && pastTournaments.isEmpty() && inProgressTournaments.isEmpty()

    val isMyFilterEmpty: Boolean
        get() = filter == TournamentFilter.MINE && isEmpty && (allUpcoming.isNotEmpty() || allPast.isNotEmpty())

    private fun isMine(tournament: Tournament): Boolean {
        val uid = firebaseUid
        val createdByMe = uid != null && tournament.createdBy == uid
        val registeredIn = myRegisteredTournamentIds.contains(tournament.id)
        return createdByMe || registeredIn
    }

    private fun passesRegionOrMine(tournament: Tournament): Boolean {
        if (isMine(tournament)) return true
        return RegionNormalizer.tournamentMatchesPlayerRegion(
            tournament = tournament,
            playerCountry = currentPlayer?.homeCountryCode,
            playerPostal = currentPlayer?.homePostalCode,
        )
    }

    private fun passesRegion(tournament: Tournament): Boolean {
        if (currentPlayer == null) return true
        return RegionNormalizer.tournamentMatchesPlayerRegion(
            tournament = tournament,
            playerCountry = currentPlayer.homeCountryCode,
            playerPostal = currentPlayer.homePostalCode,
        )
    }

    // Applied to All and Completed; My Tournaments shows every sport.
    private fun passesSport(tournament: Tournament): Boolean =
        tournament.sportType == preferredSport
}

@HiltViewModel
class TournamentListViewModel @Inject constructor(
    private val tournamentRepo: TournamentRepository,
    private val registrationRepo: RegistrationRepository,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentListUiState())
    val uiState: StateFlow<TournamentListUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            currentUserStore.preferredSportFlow.collect { sport ->
                _uiState.value = _uiState.value.copy(preferredSport = sport)
            }
        }
    }

    fun load() {
        viewModelScope.launch { loadInternal() }
    }

    suspend fun refresh() = loadInternal()

    private suspend fun loadInternal() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val upcoming = tournamentRepo.listTournaments()
            val past = tournamentRepo.listPastTournaments()
            val playerId = currentUserStore.currentPlayerId()
            val firebaseUid = currentUserStore.firebaseUid()

            var player: Player? = null
            var registeredIds: Set<UUID> = emptySet()

            if (playerId != null) {
                player = playerRepo.findPlayerById(playerId)
                registeredIds = registrationRepo.tournamentIds(playerId)
            }

            _uiState.value = _uiState.value.copy(
                allUpcoming = upcoming,
                allPast = past,
                isLoading = false,
                errorMessage = null,
                currentPlayer = player,
                myRegisteredTournamentIds = registeredIds,
                firebaseUid = firebaseUid,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message ?: "Failed to load tournaments",
            )
        }
    }

    fun setFilter(filter: TournamentFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearCreateError() {
        _uiState.value = _uiState.value.copy(createError = null)
    }

    fun create(
        title: String,
        date: Date,
        location: String,
        locationAddress: String,
        locationLatitude: Double?,
        locationLongitude: Double?,
        format: TournamentFormat,
        matchFormat: MatchFormat,
        formatConfig: FormatConfig?,
        randomPairing: Boolean,
        registrationDeadline: Date?,
        createdBy: String?,
        entryFee: Double?,
        currency: String,
        paymentInfo: String?,
        prizeInfo: String?,
        durationMinutes: Int?,
        ageGroup: AgeGroup,
        sportType: SportType,
        scoringConfig: ScoringConfig? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        if (_uiState.value.isCreating) return
        _uiState.value = _uiState.value.copy(isCreating = true)
        viewModelScope.launch {
            val ok = createInternal(
                title, date, location, locationAddress, locationLatitude, locationLongitude,
                format, matchFormat, formatConfig, randomPairing, registrationDeadline, createdBy,
                entryFee, currency, paymentInfo, prizeInfo, durationMinutes, ageGroup, sportType, scoringConfig,
            )
            _uiState.value = _uiState.value.copy(isCreating = false)
            onResult(ok)
        }
    }

    private suspend fun createInternal(
        title: String,
        date: Date,
        location: String,
        locationAddress: String,
        locationLatitude: Double?,
        locationLongitude: Double?,
        format: TournamentFormat,
        matchFormat: MatchFormat,
        formatConfig: FormatConfig?,
        randomPairing: Boolean,
        registrationDeadline: Date?,
        createdBy: String?,
        entryFee: Double?,
        currency: String,
        paymentInfo: String?,
        prizeInfo: String?,
        durationMinutes: Int?,
        ageGroup: AgeGroup,
        sportType: SportType,
        scoringConfig: ScoringConfig?,
    ): Boolean {
        val state = _uiState.value

        val profileCheck = EventRateLimiter.hasCompleteProfile(state.currentPlayer)
        if (!profileCheck.isValid) { setCreateError(profileCheck.errorMessage); return false }

        val cooldownCheck = EventRateLimiter.canCreate(EventType.TOURNAMENT)
        if (!cooldownCheck.isValid) { setCreateError(cooldownCheck.errorMessage); return false }

        val dailyCheck = EventRateLimiter.checkDailyLimit(EventType.TOURNAMENT)
        if (!dailyCheck.isValid) { setCreateError(dailyCheck.errorMessage); return false }

        val titleCheck = InputValidator.validateEventTitle(title)
        if (!titleCheck.isValid) { setCreateError(titleCheck.errorMessage); return false }

        val venueCheck = InputValidator.validateEventVenue(location)
        if (!venueCheck.isValid) { setCreateError(venueCheck.errorMessage); return false }

        val feeCheck = InputValidator.validateEntryFee(entryFee)
        if (!feeCheck.isValid) { setCreateError(feeCheck.errorMessage); return false }

        val paymentCheck = InputValidator.validatePaymentInfo(paymentInfo)
        if (!paymentCheck.isValid) { setCreateError(paymentCheck.errorMessage); return false }

        val prizeCheck = InputValidator.validatePrizeInfo(prizeInfo)
        if (!prizeCheck.isValid) { setCreateError(prizeCheck.errorMessage); return false }

        val minimumLeadTimeMs = 3 * 3600 * 1000L
        if (date.time <= System.currentTimeMillis() + minimumLeadTimeMs) {
            setCreateError("Tournament must be at least 3 hours from now.")
            return false
        }

        if (registrationDeadline != null && registrationDeadline.before(Date())) {
            setCreateError("Registration deadline must be in the future.")
            return false
        }

        try {
            val countryCode = RegionNormalizer.normalizeCountryCode(null)
            val postalCode = RegionNormalizer.normalizePostal(null, null)

            tournamentRepo.createTournament(
                title = title, date = date,
                location = location, locationAddress = locationAddress,
                locationLatitude = locationLatitude, locationLongitude = locationLongitude,
                countryCode = countryCode, postalCode = postalCode,
                format = format, matchFormat = matchFormat,
                formatConfig = formatConfig,
                randomPairing = randomPairing,
                registrationDeadline = registrationDeadline,
                createdBy = createdBy,
                entryFee = entryFee, currency = currency, paymentInfo = paymentInfo,
                prizeInfo = prizeInfo,
                durationMinutes = durationMinutes,
                ageGroup = ageGroup,
                sportType = sportType,
                scoringConfig = scoringConfig,
            )

            EventRateLimiter.recordCreation(EventType.TOURNAMENT)
            loadInternal()
            return true
        } catch (e: Exception) {
            setCreateError(e.message ?: "Failed to publish tournament")
            return false
        }
    }

    fun cancel(tournament: Tournament) {
        viewModelScope.launch {
            try {
                tournamentRepo.cancelTournament(tournament)
                load()
            } catch (e: Exception) {
                setCreateError(e.message)
            }
        }
    }

    fun delete(tournament: Tournament) {
        viewModelScope.launch {
            try {
                tournamentRepo.deleteTournament(tournament)
                load()
            } catch (e: Exception) {
                setCreateError(e.message)
            }
        }
    }

    private fun setCreateError(message: String?) {
        _uiState.value = _uiState.value.copy(createError = message ?: "Something went wrong.")
    }
}
