package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.domain.model.Team

/** Persistent teams for team sports (cricket first). Mirrors iOS `TeamRepository`. */
interface TeamRepository {
    /** Teams of this sport the player belongs to, players resolved, sorted by name. */
    suspend fun myTeams(playerId: String, sport: SportType): List<Team>

    /** One team with its players resolved, or null if it doesn't exist. */
    suspend fun team(id: String): Team?

    /** Creates a team with the captain as its first player. */
    suspend fun createTeam(name: String, sport: SportType, captain: Player, createdBy: String): Team

    /** Replaces the player list (the team's creator only). */
    suspend fun setPlayers(playerIds: List<String>, team: Team)

    /** Adds the player to the team. */
    suspend fun joinTeam(team: Team, player: Player)

    /** Removes the player from the team. */
    suspend fun leaveTeam(team: Team, player: Player)

    /** Deletes the team (the team's creator only). Past registrations keep the name. */
    suspend fun deleteTeam(team: Team)
}
