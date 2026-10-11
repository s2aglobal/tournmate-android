package com.s2aglobal.tournmate.domain.model

/**
 * A player's record in one sport: matches, wins and current streak. Each sport has its own
 * rating, so the profile shows each sport's own record. Same rules on iOS (`PlayerSportStats.swift`).
 */
class PlayerSportStats(finished: List<Match>, player: Player, sport: SportType) {
    /** Finished matches in [sport], newest first. */
    val matches: List<Match>
    val wins: Int
    /**
     * Positive = consecutive wins, negative = consecutive losses. A tie or no result has no
     * winner and neither breaks nor extends it.
     */
    val streak: Int

    init {
        val inSport = finished
            .filter { it.status == MatchStatus.FINISHED && it.sportType == sport }
            .sortedByDescending { it.createdAt }
        fun won(m: Match) = m.winnerRegistration?.containsPlayerID(player.id) == true
        matches = inSport
        wins = inSport.count(::won)
        var run = 0
        for (match in inSport) {
            if (match.winnerRegistration == null) continue
            val w = won(match)
            if (run == 0) run = if (w) 1 else -1
            else if ((run > 0) == w) run += if (w) 1 else -1
            else break
        }
        streak = run
    }
}
