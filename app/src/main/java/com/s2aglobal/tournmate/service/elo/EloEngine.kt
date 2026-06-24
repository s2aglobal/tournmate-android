package com.s2aglobal.tournmate.service.elo

import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SportType
import kotlin.math.pow

object EloEngine {
    const val K = 24.0

    fun applyResult(winner: Player, loser: Player, sport: SportType = SportType.BADMINTON): Pair<Player, Player> {
        val ra = winner.elo(sport)
        val rb = loser.elo(sport)

        val ea = 1.0 / (1.0 + 10.0.pow((rb - ra) / 400.0))
        val eb = 1.0 / (1.0 + 10.0.pow((ra - rb) / 400.0))

        val newWinner = winner.withElo(ra + K * (1.0 - ea), sport)
        val newLoser = loser.withElo(rb + K * (0.0 - eb), sport)

        return newWinner to newLoser
    }
}
