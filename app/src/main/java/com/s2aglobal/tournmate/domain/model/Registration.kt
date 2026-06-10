package com.s2aglobal.tournmate.domain.model

import java.util.Date
import java.util.UUID

data class Registration(
    val id: UUID = UUID.randomUUID(),
    val tournamentId: String = "",
    val playerId: String = "",
    val partnerId: String? = null,
    val createdAt: Date = Date(),
    val tournament: Tournament = Tournament(),
    val player: Player = Player(),
    val partner: Player? = null,
) {
    val isTeamFormed: Boolean
        get() = partnerId != null

    fun contains(playerIdToCheck: String): Boolean =
        playerId == playerIdToCheck || partnerId == playerIdToCheck

    fun containsPlayerID(id: UUID): Boolean =
        contains(id.toString()) || contains(id.toString().uppercase()) || contains(id.toString().lowercase())
}
