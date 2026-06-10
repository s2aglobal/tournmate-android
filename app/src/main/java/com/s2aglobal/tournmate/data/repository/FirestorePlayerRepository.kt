package com.s2aglobal.tournmate.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.s2aglobal.tournmate.data.mapper.toFirestoreMap
import com.s2aglobal.tournmate.data.mapper.toPlayer
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.PlayerAvatar
import com.s2aglobal.tournmate.domain.model.SportType
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestorePlayerRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : PlayerRepository {

    private val collection get() = db.collection("players")

    override suspend fun listPlayers(): List<Player> =
        collection
            .orderBy("name")
            .get()
            .await()
            .documents
            .mapNotNull { it.toPlayer() }

    override suspend fun addPlayer(
        name: String,
        phone: String,
        email: String,
        gender: Gender,
        avatarId: String,
        homeCountryCode: String?,
        homePostalCode: String?,
        firebaseUid: String?,
    ): Player {
        val player = Player(
            id = UUID.randomUUID(),
            name = name,
            phone = phone,
            email = email.lowercase(),
            genderRaw = gender.rawValue,
            createdAt = Date(),
            eloRatings = mapOf("badminton" to 1200.0),
            preferredSport = SportType.BADMINTON,
            streak = 0,
            firebaseUid = firebaseUid,
            avatarId = avatarId.ifBlank { PlayerAvatar.DEFAULT.id },
            homeCountryCode = homeCountryCode,
            homePostalCode = homePostalCode,
        )
        collection.document(player.id.toString().uppercase())
            .set(player.toFirestoreMap())
            .await()
        return player
    }

    override suspend fun findPlayerById(id: UUID): Player? {
        val upperDoc = collection.document(id.toString().uppercase()).get().await()
        upperDoc.toPlayer()?.let { return it }
        val lowerDoc = collection.document(id.toString().lowercase()).get().await()
        return lowerDoc.toPlayer()
    }

    override suspend fun findPlayerByEmail(email: String): Player? =
        collection
            .whereEqualTo("email", email.lowercase())
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toPlayer()

    override suspend fun findPlayerByPhone(phone: String): Player? =
        collection
            .whereEqualTo("phone", phone)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toPlayer()

    override suspend fun findPlayerByFirebaseUid(uid: String): Player? =
        collection
            .whereEqualTo("firebaseUid", uid)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.toPlayer()

    override suspend fun updatePlayer(player: Player) {
        collection.document(player.id.toString().uppercase())
            .set(player.toFirestoreMap(), SetOptions.merge())
            .await()
    }

    override suspend fun deletePlayer(id: UUID) {
        collection.document(id.toString().uppercase())
            .delete()
            .await()
    }
}
