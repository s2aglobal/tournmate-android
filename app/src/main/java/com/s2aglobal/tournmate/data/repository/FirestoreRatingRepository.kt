package com.s2aglobal.tournmate.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.s2aglobal.tournmate.data.mapper.toPlayerRating
import com.s2aglobal.tournmate.domain.model.PlayerRating
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreRatingRepository @Inject constructor(
    private val db: FirebaseFirestore,
) : RatingRepository {

    private val collection get() = db.collection("ratings")

    override suspend fun ratingsForPlayer(playerId: String): List<PlayerRating> {
        val snapshot = collection
            .whereEqualTo("playerId", playerId)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toPlayerRating() }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun addRating(rating: PlayerRating) {
        val data = buildMap<String, Any?> {
            put("playerId", rating.playerId)
            put("raterId", rating.raterId)
            rating.tournamentId?.let { put("tournamentId", it) }
            put("stars", rating.stars)
            rating.comment?.let { put("comment", it) }
            put("createdAt", Timestamp(rating.createdAt))
        }
        collection.document(rating.id.toString().uppercase()).set(data).await()
    }

    override suspend fun hasRated(raterId: String, playerId: String): Boolean {
        val snapshot = collection
            .whereEqualTo("raterId", raterId)
            .whereEqualTo("playerId", playerId)
            .limit(1)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    override suspend fun averageRating(playerId: String): Double {
        val ratings = ratingsForPlayer(playerId)
        if (ratings.isEmpty()) return 0.0
        return ratings.map { it.stars }.average()
    }
}
