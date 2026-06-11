package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.Player
import java.util.UUID

interface PlayerRepository {
    suspend fun listPlayers(): List<Player>
    suspend fun addPlayer(
        name: String,
        phone: String,
        email: String,
        gender: Gender,
        avatarId: String,
        homeCountryCode: String?,
        homePostalCode: String?,
        firebaseUid: String?,
    ): Player
    suspend fun findPlayerById(id: UUID): Player?
    suspend fun findPlayerByEmail(email: String): Player?
    suspend fun findPlayerByPhone(phone: String): Player?
    suspend fun findPlayerByFirebaseUid(uid: String): Player?
    suspend fun updatePlayer(player: Player)
    suspend fun updatePlayerFields(playerId: UUID, fields: Map<String, Any?>)
    suspend fun deletePlayer(id: UUID)
}
