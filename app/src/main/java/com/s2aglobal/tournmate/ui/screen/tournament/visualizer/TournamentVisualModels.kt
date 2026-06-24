package com.s2aglobal.tournmate.ui.screen.tournament.visualizer

import com.s2aglobal.tournmate.domain.model.*

data class ParticipantNode(
    val registrationId: String,
    val teamName: String,
    val seedNumber: Int? = null,
    val isEliminated: Boolean = false,
    val isWinner: Boolean = false,
)

data class MatchNode(
    val match: Match,
    val teamAName: String,
    val teamBName: String,
    val scoreLine: String,
    val isFinished: Boolean,
    val winnerName: String? = null,
)

data class RoundNode(
    val roundNumber: Int,
    val roundName: String,
    val matches: List<MatchNode>,
)

data class GroupNode(
    val label: String,
    val participants: List<ParticipantNode>,
    val advancingCount: Int,
)

data class TournamentVisualState(
    val formatType: MatchFormat = MatchFormat.SINGLE_ELIMINATION,
    val rounds: List<RoundNode> = emptyList(),
    val groups: List<GroupNode> = emptyList(),
    val knockoutRounds: List<RoundNode> = emptyList(),
    val champion: ParticipantNode? = null,
    val totalRounds: Int = 0,
) {
    companion object {
        fun fromTournament(
            matches: List<Match>,
            registrations: List<Registration>,
            format: MatchFormat,
            totalRounds: Int,
        ): TournamentVisualState {
            if (matches.isEmpty()) return TournamentVisualState(formatType = format)

            fun teamName(reg: Registration): String =
                listOfNotNull(reg.player.name, reg.partner?.name).joinToString(" & ")

            val regMap = registrations.associateBy { it.id.toString().uppercase() }

            fun matchNode(m: Match): MatchNode {
                val aReg = regMap[m.teamAId]
                val bReg = regMap[m.teamBId]
                val aName = aReg?.let { teamName(it) } ?: "TBD"
                val bName = bReg?.let { teamName(it) } ?: "TBD"
                val winnerReg = m.winnerRegistrationId?.let { regMap[it] }
                return MatchNode(
                    match = m, teamAName = aName, teamBName = bName,
                    scoreLine = m.displayScoreLine,
                    isFinished = m.status == MatchStatus.FINISHED,
                    winnerName = winnerReg?.let { teamName(it) },
                )
            }

            when (format) {
                MatchFormat.GROUP_KNOCKOUT -> {
                    val groupMatches = matches.filter { it.groupLabel != null }
                    val knockoutMatches = matches.filter { it.groupLabel == null }

                    val groups = groupMatches.groupBy { it.groupLabel ?: "" }.map { (label, gMatches) ->
                        val teamIds = gMatches.flatMap { listOf(it.teamAId, it.teamBId) }.distinct()
                        val participants = teamIds.mapIndexed { i, id ->
                            val reg = regMap[id]
                            ParticipantNode(id, reg?.let { teamName(it) } ?: "?", seedNumber = i + 1)
                        }
                        GroupNode(label, participants, advancingCount = 2)
                    }.sortedBy { it.label }

                    val koRounds = knockoutMatches.groupBy { it.round ?: 0 }.map { (round, rMatches) ->
                        RoundNode(round, Match.bracketRoundName(round, totalRounds), rMatches.map { matchNode(it) })
                    }.sortedBy { it.roundNumber }

                    return TournamentVisualState(format, groups = groups, knockoutRounds = koRounds, totalRounds = totalRounds)
                }
                else -> {
                    val rounds = matches.groupBy { it.round ?: 0 }.map { (round, rMatches) ->
                        val roundName = when (format) {
                            MatchFormat.SINGLE_ELIMINATION, MatchFormat.DOUBLE_ELIMINATION ->
                                Match.bracketRoundName(round, totalRounds)
                            MatchFormat.SWISS -> "Swiss Round $round"
                            else -> "Round $round"
                        }
                        RoundNode(round, roundName, rMatches.sortedBy { it.bracketPosition ?: Int.MAX_VALUE }.map { matchNode(it) })
                    }.sortedBy { it.roundNumber }

                    val lastRound = rounds.lastOrNull()
                    val champion = lastRound?.matches?.firstOrNull { it.isFinished }?.let { mn ->
                        mn.winnerName?.let { ParticipantNode("", it, isWinner = true) }
                    }

                    return TournamentVisualState(format, rounds = rounds, champion = champion, totalRounds = totalRounds)
                }
            }
        }
    }
}
