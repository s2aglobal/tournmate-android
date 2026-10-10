package com.s2aglobal.tournmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.data.mapper.stringList
import com.s2aglobal.tournmate.data.mapper.toPlayer
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.domain.model.Team
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collection `/teams/{id}`: name, sportType, captainId, playerIds[], colorHex?,
 * createdBy, createdAt. Same shape as iOS `FirestoreTeamRepository`.
 */
@Singleton
class FirestoreTeamRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : TeamRepository {

    private val collection get() = db.collection("teams")

    override suspend fun myTeams(playerId: String, sport: SportType): List<Team> {
        val snapshot = collection.whereArrayContains("playerIds", playerId).get().await()
        val teams = snapshot.documents.mapNotNull { it.toTeam() }.filter { it.sportType == sport }
        val players = fetchPlayers(teams.flatMap { it.playerIds }.toSet())
        return teams
            .map { t -> t.copy(players = t.playerIds.mapNotNull { players[it] }) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    override suspend fun team(id: String): Team? {
        val team = collection.document(id).get().await().toTeam() ?: return null
        val players = fetchPlayers(team.playerIds.toSet())
        return team.copy(players = team.playerIds.mapNotNull { players[it] })
    }

    override suspend fun createTeam(name: String, sport: SportType, captain: Player, createdBy: String): Team {
        val captainId = captain.id.toString().uppercase()
        val team = Team(
            id = UUID.randomUUID().toString().uppercase(),
            name = name.trim(),
            sportType = sport,
            captainId = captainId,
            playerIds = listOf(captainId),
            createdBy = createdBy,
            createdAt = Date(),
            players = listOf(captain),
        )
        collection.document(team.id).set(encode(team)).await()
        return team
    }

    override suspend fun setPlayers(playerIds: List<String>, team: Team) {
        collection.document(team.id).update("playerIds", playerIds).await()
    }

    override suspend fun joinTeam(team: Team, player: Player) {
        if (team.contains(player)) return
        collection.document(team.id)
            .update("playerIds", FieldValue.arrayUnion(player.id.toString().uppercase()))
            .await()
    }

    override suspend fun leaveTeam(team: Team, player: Player) {
        collection.document(team.id)
            .update("playerIds", FieldValue.arrayRemove(player.id.toString().uppercase()))
            .await()
    }

    override suspend fun deleteTeam(team: Team) {
        collection.document(team.id).delete().await()
    }

    private fun encode(team: Team): Map<String, Any> = buildMap {
        put("name", team.name)
        put("sportType", team.sportType.rawValue)
        put("captainId", team.captainId)
        put("playerIds", team.playerIds)
        put("createdBy", team.createdBy)
        put("createdAt", Timestamp(team.createdAt))
        team.colorHex?.let { put("colorHex", it) }
    }

    private fun DocumentSnapshot.toTeam(): Team? {
        if (!exists()) return null
        return Team(
            id = id,
            name = getString("name") ?: return null,
            sportType = SportType.fromRawValue(getString("sportType")),
            captainId = getString("captainId") ?: return null,
            playerIds = stringList("playerIds"),
            colorHex = getString("colorHex"),
            createdBy = getString("createdBy") ?: return null,
            createdAt = getTimestamp("createdAt")?.toDate() ?: Date(),
        )
    }

    private suspend fun fetchPlayers(ids: Set<String>): Map<String, Player> {
        if (ids.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, Player>()
        for (batch in ids.chunked(30)) {
            val snapshot = db.collection("players").whereIn(FieldPath.documentId(), batch).get().await()
            for (doc in snapshot.documents) doc.toPlayer()?.let { map[doc.id] = it }
        }
        return map
    }
}
