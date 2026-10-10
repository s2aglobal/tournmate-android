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
    // ── Team sports (cricket) ──
    /** `teams/{id}` this entry was made from; null for singles/doubles. */
    val teamId: String? = null,
    /** Team name at registration (shown everywhere instead of player names). */
    val teamName: String? = null,
    /** Player ids named for this event (the captain is usually one of them). */
    val rosterIds: List<String> = emptyList(),
    /** Players without an account, shown on the team sheet but never rated. */
    val guestNames: List<String> = emptyList(),
    /** Resolved [rosterIds] (players that could be loaded). Not stored. */
    val roster: List<Player> = emptyList(),
) {
    /** `true` for a team-sport entry (a named team with a roster). */
    val isTeamEntry: Boolean
        get() = teamName != null || teamId != null

    /**
     * `true` when the entry is ready to compete. Doubles: a partner is set.
     * Teams: entered with at least two players (a short roster is shown on the
     * team card via [hasFullSide]).
     */
    val isTeamFormed: Boolean
        get() = if (isTeamEntry || tournament.isTeamEvent) rosterCount >= 2 else partnerId != null

    /** Players named plus guests (team sports). */
    val rosterCount: Int
        get() = rosterIds.map { it.uppercase() }.toSet().size + guestNames.size

    /** Team sports: enough players named for a full side. */
    val hasFullSide: Boolean
        get() = rosterCount >= tournament.cricketRules.playersPerSide

    /** Everyone on the entry: player, partner and roster players (de-duplicated). */
    val allPlayers: List<Player>
        get() = (listOfNotNull(player, partner) + roster).distinctBy { it.id }

    fun contains(playerIdToCheck: String): Boolean =
        playerId.equals(playerIdToCheck, ignoreCase = true) ||
            partnerId?.equals(playerIdToCheck, ignoreCase = true) == true ||
            rosterIds.any { it.equals(playerIdToCheck, ignoreCase = true) }

    fun containsPlayerID(id: UUID): Boolean = contains(id.toString())

    companion object {
        /**
         * Players already entered in a tournament: upper-cased player id → the
         * name of the entry they play for (player, partner and roster ids of
         * every registration). A player can't play for two teams in one event.
         * Same rule as iOS `Registration.enteredPlayers`.
         */
        fun enteredPlayers(registrations: List<Registration>): Map<String, String> {
            val map = mutableMapOf<String, String>()
            for (reg in registrations) {
                val name = reg.displayName()
                for (id in listOf(reg.playerId) + listOfNotNull(reg.partnerId) + reg.rosterIds) {
                    map.putIfAbsent(id.uppercase(), name)
                }
            }
            return map
        }
    }

    /** `true` if a player with this Firebase UID is part of the entry. */
    fun containsFirebaseUid(uid: String): Boolean =
        player.firebaseUid == uid || partner?.firebaseUid == uid || roster.any { it.firebaseUid == uid }

    /**
     * Name shown for this entry: the team name for team sports, otherwise
     * "John Smith & Mike Lee" (or first names only: "John & Mike").
     */
    fun displayName(firstNamesOnly: Boolean = false): String {
        teamName?.takeIf { it.isNotEmpty() }?.let { return it }
        val names = listOfNotNull(player.name, partner?.name)
        val shown = if (firstNamesOnly) names.map { n -> n.split(" ").firstOrNull()?.ifEmpty { null } ?: n } else names
        return shown.joinToString(" & ")
    }
}
