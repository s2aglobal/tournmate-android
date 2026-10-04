package com.s2aglobal.tournmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.s2aglobal.tournmate.data.mapper.toMatch
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.service.elo.EloEngine
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreMatchRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val registrationRepo: RegistrationRepository,
    private val playerRepo: PlayerRepository,
) : MatchRepository {

    private val collection get() = db.collection("matches")

    override suspend fun createMatch(
        tournament: Tournament, teamA: Registration, teamB: Registration,
        round: Int?, bracketPosition: Int?,
    ) {
        val match = Match(
            id = UUID.randomUUID(),
            tournamentId = tournament.id.toString().uppercase(),
            teamAId = teamA.id.toString().uppercase(),
            teamBId = teamB.id.toString().uppercase(),
            round = round,
            bracketPosition = bracketPosition,
            createdAt = Date(),
            sportType = tournament.sportType,
            tournament = tournament,
            teamA = teamA,
            teamB = teamB,
        )
        val data = encodeMatch(match)
        collection.document(match.id.toString().uppercase()).set(data).await()
    }

    override suspend fun createMatches(
        tournament: Tournament,
        schedule: List<Triple<Registration, Registration, Int>>,
    ) {
        val batch = db.batch()
        for ((teamA, teamB, round) in schedule) {
            val match = Match(
                id = UUID.randomUUID(),
                tournamentId = tournament.id.toString().uppercase(),
                teamAId = teamA.id.toString().uppercase(),
                teamBId = teamB.id.toString().uppercase(),
                round = round,
                createdAt = Date(),
                sportType = tournament.sportType,
            )
            val ref = collection.document(match.id.toString().uppercase())
            batch.set(ref, encodeMatch(match))
        }
        batch.commit().await()
    }

    override suspend fun createGroupMatches(
        tournament: Tournament,
        schedule: List<GroupMatchEntry>,
    ) {
        val batch = db.batch()
        for (entry in schedule) {
            val match = Match(
                id = UUID.randomUUID(),
                tournamentId = tournament.id.toString().uppercase(),
                teamAId = entry.teamA.id.toString().uppercase(),
                teamBId = entry.teamB.id.toString().uppercase(),
                round = entry.round,
                groupLabel = entry.group,
                createdAt = Date(),
                sportType = tournament.sportType,
            )
            val ref = collection.document(match.id.toString().uppercase())
            batch.set(ref, encodeMatch(match))
        }
        batch.commit().await()
    }

    override suspend fun listMatches(tournament: Tournament): List<Match> {
        val tid = tournament.id.toString().uppercase()
        val snapshot = try {
            collection.whereEqualTo("tournamentId", tid).get(Source.SERVER).await()
        } catch (_: Exception) {
            collection.whereEqualTo("tournamentId", tid).get(Source.CACHE).await()
        }

        val registrations = registrationRepo.listRegistrations(tournament)
        val regMap = registrations.associateBy { it.id.toString().uppercase() }

        return snapshot.documents.mapNotNull { doc ->
            val id = try { UUID.fromString(doc.id) } catch (_: Exception) { return@mapNotNull null }
            val teamAId = doc.getString("teamAId") ?: return@mapNotNull null
            val teamBId = doc.getString("teamBId") ?: return@mapNotNull null
            val teamA = regMap[teamAId] ?: return@mapNotNull null
            val teamB = regMap[teamBId] ?: return@mapNotNull null

            val setScoresRaw = doc.get("setScores") as? List<*>
            val setScores = setScoresRaw?.mapNotNull { raw ->
                val map = raw as? Map<*, *> ?: return@mapNotNull null
                val a = (map["teamAPoints"] as? Number)?.toInt() ?: return@mapNotNull null
                val b = (map["teamBPoints"] as? Number)?.toInt() ?: return@mapNotNull null
                SetScore(a, b)
            } ?: emptyList()

            Match(
                id = id,
                tournamentId = tid,
                teamAId = teamAId,
                teamBId = teamBId,
                winnerRegistrationId = doc.getString("winnerRegistrationId"),
                scoreA = doc.getLong("scoreA")?.toInt(),
                scoreB = doc.getLong("scoreB")?.toInt(),
                setScores = setScores,
                statusRaw = doc.getString("statusRaw") ?: MatchStatus.SCHEDULED.rawValue,
                createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date(),
                round = doc.getLong("round")?.toInt(),
                bracketPosition = doc.getLong("bracketPosition")?.toInt(),
                groupLabel = doc.getString("groupLabel"),
                sportType = SportType.fromRawValue(doc.getString("sportType")),
                sportTypeRaw = unknownSportRaw(doc.getString("sportType")),
                submittedBy = doc.getString("submittedBy"),
                confirmedBy = doc.getString("confirmedBy"),
                tournament = tournament,
                teamA = teamA,
                teamB = teamB,
            )
        }.sortedWith(compareBy<Match> { it.round ?: Int.MAX_VALUE }
            .thenBy { it.bracketPosition ?: Int.MAX_VALUE }
            .thenBy { it.createdAt })
    }

    /**
     * Organizer saves a simple score (one number per side). The simple score
     * replaces any earlier game-by-game `setScores`, which are deleted so they
     * can't override the correction. Mirrors iOS `finalizeMatch`.
     */
    override suspend fun finalizeMatch(match: Match, scoreA: Int, scoreB: Int) {
        val winnerId = Match.winnerId(scoreA, scoreB, match.teamAId, match.teamBId)
        val updateData = mutableMapOf<String, Any>(
            "scoreA" to scoreA,
            "scoreB" to scoreB,
            "setScores" to FieldValue.delete(),
            "statusRaw" to MatchStatus.FINISHED.rawValue,
            // A draw clears any winner left over from an earlier score.
            "winnerRegistrationId" to (winnerId ?: FieldValue.delete()),
        )

        val docRef = collection.document(match.id.toString().uppercase())
        docRef.set(updateData, SetOptions.merge()).await()

        // Correcting an already-finished match must not count the result twice.
        if (match.status != MatchStatus.FINISHED) applyEloIfNeeded(match, winnerId)
    }

    override suspend fun submitSetScores(match: Match, setScores: List<SetScore>, submittedBy: String) {
        val updateData = setScoreFields(match, setScores) + mapOf(
            "submittedBy" to submittedBy,
            "statusRaw" to MatchStatus.SCORE_SUBMITTED.rawValue,
        )
        collection.document(match.id.toString().uppercase())
            .set(updateData, SetOptions.merge()).await()
    }

    override suspend fun confirmScore(match: Match, confirmedBy: String) {
        val winnerId = match.winnerRegistrationId
        val updateData = mapOf(
            "confirmedBy" to confirmedBy,
            "statusRaw" to MatchStatus.FINISHED.rawValue,
            "winnerRegistrationId" to (winnerId ?: FieldValue.delete()),
        )

        collection.document(match.id.toString().uppercase())
            .set(updateData, SetOptions.merge()).await()

        if (match.status != MatchStatus.FINISHED) applyEloIfNeeded(match, winnerId)
    }

    override suspend fun disputeScore(match: Match, disputedBy: String) {
        collection.document(match.id.toString().uppercase())
            .set(mapOf("statusRaw" to MatchStatus.DISPUTED.rawValue), SetOptions.merge()).await()
    }

    /** Organizer sets the final game-by-game score (dispute resolution and "Edit Score"). */
    override suspend fun resolveDispute(match: Match, setScores: List<SetScore>) {
        val updateData = setScoreFields(match, setScores) + mapOf("statusRaw" to MatchStatus.FINISHED.rawValue)
        collection.document(match.id.toString().uppercase())
            .set(updateData, SetOptions.merge()).await()

        if (match.status != MatchStatus.FINISHED) {
            applyEloIfNeeded(match, Match.winnerId(setScores.count { it.teamAWon }, setScores.count { it.teamBWon }, match.teamAId, match.teamBId))
        }
    }

    /**
     * `setScores` plus `scoreA`/`scoreB` (games won) and the winner derived from
     * them, so the fields can never disagree; a drawn set count deletes any stale
     * winner. Mirrors iOS `applySetScores`.
     */
    private fun setScoreFields(match: Match, setScores: List<SetScore>): Map<String, Any> {
        val setsWonA = setScores.count { it.teamAWon }
        val setsWonB = setScores.count { it.teamBWon }
        val winnerId = Match.winnerId(setsWonA, setsWonB, match.teamAId, match.teamBId)
        return mapOf(
            "setScores" to setScores.map { mapOf("teamAPoints" to it.teamAPoints, "teamBPoints" to it.teamBPoints) },
            "scoreA" to setsWonA,
            "scoreB" to setsWonB,
            "winnerRegistrationId" to (winnerId ?: FieldValue.delete()),
        )
    }

    override suspend fun matchesForTournament(tournamentId: String): List<Match> {
        val snapshot = collection
            .whereEqualTo("tournamentId", tournamentId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toMatch()
        }
    }

    override suspend fun deleteMatches(tournament: Tournament) {
        val tid = tournament.id.toString().uppercase()
        val snapshot = collection.whereEqualTo("tournamentId", tid).get().await()
        val batch = db.batch()
        for (doc in snapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }

    /**
     * Client-side Elo + streak update. Best-effort: security rules reject client
     * writes to elo/streak; the server applies Elo once when the match first
     * becomes finished (guarded by `match.eloApplied`).
     */
    private suspend fun applyEloIfNeeded(match: Match, winnerId: String?) {
        if (winnerId == null) return
        val winnerReg = if (winnerId == match.teamAId) match.teamA else match.teamB
        val loserReg = if (winnerId == match.teamAId) match.teamB else match.teamA

        val winners = listOfNotNull(winnerReg.player, winnerReg.partner)
        val losers = listOfNotNull(loserReg.player, loserReg.partner)

        val current = linkedMapOf<UUID, Player>()
        (winners + losers).forEach { current[it.id] = it }
        for (w in winners) {
            for (l in losers) {
                val (newW, newL) = EloEngine.applyResult(current.getValue(w.id), current.getValue(l.id), match.sportType)
                current[w.id] = newW
                current[l.id] = newL
            }
        }
        winners.forEach { w -> current[w.id] = current.getValue(w.id).let { it.copy(streak = maxOf(it.streak, 0) + 1) } }
        losers.forEach { l -> current[l.id] = current.getValue(l.id).let { it.copy(streak = minOf(it.streak, 0) - 1) } }

        for (player in current.values) {
            try {
                playerRepo.updatePlayerFields(player.id, mapOf(
                    "eloRatings" to player.eloRatings,
                    "elo" to player.elo(SportType.BADMINTON),
                    "streak" to player.streak,
                ))
            } catch (_: Exception) { }
        }
    }

    private fun encodeMatch(match: Match): Map<String, Any> = buildMap {
        put("tournamentId", match.tournamentId)
        put("teamAId", match.teamAId)
        put("teamBId", match.teamBId)
        put("statusRaw", match.statusRaw)
        put("createdAt", Timestamp(match.createdAt))
        match.round?.let { put("round", it) }
        match.bracketPosition?.let { put("bracketPosition", it) }
        match.groupLabel?.let { put("groupLabel", it) }
        put("sportType", match.sportType.storedRawValue(match.sportTypeRaw))
    }
}
