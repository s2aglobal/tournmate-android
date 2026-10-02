package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.*
import java.util.Date
import java.util.UUID

interface TournamentRepository {
    suspend fun findTournament(id: UUID): Tournament?
    suspend fun listTournaments(): List<Tournament>
    suspend fun listPastTournaments(): List<Tournament>
    suspend fun createTournament(
        title: String,
        date: Date,
        location: String,
        locationAddress: String,
        locationLatitude: Double?,
        locationLongitude: Double?,
        countryCode: String?,
        postalCode: String?,
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
    )
    suspend fun updateTournament(
        tournament: Tournament,
        title: String,
        date: Date,
        location: String,
        locationAddress: String,
        locationLatitude: Double?,
        locationLongitude: Double?,
        countryCode: String?,
        postalCode: String?,
        format: TournamentFormat,
        matchFormat: MatchFormat,
        formatConfig: FormatConfig?,
        randomPairing: Boolean,
        registrationDeadline: Date,
        entryFee: Double?,
        currency: String,
        paymentInfo: String?,
        prizeInfo: String?,
        durationMinutes: Int?,
        ageGroup: AgeGroup,
        scoringConfig: ScoringConfig? = null,
    )
    suspend fun cancelTournament(tournament: Tournament)
    suspend fun deleteTournament(tournament: Tournament)
}
