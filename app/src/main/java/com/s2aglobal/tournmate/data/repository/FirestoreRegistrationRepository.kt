package com.s2aglobal.tournmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.data.mapper.toPlayer
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.Tournament
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRegistrationRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : RegistrationRepository {

    private val collection get() = db.collection("registrations")

    override suspend fun listRegistrations(tournament: Tournament): List<Registration> {
        val tid = tournament.id.toString().uppercase()

        val snapshot = collection
            .whereEqualTo("tournamentId", tid)
            .get()
            .await()

        val playerIds = mutableSetOf<String>()
        for (doc in snapshot.documents) {
            doc.getString("playerId")?.let { playerIds.add(it) }
            doc.getString("partnerId")?.let { playerIds.add(it) }
        }

        val playerMap = fetchPlayerMap(playerIds)

        return snapshot.documents.mapNotNull { doc ->
            val id = try { UUID.fromString(doc.id) } catch (_: Exception) { return@mapNotNull null }
            val playerId = doc.getString("playerId") ?: return@mapNotNull null
            val player = playerMap[playerId] ?: return@mapNotNull null
            val partnerId = doc.getString("partnerId")
            val partner = partnerId?.let { playerMap[it] }
            val createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()

            Registration(
                id = id,
                tournamentId = tid,
                playerId = playerId,
                partnerId = partnerId,
                createdAt = createdAt,
                tournament = tournament,
                player = player,
                partner = partner,
            )
        }.sortedBy { it.createdAt }
    }

    override suspend fun findOpenRegistration(
        player: Player,
        tournament: Tournament,
    ): Registration? {
        val snapshot = collection
            .whereEqualTo("tournamentId", tournament.id.toString().uppercase())
            .whereEqualTo("playerId", player.id.toString().uppercase())
            .get()
            .await()

        for (doc in snapshot.documents) {
            val partnerId = doc.getString("partnerId")
            if (partnerId == null) {
                val id = try { UUID.fromString(doc.id) } catch (_: Exception) { continue }
                val createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                return Registration(
                    id = id,
                    tournamentId = tournament.id.toString().uppercase(),
                    playerId = player.id.toString().uppercase(),
                    createdAt = createdAt,
                    tournament = tournament,
                    player = player,
                )
            }
        }
        return null
    }

    override suspend fun setPartner(partner: Player, registration: Registration) {
        collection.document(registration.id.toString().uppercase())
            .update("partnerId", partner.id.toString().uppercase())
            .await()
    }

    override suspend fun clearPartner(registration: Registration) {
        collection.document(registration.id.toString().uppercase())
            .update("partnerId", FieldValue.delete())
            .await()
    }

    override suspend fun createRegistration(
        player: Player,
        partner: Player?,
        tournament: Tournament,
    ) {
        val regId = UUID.randomUUID()
        val data = buildMap<String, Any> {
            put("tournamentId", tournament.id.toString().uppercase())
            put("playerId", player.id.toString().uppercase())
            put("createdAt", Timestamp(Date()))
            partner?.let { put("partnerId", it.id.toString().uppercase()) }
        }
        collection.document(regId.toString().uppercase())
            .set(data)
            .await()
    }

    override suspend fun deleteRegistration(registration: Registration) {
        collection.document(registration.id.toString().uppercase())
            .delete()
            .await()
    }

    override suspend fun tournamentIds(forPlayerID: UUID): Set<UUID> {
        val pid = forPlayerID.toString().uppercase()

        val asPlayer = collection
            .whereEqualTo("playerId", pid)
            .get()
            .await()

        val asPartner = collection
            .whereEqualTo("partnerId", pid)
            .get()
            .await()

        val ids = mutableSetOf<UUID>()
        for (doc in asPlayer.documents + asPartner.documents) {
            val tid = doc.getString("tournamentId") ?: continue
            try { ids.add(UUID.fromString(tid)) } catch (_: Exception) { /* skip */ }
        }
        return ids
    }

    private suspend fun fetchPlayerMap(ids: Set<String>): Map<String, Player> {
        if (ids.isEmpty()) return emptyMap()

        val map = mutableMapOf<String, Player>()
        for (batch in ids.chunked(30)) {
            val snapshot = db.collection("players")
                .whereIn(FieldPath.documentId(), batch)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.toPlayer()?.let { map[doc.id] = it }
            }
        }
        return map
    }
}
