package com.s2aglobal.tournmate.domain.model

import java.util.Date
import java.util.UUID

data class PlayerRating(
    val id: UUID = UUID.randomUUID(),
    val playerId: String = "",
    val raterId: String = "",
    val tournamentId: String? = null,
    val stars: Int = 3,
    val comment: String? = null,
    val createdAt: Date = Date(),
    val player: Player = Player(),
    val rater: Player = Player(),
    val tournament: Tournament? = null,
) {
    init {
        require(stars in 1..5) { "Stars must be between 1 and 5" }
    }
}
