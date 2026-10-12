package com.s2aglobal.tournmate.domain.model

import java.util.Date

/**
 * A persistent team for team sports (cricket first). Created by a captain and
 * reused across tournaments. Firestore: `/teams/{id}`
 * (name, sportType, captainId, playerIds[], colorHex?, createdBy, createdAt).
 */
data class Team(
    val id: String,
    val name: String,
    val sportType: SportType,
    /** Player id of the captain. */
    val captainId: String,
    val playerIds: List<String>,
    val colorHex: String? = null,
    /** Firebase uid of the creator; only they can rename or delete the team. */
    val createdBy: String,
    val createdAt: Date = Date(),
    /** Players resolved from [playerIds] (filled by the repository). */
    val players: List<Player> = emptyList(),
) {
    fun isCaptain(player: Player?): Boolean =
        player != null && captainId.equals(player.id.toString(), ignoreCase = true)

    fun contains(player: Player): Boolean =
        playerIds.any { it.equals(player.id.toString(), ignoreCase = true) }

    companion object {
        const val NAME_LIMIT = 40
        const val MAX_PLAYERS = 30
    }
}
