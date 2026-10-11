package com.s2aglobal.tournmate.service.pairing

import com.s2aglobal.tournmate.domain.model.*
import java.util.UUID
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
     * Round robin with an optional second leg (`FormatConfig.doubleRoundRobin`):
     * leg 2 repeats leg 1 with home/away swapped, its rounds numbered on from
     * leg 1's, so every pair meets exactly twice. Mirrors iOS.
     */
    fun generateRoundRobinSchedule(
        teams: List<Registration>,
        doubleRoundRobin: Boolean,
    ): List<Triple<Registration, Registration, Int>> {
        val leg = generateRoundRobinSchedule(teams)
        val legRounds = leg.maxOfOrNull { it.third }
        if (!doubleRoundRobin || legRounds == null) return leg
        return leg + leg.map { Triple(it.second, it.first, it.third + legRounds) }
    }

    /**
     * Teams in knockout seeding order: highest average rating first (ties keep
     * the input order, i.e. registration order). Mirrors iOS.
     */
    fun seededByRating(teams: List<Registration>): List<Registration> =
        teams.sortedByDescending { avgElo(it) }

    /**
     * Teams in knockout seeding order for the organizer's choice: by rating,
     * or for RANDOM a fixed shuffle keyed on [salt] (the tournament id), so
     * every rebuild of the bracket (and iOS) gets the same order. Mirrors iOS.
     */
    fun seeded(teams: List<Registration>, mode: SeedingMode, salt: String): List<Registration> {
        if (mode.effective != SeedingMode.RANDOM) return seededByRating(teams)
        val key = salt.uppercase()
        val hashes: List<ULong> = teams.map { mix64(fnv1a64(key + "|" + it.id.toString().uppercase())) }
        val order = teams.indices.sortedWith { a, b ->
            if (hashes[a] != hashes[b]) hashes[a].compareTo(hashes[b]) else a.compareTo(b)
        }
        return order.map { teams[it] }
    }

    /** SplitMix64 finalizer: spreads small input differences over every bit. Mirrors iOS. */
    fun mix64(value: ULong): ULong {
        var z = value
        z = (z xor (z shr 30)) * 0xbf58476d1ce4e5b9uL
        z = (z xor (z shr 27)) * 0x94d049bb133111ebuL
        return z xor (z shr 31)
    }

    /** FNV-1a, 64-bit, over the UTF-8 bytes (same on iOS). */
    fun fnv1a64(text: String): ULong {
        var hash = 0xcbf29ce484222325uL
        for (byte in text.toByteArray(Charsets.UTF_8)) {
            hash = hash xor byte.toUByte().toULong()
            hash *= 0x100000001b3uL
        }
        return hash
    }

    /**
     * The teams that had a first-round bye: the seeded teams that aren't in any
     * round-1 match, in seeding order. Derived from the matches actually
     * created, so a rating change after the draw can't change who had a bye.
     */
    fun knockoutByeTeams(seeded: List<Registration>, roundOneTeamIds: Set<String>): List<Registration> {
        val playing = roundOneTeamIds.map { it.uppercase() }.toSet()
        return seeded.filter { it.id.toString().uppercase() !in playing }
    }

    /**
     * Knockout seeding for Group + Knockout: group winners first (group A, B…),
     * then runners-up, and so on, with teams swapped so that no first-round
     * match is a rematch from the same group when that can be avoided.
     * [groups] are the group tables in label order, each ranked. Mirrors iOS.
     */
    fun knockoutSeedingFromGroups(groups: List<List<Registration>>, advancingPerGroup: Int): List<Registration> {
        val perGroup = maxOf(1, advancingPerGroup)
        val seeded = mutableListOf<Registration>()
        val groupOf = mutableMapOf<UUID, Int>()
        for (tier in 0 until perGroup) {
            groups.forEachIndexed { gi, table ->
                if (tier < table.size) {
                    seeded.add(table[tier])
                    groupOf[table[tier].id] = gi
                }
            }
        }
        val q = seeded.size
        if (q < 2 || groups.size < 2) return seeded

        // First-round pairs are (byes + i) vs (q − 1 − i). Where both come from
        // the same group, swap the lower-seeded team with the lower-seeded team
        // of another pair (latest pair first) if that clears both pairs.
        val byes = nextPowerOf2(q) - q
        val pairCount = (q - byes) / 2
        fun sameGroup(x: Int, y: Int) = groupOf[seeded[x].id] == groupOf[seeded[y].id]
        fun swap(x: Int, y: Int) { val t = seeded[x]; seeded[x] = seeded[y]; seeded[y] = t }
        for (i in 0 until pairCount) {
            val hi = byes + i
            val lo = q - 1 - i
            if (!sameGroup(hi, lo)) continue
            for (j in pairCount - 1 downTo 0) {
                if (j == i) continue
                val hj = byes + j
                val lj = q - 1 - j
                swap(lo, lj)
                if (!sameGroup(hi, lo) && !sameGroup(hj, lj)) break
                swap(lo, lj)
            }
        }
        return seeded
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

    fun generateBracketFirstRound(teams: List<Registration>): BracketResult =
        generateBracketFirstRoundSeeded(seededByRating(teams))

    /** First knockout round for teams already in seeding order (no re-sorting). Mirrors iOS. */
    fun generateBracketFirstRoundSeeded(seeded: List<Registration>): BracketResult {
        if (seeded.size < 2) return BracketResult(emptyList(), emptyList(), 0)
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
        val seeded = seededByRating(teams)

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

    /**
     * Pairs teams with similar records (win count) for a Swiss round.
     *
     * Algorithm (identical to iOS):
     * 1. Each team's wins from finished matches (ties and no results are not
     *    wins) and its byes so far (rounds it didn't play).
     * 2. Sort by wins descending, then Elo, then input order.
     * 3. Odd count: the bye goes to the lowest-ranked team with the fewest byes
     *    for which the rest can still be paired.
     * 4. Pair top-down, each team with the highest-ranked opponent it hasn't
     *    met, backtracking when a choice leaves teams that can't be paired.
     *    When no full round of new matchups exists (or the search budget runs
     *    out), fall back to the greedy pass, which leaves stranded teams unpaired.
     */
    fun generateSwissPairings(
        teams: List<Registration>,
        finishedMatches: List<Match>,
        swissRound: Int,
    ): List<Pair<Registration, Registration>> {
        if (teams.size < 2) return emptyList()

        val wins = mutableMapOf<String, Int>()
        val played = mutableMapOf<String, Int>()
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
            played[match.teamAId] = (played[match.teamAId] ?: 0) + 1
            played[match.teamBId] = (played[match.teamBId] ?: 0) + 1
        }
        val roundsSoFar = finishedMatches.mapNotNull { it.round }.toSet().size

        val sorted = teams.sortedWith(compareByDescending<Registration> {
            wins[it.id.toString().uppercase()] ?: 0
        }.thenByDescending { avgElo(it) })
        val ids = sorted.map { it.id.toString().uppercase() }
        val n = sorted.size

        fun canMeet(i: Int, j: Int) = pairKey(ids[i], ids[j]) !in playedPairs

        // Backtracking search over indices into `sorted`, in preference order.
        var budget = SWISS_SEARCH_BUDGET
        var partner = IntArray(n) { -1 }
        fun pairRest(excluded: Int): Boolean {
            val a = (0 until n).firstOrNull { it != excluded && partner[it] < 0 } ?: return true
            for (b in (a + 1) until n) {
                if (b == excluded || partner[b] >= 0 || !canMeet(a, b)) continue
                budget -= 1
                if (budget < 0) return false
                partner[a] = b; partner[b] = a
                if (pairRest(excluded)) return true
                partner[a] = -1; partner[b] = -1
            }
            return false
        }

        var found = false
        if (n % 2 == 0) {
            found = pairRest(-1)
        } else {
            // Bye candidates: fewest byes first, then lowest-ranked first.
            val byes = ids.map { roundsSoFar - (played[it] ?: 0) }
            val candidates = (0 until n).sortedWith(compareBy<Int> { byes[it] }.thenByDescending { it })
            for (c in candidates) {
                partner = IntArray(n) { -1 }
                if (pairRest(c)) { found = true; break }
                if (budget < 0) break
            }
        }

        if (found) {
            return (0 until n).mapNotNull { i -> if (partner[i] > i) sorted[i] to sorted[partner[i]] else null }
        }

        // Greedy fallback: pair top-down, skipping rematches.
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

    /** Pairing attempts before the Swiss search gives up and pairs greedily. Same as iOS. */
    const val SWISS_SEARCH_BUDGET = 200_000

    /**
     * Average Elo of the players on an entry, in the tournament's sport. Team
     * sports average the resolved roster (guests have no rating), falling back
     * to the captain.
     */
    internal fun avgElo(registration: Registration): Double {
        val sport = registration.tournament.sportType
        if (registration.isTeamEntry && registration.roster.isNotEmpty()) {
            // A player listed twice on the roster counts once.
            val roster = registration.roster.distinctBy { it.id }
            return roster.sumOf { it.elo(sport) } / roster.size
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
