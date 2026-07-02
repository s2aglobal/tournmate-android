package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.PlayerRating

interface RatingRepository {
    suspend fun ratingsForPlayer(playerId: String): List<PlayerRating>
    suspend fun addRating(rating: PlayerRating)
    suspend fun hasRated(raterId: String, playerId: String): Boolean
    suspend fun averageRating(playerId: String): Double
}
