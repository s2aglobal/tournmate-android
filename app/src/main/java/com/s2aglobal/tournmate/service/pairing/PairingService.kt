package com.s2aglobal.tournmate.service.pairing

import com.s2aglobal.tournmate.domain.model.*
import kotlin.math.log2

data class BracketResult(
    val matches: List<Triple<Registration, Registration, Int>>,
    val byeTeams: List<Registration>,
    val totalRounds: Int,
)

object PairingService {

    fun generateMixedDoublesPairs(registrations: List<Registration>): List<Pair<Registration, Registration>> {
        val solo = registrations.filter { it.partnerId == null }
        val males = solo.filter { it.player.gender == Gender.MALE }.shuffled()
        val females = solo.filter { it.player.gender == Gender.FEMALE }.shuffled()
        return males.zip(females)
    }

    fun generateRandomDoublesPairs(registrations: List<Registration>): List<Pair<Registration, Registration>> {
        val solo = registrations.filter { it.partnerId == null }.shuffled()
        val pairs = mutableListOf<Pair<Registration, Registration>>()
        var i = 0
        while (i + 1 < solo.size) {
            pairs.add(solo[i] to solo[i + 1])
            i += 2
        }
        return pairs
    }

    /**
     * Classic round robin: every team plays every other team exactly once.
     *
     * Circle method (Berger tables): fix the first slot and rotate the rest one
     * step per round. N teams play N − 1 rounds of N/2 matches; with an odd N a
     * "bye" slot is added, so there are N rounds and each team sits out once.
     * Home/away follows Berger: the fixed slot alternates by round, the other
     * pairs by position, so no team is listed first in every match. Mirrors iOS.
     */
    fun generateRoundRobinSchedule(
        teams: List<Registration>,
    ): List<Triple<Registration, Registration, Int>> {
        if (teams.size < 2) return emptyList()

        val slots: MutableList<Registration?> = teams.toMutableList()
        if (slots.size % 2 == 1) slots.add(null)
        val m = slots.size
        val schedule = mutableListOf<Triple<Registration, Registration, Int>>()

        for (round in 0 until m - 1) {
            for (i in 0 until m / 2) {
                val a = slots[i] ?: continue          // bye
                val b = slots[m - 1 - i] ?: continue  // bye
                val flip = if (i == 0) round % 2 == 1 else i % 2 == 1
                schedule.add(if (flip) Triple(b, a, round + 1) else Triple(a, b, round + 1))
            }
            // Rotate every slot but the first.
            val last = slots.removeAt(slots.size - 1)
            slots.add(1, last)
        }
        return schedule
    }

    /**
     * Pairs the teams advancing to the next knockout round: first vs last,
     * second vs second-last… In round 1 the bye teams come first, then the
     * round-1 winners (see TournamentDetailViewModel.advanceToNextRound).
     */
    fun nextRoundPairs(advancing: List<Registration>): List<Pair<Registration, Registration>> {
        val half = advancing.size / 2
        return (0 until half).map { advancing[it] to advancing[advancing.size - 1 - it] }
    }

    fun generateBracketFirstRound(teams: List<Registration>): BracketResult {
        if (teams.size < 2) return BracketResult(emptyList(), emptyList(), 0)

        val seeded = teams.sortedByDescending { avgElo(it) }
        val bracketSize = nextPowerOf2(seeded.size)
        val totalRounds = log2(bracketSize.toDouble()).toInt()
        val byeCount = bracketSize - seeded.size

        val byeTeams = seeded.take(byeCount)
        val playingTeams = seeded.drop(byeCount)

        val matches = mutableListOf<Triple<Registration, Registration, Int>>()
        val half = playingTeams.size / 2

        for (i in 0 until half) {
            val teamA = playingTeams[i]
            val teamB = playingTeams[playingTeams.size - 1 - i]
            matches.add(Triple(teamA, teamB, i))
        }

        return BracketResult(matches, byeTeams, totalRounds)
    }

