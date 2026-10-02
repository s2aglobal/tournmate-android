package com.s2aglobal.tournmate.ui.screen.court

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.HomeRegionCountry
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.service.court.CourtResult
import com.s2aglobal.tournmate.service.court.CourtSearchService
import com.s2aglobal.tournmate.service.region.RegionNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A postal-code example used in empty-state suggestion pills. */
data class ZipExample(val zip: String, val label: String)

data class CourtFinderUiState(
    val zipCode: String = "",
    val courts: List<CourtResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val usesHomeRegion: Boolean = false,
    val popularExamples: List<ZipExample> = listOf(
        ZipExample("78641", "Leander"),
        ZipExample("78660", "Pflugerville"),
        ZipExample("78745", "Austin"),
    ),
) {
    val welcomeSubtitle: String
        get() = if (usesHomeRegion) {
            "Court search follows your home country from Profile — the same region as tournaments and Open Play."
        } else {
            "Find badminton courts near where you play. Add your home region in Profile to match local listings."
        }

    val zipExamples: List<String> get() = popularExamples.map { it.zip }
}

/**
 * Manages the search-for-courts workflow (mirrors iOS CourtFinderViewModel).
 *
 * When the signed-in player has a complete home region (country + postal), postal search and
 * "use current location" search are limited to that country — aligned with tournaments and Open Play.
 */
@HiltViewModel
class CourtFinderViewModel @Inject constructor(
    private val searchService: CourtSearchService,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CourtFinderUiState())
    val uiState: StateFlow<CourtFinderUiState> = _uiState.asStateFlow()

    private var currentPlayer: Player? = null

    init {
        loadPlayerProfile()
    }

    fun onZipCodeChange(value: String) {
        _uiState.update { it.copy(zipCode = value) }
    }

    private fun loadPlayerProfile() {
        viewModelScope.launch {
            val id = currentUserStore.currentPlayerId()
            currentPlayer = id?.let { playerRepo.findPlayerById(it) }
            _uiState.update {
                it.copy(
                    usesHomeRegion = playerHasCompleteHomeRegion(),
                    popularExamples = examplesForHomeCountry(),
                )
            }
        }
    }

    fun searchByZipCode() {
        val trimmed = _uiState.value.zipCode.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter a zip code or postal code.") }
            return
        }

        viewModelScope.launch {
            // Ensure profile is loaded for region rules.
            if (currentPlayer == null) {
                val id = currentUserStore.currentPlayerId()
                currentPlayer = id?.let { playerRepo.findPlayerById(it) }
            }

            if (playerHasCompleteHomeRegion()) {
                val err = RegionNormalizer.validateHomePostalForCountry(
                    countryCode = currentPlayer?.homeCountryCode,
                    postalRaw = trimmed,
                )
                if (err != null) {
                    _uiState.update {
                        it.copy(errorMessage = err, hasSearched = true, courts = emptyList(), isLoading = false)
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, hasSearched = true, courts = emptyList())
            }

            val results = runCatching { searchService.searchCourts(trimmed, currentUserStore.preferredSportFlow.first()) }.getOrElse { emptyList() }
            _uiState.update { it.copy(courts = results, isLoading = false) }
        }
    }

    /** Searches near an explicit coordinate (the user's current location), respecting home-region rules. */
    fun searchNearLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            if (currentPlayer == null) {
                val id = currentUserStore.currentPlayerId()
                currentPlayer = id?.let { playerRepo.findPlayerById(it) }
            }

            if (playerHasCompleteHomeRegion()) {
                val homeCC = RegionNormalizer.normalizeCountryCode(currentPlayer?.homeCountryCode)
                val resolvedCC = RegionNormalizer.normalizeCountryCode(
                    searchService.reverseGeocodeCountryCode(latitude, longitude),
                )
                if (homeCC != null && resolvedCC != null && homeCC != resolvedCC) {
                    val place = HomeRegionCountry.matching(currentPlayer?.homeCountryCode ?: "")?.name
                        ?: "your home country"
                    _uiState.update {
                        it.copy(
                            errorMessage = "This location is outside your home country ($place). Search with a postal code in your home region, or update your home region in Profile.",
                            hasSearched = true,
                            courts = emptyList(),
                            isLoading = false,
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, hasSearched = true, courts = emptyList())
            }

            val results = runCatching { searchService.searchNearby(latitude, longitude, currentUserStore.preferredSportFlow.first()) }.getOrElse { emptyList() }
            _uiState.update { it.copy(courts = results, isLoading = false) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetSearch() {
        _uiState.update {
            it.copy(zipCode = "", courts = emptyList(), hasSearched = false, errorMessage = null)
        }
    }

    private fun playerHasCompleteHomeRegion(): Boolean {
        val p = currentPlayer ?: return false
        val cc = RegionNormalizer.normalizeCountryCode(p.homeCountryCode) ?: return false
        val pc = RegionNormalizer.normalizePostal(p.homePostalCode, cc)
        return !pc.isNullOrEmpty()
    }

    private fun examplesForHomeCountry(): List<ZipExample> {
        val cc = RegionNormalizer.normalizeCountryCode(currentPlayer?.homeCountryCode)
        return when (cc) {
            "IN" -> listOf(
                ZipExample("500081", "Hyderabad"),
                ZipExample("560001", "Bengaluru"),
                ZipExample("110001", "Delhi"),
            )
            else -> listOf(
                ZipExample("78641", "Leander"),
                ZipExample("78660", "Pflugerville"),
                ZipExample("78745", "Austin"),
            )
        }
    }
}
