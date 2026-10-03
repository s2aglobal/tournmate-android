package com.s2aglobal.tournmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.data.mapper.toFirestoreMap
import com.s2aglobal.tournmate.data.mapper.toTournament
import com.s2aglobal.tournmate.domain.model.*
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTournamentRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : TournamentRepository {

    private val collection get() = db.collection("tournaments")

    override suspend fun findTournament(id: UUID): Tournament? {
        val upper = id.toString().uppercase()
        val doc = collection.document(upper).get().await()
        if (doc.exists()) return doc.toTournament()
        // Fallback for legacy lowercase document IDs
        val lower = id.toString().lowercase()
        val fallback = collection.document(lower).get().await()
        return fallback.toTournament()
    }

    override suspend fun listTournaments(): List<Tournament> {
        val today = startOfToday()

        return collection
            .whereGreaterThanOrEqualTo("date", Timestamp(today))
            .orderBy("date")
            .get()
            .await()
            .documents
            .mapNotNull { it.toTournament() }
    }

    override suspend fun listPastTournaments(): List<Tournament> {
        val today = startOfToday()

        return collection
            .whereLessThan("date", Timestamp(today))
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toTournament() }
    }

    private fun startOfToday(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    override suspend fun createTournament(
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
        scoringConfig: ScoringConfig?,
    ) {
        val configJson = formatConfig?.encode()
        val deadline = registrationDeadline ?: Tournament.defaultDeadline(date)

        val tournament = Tournament(
            id = UUID.randomUUID(),
            title = title,
            date = date,
            location = location,
            locationAddress = locationAddress,
            locationLatitude = locationLatitude,
            locationLongitude = locationLongitude,
            statusRaw = TournamentStatus.SCHEDULED.rawValue,
            formatRaw = format.rawValue,
            matchFormatRaw = matchFormat.rawValue,
            randomPairing = randomPairing,
            registrationDeadline = deadline,
            createdAt = Date(),
            createdBy = createdBy,
            entryFee = entryFee,
            currency = currency,
            paymentInfo = paymentInfo,
            prizeInfo = prizeInfo,
            ageGroupRaw = ageGroup.rawValue,
            durationMinutes = durationMinutes,
            formatConfigData = configJson,
            scoringConfigData = (scoringConfig ?: sportType.scoringRules.defaultConfig).encode(),
            countryCode = countryCode,
            postalCode = postalCode,
            timeZone = java.util.TimeZone.getDefault().id,
            sportType = sportType,
        )

        collection.document(tournament.id.toString().uppercase())
            .set(tournament.toFirestoreMap())
            .await()
    }

    override suspend fun updateTournament(
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
        scoringConfig: ScoringConfig?,
    ) {
        val configJson = formatConfig?.encode() ?: tournament.formatConfigData

        val updated = tournament.copy(
            title = title,
            date = date,
            location = location,
            locationAddress = locationAddress,
            locationLatitude = locationLatitude,
            locationLongitude = locationLongitude,
            countryCode = countryCode,
            postalCode = postalCode,
            formatRaw = format.rawValue,
            matchFormatRaw = matchFormat.rawValue,
            randomPairing = randomPairing,
            registrationDeadline = registrationDeadline,
            entryFee = entryFee,
            currency = currency,
            paymentInfo = paymentInfo,
            prizeInfo = prizeInfo,
            durationMinutes = durationMinutes,
            ageGroupRaw = ageGroup.rawValue,
            formatConfigData = configJson,
            scoringConfigData = scoringConfig?.encode() ?: tournament.scoringConfigData,
        )

        collection.document(tournament.id.toString().uppercase())
            .set(updated.toFirestoreMap(), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    override suspend fun cancelTournament(tournament: Tournament) {
        collection.document(tournament.id.toString().uppercase())
            .update("statusRaw", TournamentStatus.CANCELLED.rawValue)
            .await()
    }

    override suspend fun deleteTournament(tournament: Tournament) {
        collection.document(tournament.id.toString().uppercase())
            .delete()
            .await()
    }
}