    fun optimalGroupCount(totalTeams: Int, advancingPerGroup: Int): Int {
        if (totalTeams < 4 || advancingPerGroup < 1) return 2
        val minGroupSize = advancingPerGroup + 1
        val maxGroups = totalTeams / minGroupSize
        // Too few teams for two groups: one group, its top teams go to the knockout.
        if (maxGroups < 2) return 1

        for (candidateGroups in 2..maxGroups) {
            val groupSize = totalTeams / candidateGroups
            if (groupSize < minGroupSize) break
            val advancingTotal = candidateGroups * advancingPerGroup
            val isPowerOf2 = advancingTotal > 0 && (advancingTotal and (advancingTotal - 1)) == 0
            if (isPowerOf2) return candidateGroups
        }

        return maxGroups
    }

    fun generateGroupStageSchedule(
        teams: List<Registration>,
        groupCount: Int,
        advancingPerGroup: Int = 2,
    ): List<GroupStageEntry> {
        if (teams.size < 4 || groupCount < 1) return emptyList()

        val effectiveGroupCount = optimalGroupCount(teams.size, advancingPerGroup)
        val seeded = teams.sortedByDescending { avgElo(it) }

        val groups = Array(effectiveGroupCount) { mutableListOf<Registration>() }
        for ((index, team) in seeded.withIndex()) {
            val row = index / effectiveGroupCount
            val col = index % effectiveGroupCount
            val groupIndex = if (row % 2 == 0) col else (effectiveGroupCount - 1 - col)
            groups[groupIndex].add(team)
        }

        val groupLabels = (0 until effectiveGroupCount).map { ('A' + it).toString() }
        val schedule = mutableListOf<GroupStageEntry>()

        for ((gi, group) in groups.withIndex()) {
            val label = groupLabels[gi]
            val rrSchedule = generateRoundRobinSchedule(group.toList())
            for ((teamA, teamB, round) in rrSchedule) {
                schedule.add(GroupStageEntry(teamA, teamB, round, label))
            }
        }

        return schedule
    }

    fun generateSwissPairings(
        teams: List<Registration>,
        finishedMatches: List<Match>,
        swissRound: Int,
    ): List<Pair<Registration, Registration>> {
        if (teams.size < 2) return emptyList()

        val wins = mutableMapOf<String, Int>()
        for (team in teams) wins[team.id.toString().uppercase()] = 0
        for (match in finishedMatches) {
            if (match.status == MatchStatus.FINISHED) {
                match.winnerRegistrationId?.let { wid ->
                    wins[wid] = (wins[wid] ?: 0) + 1
                }
            }
        }

        val playedPairs = mutableSetOf<String>()
        for (match in finishedMatches) {
            playedPairs.add(pairKey(match.teamAId, match.teamBId))
        }

        val sorted = teams.sortedWith(compareByDescending<Registration> {
            wins[it.id.toString().uppercase()] ?: 0
        }.thenByDescending { avgElo(it) })

        val used = mutableSetOf<String>()
        val pairs = mutableListOf<Pair<Registration, Registration>>()

        for (i in sorted.indices) {
            val a = sorted[i]
            val aId = a.id.toString().uppercase()
            if (aId in used) continue

            for (j in (i + 1) until sorted.size) {
                val b = sorted[j]
                val bId = b.id.toString().uppercase()
                if (bId in used) continue

                val key = pairKey(aId, bId)
                if (key in playedPairs) continue

                pairs.add(a to b)
                used.add(aId)
                used.add(bId)
                break
            }
        }

        return pairs
    }

    /**
     * Average Elo of the players on an entry, in the tournament's sport. Team
     * sports average the resolved roster (guests have no rating), falling back
     * to the captain.
     */
    internal fun avgElo(registration: Registration): Double {
        val sport = registration.tournament.sportType
        if (registration.isTeamEntry && registration.roster.isNotEmpty()) {
            return registration.roster.sumOf { it.elo(sport) } / registration.roster.size
        }
        val elo1 = registration.player.elo(sport)
        val partner = registration.partner
        return if (partner != null) (elo1 + partner.elo(sport)) / 2.0 else elo1
    }

    private fun nextPowerOf2(n: Int): Int {
        var v = 1
        while (v < n) v *= 2
        return v
    }

    private fun pairKey(a: String, b: String): String =
        if (a < b) "$a-$b" else "$b-$a"
}

data class GroupStageEntry(
    val teamA: Registration,
    val teamB: Registration,
    val round: Int,
    val group: String,
)
